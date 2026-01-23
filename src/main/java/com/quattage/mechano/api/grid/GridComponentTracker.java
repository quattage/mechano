package com.quattage.mechano.api.grid;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridComponentTracker.ComponentHierarchy.ComponentNotFoundException;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.EntityUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.UUIDComposite;
import com.quattage.mechano.api.grid.component.ComponentUUID.VoxelUUID;
import com.quattage.mechano.api.grid.component.DiscreteComponent;
import com.quattage.mechano.api.grid.component.DiscreteComponent.NodeStub;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.component.GridConstruct.GridReferent;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.quattage.mechano.foundation.WorldlyObject;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * A central class for managing the serialization and representation of various data sources.
 * Simultaneously represents a discriminator for {@link ComponentUUID} serialization, as well
 * as a loosely defined set of {@link Griddable} source types. Griddables can be attached to
 * {@link Entity entities}, {@link BlockEntity block entities}, or even the {@link LevelReader level}
 * (theoretically), and these all need ways to distinguish between one another, since these objects
 * are stored differently by the Level and have different needs.
 */
public enum GridComponentTracker {
    
    VOXEL(VoxelUUID.class, BlockEntity.class),
    CHUNK(VoxelUUID.class, LevelChunk.class),
    CONTRAPTION(EntityUUID.class, AbstractContraptionEntity.class),
    ENTITY(EntityUUID.class, Entity.class);

    private static final String PREFIX = "type";

    public static final Codec<ComponentUUID<?>> CODEC = new Codec<>() {
        @Override public <T> DataResult<T> encode(ComponentUUID<?> input, DynamicOps<T> ops, T prefix) {
            GridComponentTracker type = input.getTrackerScope();
            RecordBuilder<T> builder = ops.mapBuilder();
            builder.add(GridComponentTracker.PREFIX, type.ordinal(), Codec.INT);
            try { input.write(builder); } catch (Exception e) {
                String message = "Unknown error occured while encoding UUID type '" + type + "'";
                Mechano.LOGGER.error(message);
                e.printStackTrace();
                return DataResult.error(() -> message);
            }
            return builder.build(prefix);
        }
        @Override public <T> DataResult<Pair<ComponentUUID<?>, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> dyn = new Dynamic<>(ops, input);
            int ordinal = dyn.get(GridComponentTracker.PREFIX).asInt(-1);
            GridComponentTracker[] types = GridComponentTracker.values();
            if(ordinal < 0 || ordinal >= types.length)
                return DataResult.error(() -> "Ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            GridComponentTracker datasource = types[ordinal];
            try {
                ComponentUUID<?> newInstance = datasource.createUUID(dyn);
                return DataResult.success(Pair.of(newInstance, input));
            } catch (Exception e) {
                String message = "Unknown error occured while decoding UUID type '" + datasource + "'";
                Mechano.LOGGER.error(message);
                e.printStackTrace();
                return DataResult.error(() -> message);
            }
        }
    };

    public static final StreamCodec<? super RegistryFriendlyByteBuf, ComponentUUID<?>> STREAM_CODEC = new StreamCodec<>() {
        @Override public void encode(RegistryFriendlyByteBuf buffer, ComponentUUID<?> value) {
            buffer.writeInt(value.getTrackerScope().ordinal());
            value.write(buffer);
        }
        @Override public ComponentUUID<?> decode(RegistryFriendlyByteBuf buffer) {
            return GridComponentTracker.values()[buffer.readInt()].createUUID(buffer);
        }
    };

    public static CircuitComponent find(WorldlyObject world, ComponentUUID<?> id) {
        return GridComponentTracker.find(world.getWorld(), id);
    }

    public static CircuitComponent find(LevelReader world, ComponentUUID<?> id) {
        try{ return GridComponentTracker.findOrThrow(world, id); } catch (ComponentNotFoundException e) { return null; }
    }

    public static CircuitComponent findOrThrow(WorldlyObject world, ComponentUUID<?> id) {
        return GridComponentTracker.findOrThrow(world.getWorld(), id);
    }

