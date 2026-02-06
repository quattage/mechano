package com.quattage.mechano.api.grid;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
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
import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy.ComponentNotFoundException;
import com.quattage.mechano.api.grid.GridUUID.EntityUUID;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.GridUUID.VoxelUUID;
import com.quattage.mechano.api.grid.HierarchicalConstruct.GridReferent;
import com.quattage.mechano.api.grid.HierarchicalConstruct.SourceProvider;
import com.quattage.mechano.api.grid.component.Circuit;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.DiscreteComponent;
import com.quattage.mechano.api.grid.component.DiscreteComponent.NodeStub;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.grid.topology.landmark.ComponentLink;
import com.quattage.mechano.api.grid.topology.landmark.Node;
import com.quattage.mechano.api.grid.topology.landmark.Terminal;
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
 * Simultaneously represents a discriminator for {@link GridUUID} serialization, as well
 * as a loosely defined set of {@link Griddable} source types. Griddables can be attached to
 * {@link Entity entities}, {@link BlockEntity block entities}, or even the {@link LevelReader level}
 * (theoretically), and these all need ways to distinguish between one another, since these objects
 * are stored differently by the Level and have different needs.
 */
public enum GridTracking {

    VOXEL(VoxelUUID.class, BlockEntity.class),
    CHUNK(VoxelUUID.class, LevelChunk.class),
    CONTRAPTION(EntityUUID.class, AbstractContraptionEntity.class),
    ENTITY(EntityUUID.class, Entity.class);

    private static final String PREFIX = "type";

    public static final Codec<GridUUID<?>> UUID_CODEC = new Codec<>() {
        @Override public <T> DataResult<T> encode(GridUUID<?> input, DynamicOps<T> ops, T prefix) {
            GridTracking type = input.getTrackerScope();
            RecordBuilder<T> builder = ops.mapBuilder();
            builder.add(GridTracking.PREFIX, type.ordinal(), Codec.INT);
            try { input.write(builder); } catch (Exception e) {
                String message = "Unknown error occured while encoding UUID type '" + type + "'";
                Mechano.LOGGER.error(message);
                e.printStackTrace();
                return DataResult.error(() -> message);
            }
            return builder.build(prefix);
        }
        @Override public <T> DataResult<Pair<GridUUID<?>, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> dyn = new Dynamic<>(ops, input);
            int ordinal = dyn.get(GridTracking.PREFIX).asInt(-1);
            GridTracking[] types = GridTracking.values();
            if(ordinal < 0 || ordinal >= types.length)
                return DataResult.error(() -> "Ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            GridTracking datasource = types[ordinal];
            try {
                GridUUID<?> newInstance = datasource.createUUID(dyn);
                return DataResult.success(Pair.of(newInstance, input));
            } catch (Exception e) {
                String message = "Unknown error occured while decoding UUID type '" + datasource + "'";
                Mechano.LOGGER.error(message);
                e.printStackTrace();
                return DataResult.error(() -> message);
            }
        }
    };

    public static final StreamCodec<? super RegistryFriendlyByteBuf, GridUUID<?>> UUID_STREAM_CODEC = new StreamCodec<>() {
        @Override public void encode(RegistryFriendlyByteBuf buffer, GridUUID<?> value) {
            buffer.writeInt(value.getTrackerScope().ordinal());
            value.write(buffer);
        }
        @Override public GridUUID<?> decode(RegistryFriendlyByteBuf buffer) {
            return GridTracking.values()[buffer.readInt()].createUUID(buffer);
        }
    };

    public static CircuitComponent getComponent(WorldlyObject world, GridUUID<?> id) {
        return GridTracking.getComponent(world.getWorld(), id);
    }

    public static CircuitComponent getComponent(LevelReader world, GridUUID<?> id) {
        try{ return GridTracking.getComponentOrThrow(world, id); } catch (ComponentNotFoundException e) { return null; }
    }

    public static CircuitComponent getComponentOrThrow(WorldlyObject world, GridUUID<?> id) {
        return GridTracking.getComponentOrThrow(world.getWorld(), id);
    }