    public static CircuitComponent findOrThrow(LevelReader world, ComponentUUID<?> id) {
        Griddable<?> source = id.getProviderSource(world);
        if(source == null) throw new ComponentNotFoundException(id, "No griddable could be located at this ID's primary coordinate!");
        if(!id.hasBindings()) throw new ComponentNotFoundException(id, "The provided ID has no bindings!");

        GridConstruct previous = source;
        for(int x = 0; x < id.getBindingCount(); x++) {
            UUIDComposite binding = id.getBinding(x);
            if(binding == null) throw new NullPointerException("Encountered a null binding while traversing UUID");
            if(previous == null) throw new ComponentNotFoundException(id, "Couldn't find sub-component for binding at index " + x);
            CircuitComponent sub = null;
            try { sub = previous.getComponent(binding); } catch (RuntimeException e) {
                if(e instanceof ComponentNotFoundException cnfe) throw cnfe;
                e.printStackTrace();
                throw new ComponentNotFoundException(id, "Component getter for '" + sub.getClass().getSimpleName() + "' encountered an exception (see above)");
            }
            if(sub == previous && x < (id.getBindingCount() - 1))
                throw new ComponentNotFoundException(id, "Component getter in '" + previous.getClass().getSimpleName() + "' returned itself!");
            if(sub instanceof GridConstruct gc) {
                if(gc.getHierarchyType() != binding.getHierarchyType()) {
                    throw new ComponentNotFoundException(id, "Returned component instance didn't conform to the expected type!"
                        + " (expected " + binding.getHierarchyType() + ", got " + gc.getHierarchyType() + ") at index " + x);
                }
                previous = gc;
                continue;
            }
            if(sub == null) throw new ComponentNotFoundException(id, "Couldn't find sub-component for binding at index " + x + ", the value returned by getter in '" + previous.getClass().getSimpleName() + "' was null!");
            if(x == id.getBindingCount() - 1) return sub;
            throw new ComponentNotFoundException(id, "Traversed a non-construct object at binding index " + x + " (got" + sub.getClass().getSimpleName() + ")");

        }
        if(previous instanceof CircuitComponent comp) return comp;
        throw new ComponentNotFoundException(id, "The object at this address is not a valid substitute for a CircuitCompoent! (got" + previous.getClass().getSimpleName() + ")");
    }

    /**
     * Acquires a {@link ComponentUUID} instance pointing to <code>source</code>
     * and bound to the supplied {@link CircuitComponent} <code>component</code>.
     * <h3>There is no error checking to ensure that the supplied <code>component</code>
     * belongs to some construct attached to <code>source</code>. For the UUID to be useful,
     * you need to garantee this yourself.</h3>
     * @param source The {@link Griddable} to pull a UUID instance from
     * @param component The particular {@link CircuitComponent} that the returned UUID will be bound to, 
     * provided it belongs to <code>source</code>
     * @return A (newly instantiated or cachced) ComponentUUID instance. 
     * While modification is allowed, it is not reccomended.
     * @see {@link Griddable#getAddress()}
     * @see {@link GridConstruct#bindUUID}
     */
    public static <T extends ComponentUUID<T>> T getAddress(GridReferent<T> obj, GridConstruct component) {
        Objects.requireNonNull(obj);
        Objects.requireNonNull(component);
        T id = obj.getUUIDSafe();
        component.forEachConstructInHierarchy(construct -> { 
            if(!(construct instanceof Griddable<?>)) 
                construct.bindUUID(id); 
        });
        return id;
    }

    /**
     * Acquires a {@link ComponentUUID} instance pointing to <code>source</code>
     * and bound to the supplied {@link CircuitComponent} <code>component</code>.
     * <h3>There is no error checking to ensure that the supplied <code>component</code>
     * belongs to some construct attached to <code>source</code>. For the UUID to be useful,
     * you need to garantee this yourself.</h3>
     * @param source The {@link Griddable} to pull a UUID instance from
     * @param component The particular {@link CircuitComponent} that the returned UUID will be bound to, 
     * provided it belongs to <code>source</code>
     * @return A (newly instantiated or cachced) ComponentUUID instance. 
     * While modification is allowed, it is not reccomended.
     * @see {@link Griddable#getAddress()}
     * @see {@link GridConstruct#bindUUID}
     */
    public static <T extends ComponentUUID<T>> T getAddress(GridReferent<T> obj) {
        Objects.requireNonNull(obj);
        return obj.getUUIDSafe();
    }

    public static @Nullable Griddable<?> getSource(Object obj) {
        return GridComponentTracker.getSource(null, obj);
    }

    public static @Nullable Griddable<?> getSource(@Nullable LevelReader world, Object obj) {
        if((obj instanceof GridReferent<?> gr)) gr.getProviderSource(world);
        if(obj instanceof Node n) {
            List<AncillaryNode<?>> ancillaries = n.getAncillaries();
            if(ancillaries == null || ancillaries.isEmpty()) return null;
            AncillaryNode<?> first = ancillaries.getFirst();
            return first == null ? null : first.getProviderSource();
        }
        return null;
    }

    public static List<AncillaryPair> getLinksBelongingTo(Griddable<?> source) {
        Grid grid = Grid.getUnsided(source.getWorld());
        return grid.getLinksBelongingTo(source);
    }

    // TODO make this use a stream instead because this could have really bad iteration performance in worst case scenarios
    public static Set<ServerPlayer> collectPlayersTracking(ServerLevel world, Collection<GridReferent<?>> objs) {
        Set<ServerPlayer> senders = new HashSet<>();
        for(ServerPlayer sp : world.getServer().getPlayerList().getPlayers()) {
            for(GridReferent<?> referent : objs) {
                if(!referent.isBeingTrackedBy(sp)) continue;
                Griddable<?> source = referent.getProviderSource(world);
                if(source != null && source.isBeingTrackedBy(sp)) {
                    senders.add(sp);
                    break;
                }
            }
        }
        return senders;
    }

    public static Set<ServerPlayer> collectPlayersTracking(ServerLevel world, GridReferent<?>... objs) {
        Set<ServerPlayer> senders = new HashSet<>();
        for(ServerPlayer sp : world.getServer().getPlayerList().getPlayers()) {
            for(GridReferent<?> referent : objs) {
                if(referent.isBeingTrackedBy(sp)) {
                    Griddable<?> source = referent.getProviderSource(world);
                    if(source == null) continue;
                    if(source.isBeingTrackedBy(sp)) {
                        senders.add(sp);
                        break;
                    }
                }
            }
        }
        return senders;
    }

    /**
     * Checks whether or not the provided object can be discovered by this
     * Grid. Non-reachable objects are either not loaded by Minecraft or
     * no longer exist for whatever reason. In most scenarios, you can 
     * already guarantee the reachability of a {@link Griddable} as long 
     * as you use tranditional instantiation methods (like placing a block 
     * or spawning an entity) - In situations where that is not the case, 
     * (e.g. tests) this method will tell you whether or not <code>obj</code>
     * can be discovered in the world.
     * @param obj {@link GridIdentifiable} to address
     * @return <code>true</code> if <code>obj</code> is reachable.
     */
    public static boolean isReachable(LevelReader world, GridReferent<?> obj) {
        Griddable<?> source = obj.getUUIDSafe().getProviderSource(world);
        return source != null && source.getComponent() != null;
    }

    public static void forEachSourceType(Consumer<GridComponentTracker> cons) {
        for(int x = 0; x < GridComponentTracker.values().length; x++) {
            GridComponentTracker type = GridComponentTracker.values()[x];
            cons.accept(type);
        }
    }

    public static CompoundTag write(ComponentUUID<?> addr, CompoundTag tag) {
        tag.putInt(GridComponentTracker.PREFIX, addr.getTrackerScope().ordinal());
        addr.write(tag);
        return tag;
    }

    public static ByteBuf write(ComponentUUID<?> addr, ByteBuf buffer) {
        buffer.writeInt(addr.getTrackerScope().ordinal());
        addr.write(buffer);
        return buffer;
    }

    public static void write(ComponentUUID<?> addr, RecordBuilder<?> builder) {
        builder.add(GridComponentTracker.PREFIX, addr.getTrackerScope().ordinal(), Codec.INT);
        addr.write(builder);
    }

    public static ComponentUUID<?> read(CompoundTag tag) {
        int ordinal = tag.getInt(GridComponentTracker.PREFIX);
        GridComponentTracker[] types = GridComponentTracker.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + tag 
                +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(tag);
    }

    public static ComponentUUID<?> read(ByteBuf buffer) {
        int ordinal = buffer.readInt();
        GridComponentTracker[] types = GridComponentTracker.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + buffer 
                + " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(buffer);
    }

    public static ComponentUUID<?> read(Dynamic<?> dyn) {
        int ordinal = dyn.get(GridComponentTracker.PREFIX).asInt(-1);
        GridComponentTracker[] types = GridComponentTracker.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + dyn 
                +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(dyn);
    }

    /**
     * This class uses reflection, and so stores
     * references to the various constructors in each
     * {@link ComponentUUID} class. This method clears those
     * weakly-referenced constructors from the discriminator.
     */
    public static void clearReferences() {
        for(int x = 0; x < GridComponentTracker.values().length; x++)
            GridComponentTracker.values()[x].clear();
    }

    private final Class<? extends ComponentUUID<?>> clazz; 
    private final Class<? extends IAttachmentHolder> referent;
    private WeakReference<Constructor<? extends ComponentUUID<?>>> tagCtor = new WeakReference<>(null);;
    private WeakReference<Constructor<? extends ComponentUUID<?>>> byteBufCtor = new WeakReference<>(null);
    private WeakReference<Constructor<? extends ComponentUUID<?>>> dynamicCtor = new WeakReference<>(null);;