    public static CircuitComponent getComponentOrThrow(LevelReader world, GridUUID<?> id) {
        Griddable<?> source = GridTracking.getSource(world, id);
        if(source == null) throw new ComponentNotFoundException(id, "No griddable could be located at this ID's primary coordinate!");
        if(!id.hasBindings()) throw new ComponentNotFoundException(id, "The provided ID has no bindings!");

        HierarchicalConstruct previous = source;
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
            if(sub instanceof HierarchicalConstruct gc) {
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
     * Acquires a {@link GridUUID} instance pointing to <code>source</code>
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
     * @see {@link HierarchicalConstruct#bindUUID}
     */
    public static <T extends GridUUID<T>> T getAddress(GridReferent<T> obj, HierarchicalConstruct component) {
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
     * Acquires a {@link GridUUID} instance pointing to <code>source</code>
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
     * @see {@link HierarchicalConstruct#bindUUID}
     */
    public static <T extends GridUUID<T>> T getAddress(GridReferent<T> obj) {
        Objects.requireNonNull(obj);
        return obj.getUUIDSafe();
    }

    public static @Nullable Griddable<?> getSource(SourceProvider prov) {
        return GridTracking.getSource(null, prov);
    }

    public static @Nullable Griddable<?> getSource(@Nullable LevelReader world, SourceProvider prov) {
        Objects.requireNonNull(prov);
        GridReferent<?> referent = world == null ? prov.getProviderSource() : prov.getProviderSource(world);
        if(!(referent instanceof Griddable<?> source)) {
            return null;
            // throw new ComponentNotFoundException("Couldn't get griddable source for '" + prov.getClass().getSimpleName() + "' - This provider returned a referent of type '" 
                // + (referent == null ? "null" : referent.getClass().getSimpleName()) + "', which isn't a valid griddable instance!");
        }
        return source;
    }

    public static Set<ServerPlayer> collectPlayersTracking(ServerLevel world, Collection<GridReferent<?>> objs) {
        Set<ServerPlayer> senders = new HashSet<>();
        for(ServerPlayer sp : world.getServer().getPlayerList().getPlayers()) {
            for(GridReferent<?> referent : objs) {
                if(!referent.isBeingTrackedBy(sp)) continue;
                Griddable<?> source = GridTracking.getSource(world, referent);
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
                    Griddable<?> source = GridTracking.getSource(world, referent);
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
    public static boolean isReachable(LevelReader world, GridReferent<?> referent) {
        Griddable<?> source = GridTracking.getSource(world, referent);
        if(source == null || source.getComponent() == null) return false;
        referent = GridTracking.getAddress(referent);
        source = GridTracking.getSource(world, referent);
        return source != null;
    }

    public static void forEachSourceType(Consumer<GridTracking> cons) {
        for(int x = 0; x < GridTracking.values().length; x++) {
            GridTracking type = GridTracking.values()[x];
            cons.accept(type);
        }
    }

    public static CompoundTag write(GridUUID<?> addr, CompoundTag tag) {
        tag.putInt(GridTracking.PREFIX, addr.getTrackerScope().ordinal());
        addr.write(tag);
        return tag;
    }

    public static ByteBuf write(GridUUID<?> addr, ByteBuf buffer) {
        buffer.writeInt(addr.getTrackerScope().ordinal());
        addr.write(buffer);
        return buffer;
    }

    public static void write(GridUUID<?> addr, RecordBuilder<?> builder) {
        builder.add(GridTracking.PREFIX, addr.getTrackerScope().ordinal(), Codec.INT);
        addr.write(builder);
    }

    public static GridUUID<?> read(CompoundTag tag) {
        int ordinal = tag.getInt(GridTracking.PREFIX);
        GridTracking[] types = GridTracking.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + tag 
                +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(tag);
    }

    public static GridUUID<?> read(ByteBuf buffer) {
        int ordinal = buffer.readInt();
        GridTracking[] types = GridTracking.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + buffer 
                + " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(buffer);
    }

    public static GridUUID<?> read(Dynamic<?> dyn) {
        int ordinal = dyn.get(GridTracking.PREFIX).asInt(-1);
        GridTracking[] types = GridTracking.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + dyn 
                +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(dyn);
    }

    /**
     * This class uses reflection, and so stores
     * references to the various constructors in each
     * {@link GridUUID} class. This method clears those
     * weakly-referenced constructors from the discriminator.
     */
    public static void clearReferences() {
        for(int x = 0; x < GridTracking.values().length; x++)
            GridTracking.values()[x].clear();
    }

    private final Class<? extends GridUUID<?>> clazz; 
    private final Class<? extends IAttachmentHolder> referent;
    private WeakReference<Constructor<? extends GridUUID<?>>> tagCtor = new WeakReference<>(null);;
    private WeakReference<Constructor<? extends GridUUID<?>>> byteBufCtor = new WeakReference<>(null);
    private WeakReference<Constructor<? extends GridUUID<?>>> dynamicCtor = new WeakReference<>(null);;

    <T extends GridUUID<T>> GridTracking(Class<T> clazz, Class<? extends IAttachmentHolder> referent) {
        this.clazz = clazz;
        this.referent = referent;
    }

    public Class<? extends GridUUID<?>> getUUIDClass() {
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

    private GridUUID<?> createUUID(CompoundTag tag) {
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

    public GridUUID<?> createRandomUUID(RandomSource random) {
        try {
            Constructor<? extends GridUUID<?>> randomCtor = clazz.getDeclaredConstructor(RandomSource.class);
            return randomCtor.newInstance(random);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate ComponentUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + random.getClass().getName());
        }
    }

    private GridUUID<?> createUUID(ByteBuf buffer) {
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

    private GridUUID<?> createUUID(Dynamic<?> dyn) {
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
            public ComponentNotFoundException(GridUUID<?> id, String message) {
                super("Error getting component at " + id + " - " + message);
            }
        }

        public static class UnexpectedReferentException extends RuntimeException {
            public UnexpectedReferentException(HierarchicalConstruct referent, ComponentHierarchy expected) {
                super("Got '" + referent.getClass().getSimpleName() + "' of type '" + referent.getHierarchyType() + "', but operation expected the referent '" + expected + "'");
            }
        }
    }
}