    <T extends ComponentUUID<T>> GridComponentTracker(Class<T> clazz, Class<? extends IAttachmentHolder> referent) {
        this.clazz = clazz;
        this.referent = referent;
    }

    public Class<? extends ComponentUUID<?>> getUUIDClass() {
        return clazz;
    }

    public boolean permits(IAttachmentHolder holder) {
        return referent.isAssignableFrom(holder.getClass());
    }

    protected void clear() {
        byteBufCtor.clear();
        dynamicCtor.clear();
        tagCtor.clear();
    }

    private ComponentUUID<?> createUUID(CompoundTag tag) {
        if(clazz == null) throw new NullPointerException("Cannot instantiate ComponentUUID '" + this + "' because this enum member hasn't been configured!");
        try {
            if(tagCtor.refersTo(null)) tagCtor = new WeakReference<>(clazz.getDeclaredConstructor(CompoundTag.class));
            return tagCtor.get().newInstance(tag);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + tag.getClass().getName());
        }
    }

    public ComponentUUID<?> createRandomUUID(RandomSource random) {
        try {
            Constructor<? extends ComponentUUID<?>> randomCtor = clazz.getDeclaredConstructor(RandomSource.class);
            return randomCtor.newInstance(random);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + random.getClass().getName());
        }
    }

    private ComponentUUID<?> createUUID(ByteBuf buffer) {
        if(clazz == null) throw new NullPointerException("Cannot instantiate ComponentUUID " + this + " because this enum member hasn't been configured!");
        try {
            if(byteBufCtor.refersTo(null)) byteBufCtor = new WeakReference<>(clazz.getDeclaredConstructor(ByteBuf.class));
            return byteBufCtor.get().newInstance(buffer);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + buffer.getClass().getName());
        }
    }

    private ComponentUUID<?> createUUID(Dynamic<?> dyn) {
        if(clazz == null) throw new NullPointerException("Cannot instantiate ComponentUUID " + this + " because this enum member hasn't been configured!");
        try {
            if(dynamicCtor.refersTo(null)) dynamicCtor = new WeakReference<>(clazz.getDeclaredConstructor(Dynamic.class));
            return dynamicCtor.get().newInstance(dyn);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + dyn.getClass().getName());
        }
    }

    public enum ComponentHierarchy implements StringRepresentable {

        LINK(2, ComponentLink.class),
        CIRCUIT(0, Circuit.class, LINK),
        STUB(1, NodeStub.class, CIRCUIT),
        NODE(3, Node.class, STUB),
        ANCILLARY(4, AncillaryNode.class, NODE),
        DISCRETE(1, DiscreteComponent.class, CIRCUIT, LINK),
        TERMINAL(5, Terminal.class, DISCRETE, NODE),
        STRANGER(20, null);

        private final byte mergePriority;
        private final Class<? extends CircuitComponent> typeClass;
        private final ComponentHierarchy[] parents;

        ComponentHierarchy(int mergePriority, Class<? extends CircuitComponent> typeClass) {
            this.mergePriority = (byte)mergePriority;
            this.typeClass = typeClass;
            this.parents = new ComponentHierarchy[0];
        }

        ComponentHierarchy(int mergePriority, Class<? extends CircuitComponent> typeClass, ComponentHierarchy... parents) {
            this.mergePriority = (byte)mergePriority;
            this.typeClass = typeClass;
            this.parents = parents;
        }

        public Class<? extends CircuitComponent> getTypeClass() {
            return typeClass;
        }

        public boolean canBeOwnedBy(ComponentHierarchy type) {
            if(parents.length < 0) return false;
            for(int x = 0; x < parents.length; x++)
                if(parents[x] == type) return true;
            return false;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getSerializedName();
        }

        public int getMergePriority() {
            return mergePriority;
        }

        public static class ComponentNotFoundException extends RuntimeException {
            public ComponentNotFoundException(String message) {
                super(message);
            }
            public ComponentNotFoundException(ComponentUUID<?> id, String message) {
                super("Error getting component at " + id + " - " + message);
            }
        }

        public static class UnexpectedReferentException extends RuntimeException {
            public UnexpectedReferentException(GridConstruct referent, ComponentHierarchy expected) {
                super("Got '" + referent.getClass().getSimpleName() + "' of type '" + referent.getHierarchyType() + "', but operation expected the referent '" + expected + "'");
            }
        }
    }
}