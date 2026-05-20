package com.quattage.mechano.grid;

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
import com.quattage.mechano.api.Griddable;
import com.quattage.mechano.foundation.WorldlyObject;
import com.quattage.mechano.grid.GridTracking.ComponentHierarchy.ComponentNotFoundException;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.Circuit;
import com.quattage.mechano.grid.topology.ComponentLink;
import com.quattage.mechano.grid.topology.core.CircuitComponent;
import com.quattage.mechano.grid.topology.core.DiscreteComponent;
import com.quattage.mechano.grid.topology.core.DiscreteComponent.NodeStub;
import com.quattage.mechano.grid.topology.core.GridUUID;
import com.quattage.mechano.grid.topology.core.GridUUID.EntityUUID;
import com.quattage.mechano.grid.topology.core.GridUUID.UUIDComposite;
import com.quattage.mechano.grid.topology.core.GridUUID.VoxelUUID;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct.GridReferent;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct.SourceProvider;
import com.quattage.mechano.grid.topology.core.Node;
import com.quattage.mechano.grid.topology.core.Terminal;
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
 * as a loosely defined set of {@link Griddable} owner types. Griddables can be attached to
 * {@link Entity entities}, {@link BlockEntity block entities}, or even the {@link LevelReader level}
 * (theoretically), and these all need ways to distinguish between one another, since these objects
 * are stored differently by the Level and have different needs.
 */
public enum GridTracking {

    VOXEL(VoxelUUID.class, BlockEntity.class),
    CHUNK(VoxelUUID.class, LevelChunk.class),
    CONTRAPTION(EntityUUID.class, AbstractContraptionEntity.class),
    // SIMULATED(EntityUUID.class, ??),
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
        Griddable<?> owner = GridTracking.getReferentOrThrow(world, id);
        if(owner == null) throw new ComponentNotFoundException(id, "No griddable could be located at this ID's primary coordinate!");
        if(!id.hasBindings()) throw new ComponentNotFoundException(id, "The provided ID has no bindings!");

        HierarchicalConstruct previous = owner;
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
     * Acquires a {@link GridUUID} instance pointing to <code>owner</code>
     * and bound to the supplied {@link CircuitComponent} <code>component</code>.
     * There is no error checking to ensure that the supplied <code>component</code>
     * belongs to some construct attached to <code>owner</code>. For the UUID to be useful,
     * you need to garantee this yourself.
     * @param owner The {@link Griddable} to pull a UUID instance from
     * @param obj The particular {@link CircuitComponent} that the returned UUID will be bound to, 
     * provided it belongs to <code>owner</code>
     * @return A (newly instantiated or cachced) ComponentUUID instance. 
     * While modification is allowed, it is not reccomended.
     * @see {@link Griddable#getAddress()}
     * @see {@link HierarchicalConstruct#bindUUID}
     * @throws NullPointerException if <code>owner</code> is null, <code>obj</code> is null or can't provide a valid UUID for whatever reason
     */
    public static <T extends GridUUID<T>> T getAddress(GridReferent<T> owner, HierarchicalConstruct obj) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(obj);
        T addr = GridTracking.getAddress(owner);
        obj.forEachConstructInHierarchy(construct -> { 
            if(!(construct instanceof Griddable<?>)) 
                construct.bindUUID(addr); 
        });
        return addr;
    }

    /**
     * Returns a {@link GridUUID} which points to <code>component</code>.
     * This is useful for describing this component's physical location 
     * in a serializable format. <p>
     * This method automatically traverses the hierarcy structure of
     * <code>component</code> upwards until it finds a valid parent
     * {@link Griddable}. This griddable instance is used as the
     * basis for the new GridUUID.
     * This call can be rather expensive and should be avoided
     * in favour of {@link #getAddress(GridReferent, HierarchicalConstruct) 
     * the overload} that takes a manually-supplied parent object.
     * @param obj The component to get a bound UUID for
     * @param world Optional, but you should provide it if you have access to 
     * one where you're calling this method. This level instance is used only 
     * when absolutely necessary to look up {@link GridReferent} data attached 
     * to or existing within the level. These lookups are usually skipped for 
     * optimization purposes and beccause the {@link HierarchicalConstruct}
     * makes this convenient, but providing a level instance may be necessary 
     * for acquiring components attached to some types of referents.
     * 
     * @return a new {@link GridUUID} instance that points to <code>component</code>
     * @throws ComponentNotFoundException if <code>obj</code> has no valid {@link GridReferent} in its parental hierarchy
     * @throws NullPointerException if <code>obj</code> is null or grid referent that owns <code>obj</code> failed to provide a valid UUID
     * @throws UnsupportedOperationException if a traversed component or griddable requires the world to proceed and no world was given
     */
    public static GridUUID<?> getAddress(HierarchicalConstruct obj) {
        Objects.requireNonNull(obj);
        if(obj instanceof GridReferent gr)
            return GridTracking.getAddress(gr.getReferent(), obj);
        LevelReader world = obj instanceof WorldlyObject wrl ? wrl.getWorld() : null;
        Griddable<?> owner = GridTracking.getReferentOrThrow(world, obj);
        return GridTracking.getAddress(owner, obj);
    }

    /**
     * Returns a {@link GridUUID} pointing to <code>obj</code>
     * <p>
     * This method exists for consistency's sake, to pad out the {@link #getAddress} 
     * series of overloads. It is functionally identical to calling {@link GridReferent#getUUID}
     * but with a built-in nullcheck.
     * @param obj The component to get a bound UUID for
     * @return a new {@link GridUUID} as described in {@link GridReferent#getUUID()}
     * @throws NullPointerException if <code>obj</code> is null or failed to provide a valid UUID
     */
    public static <T extends GridUUID<T>> T getAddress(GridReferent<T> obj) {
        Objects.requireNonNull(obj);
        T addr = obj.getUUID();
        if(addr == null)
            throw new NullPointerException("GridReferent '" + obj.getClass().getSimpleName() + " failed to provide a non-null UUID!");
        return addr;
    }

    /**
     * Acquires a {@link GridUUID} instance pointing to <code>owner</code>
     * and bound to the supplied {@link CircuitComponent} <code>component</code>.
     * There is no error checking to ensure that the supplied <code>component</code>
     * belongs to some construct attached to <code>owner</code>. For the UUID to be useful,
     * you need to garantee this yourself.
     * @param component The particular {@link CircuitComponent} that the returned UUID will be bound to.
     * The owner will be automatically found for you.
     * @return A (newly instantiated or cachced) ComponentUUID instance. 
     * While modification is allowed, it is not reccomended.
     * @see {@link Griddable#getAddress()}
     * @see {@link HierarchicalConstruct#bindUUID}
     * @throws ComponentNotFoundException if <code>obj</code> has no valid {@link GridReferent} in its parental hierarchy
     * @throws NullPointerException if <code>obj</code> is null or grid referent that owns <code>obj</code> failed to provide a valid UUID
     * @throws UnsupportedOperationException if a traversed component or griddable requires the world to proceed and no world was given
     */
    @SuppressWarnings("unchecked")
    public static GridUUID<?> getAddress(@Nullable LevelReader world, HierarchicalConstruct component) {
        Objects.requireNonNull(component);
        GridUUID<?> rawID;
        if(component instanceof GridReferent gr) {
            rawID = GridTracking.getAddress(gr);
            component.bindUUID(rawID);
            return rawID;
        }
        if(world == null && component instanceof WorldlyObject wrl)
            world = wrl.getWorld(); 
        Griddable<?> owner = GridTracking.getReferentOrThrow(world, component);
        return GridTracking.getAddress(owner, component);
    }

    public static Griddable<?> getReferentOrThrow(GridReferent<?> obj) {
        return GridTracking.getReferentOrThrow(null, obj);
    }

    public static Griddable<?> getReferentOrThrow(HierarchicalConstruct hc) {
        return GridTracking.getReferentOrThrow(null, hc);
    }

    public static Griddable<?> getReferentOrThrow(AncillaryNode<?> anc) {
        Objects.requireNonNull(anc);
        return GridTracking.getReferentOrThrow(anc.getWorld(), (GridReferent<?>)anc.getReferent());
    }

    public static Griddable<?> getReferentOrThrow(@Nullable LevelReader world, HierarchicalConstruct hc) {
        return switch (hc) {
            case null -> throw new ComponentNotFoundException("Can't locate owner for a null object!");
            case Griddable<?> owner -> owner;
            case GridReferent<?> gr -> GridTracking.getReferentOrThrow(world, gr);
            case SourceProvider sp -> {
                GridReferent<?> tryGet = sp.getReferent();
                if(tryGet instanceof Griddable<?> owner) yield owner;
                if(tryGet != null) tryGet = world == null ? tryGet.getReferent() : tryGet.getReferent(world);
                yield GridTracking.find(world, hc);
            } default -> GridTracking.find(world, hc);
        };
    }

    public static Griddable<?> getReferentOrThrow(@Nullable LevelReader world, GridReferent<?> obj) {
        if(obj == null)
            throw new ComponentNotFoundException("Can't locate owner for a null object!");
        if(obj instanceof Griddable<?> owner) return owner;
        GridReferent<?> tryGet = world == null ? obj.getReferent() : obj.getReferent(world);
        if(tryGet instanceof Griddable<?> owner) return owner;
        if(obj instanceof HierarchicalConstruct hc)
            return GridTracking.find(world, hc);
        throw new ComponentNotFoundException("Object " + obj.getClass().getSimpleName() 
            + " couldn't fall back on a hierarchical search to locate its parent owner because this object is not a hierarchical type!");
    }

    public static Griddable<?> find(LevelReader world, HierarchicalConstruct hc) {
        GridReferent<?> tryGet = GridTracking.findReferent(hc);
        if(tryGet instanceof Griddable<?> owner) return owner;
        if(tryGet != null) tryGet = world == null ? tryGet.getReferent() : tryGet.getReferent(world);
        if(tryGet instanceof Griddable<?> owner) return owner;
        throw new ComponentNotFoundException("Object " + hc.getClass().getSimpleName() 
            + " couuldn't locate a griddable owner within its parental hierarchy!");
    }

    private static @Nullable GridReferent<?> findReferent(HierarchicalConstruct hc) {
        HierarchicalConstruct current = hc;
        HierarchicalConstruct next = hc;
        for(int x = 0; x < 255; x++) {
            if(current instanceof GridReferent<?> source) return source;
            if(current == null || next == null || next == current)
                return null;
            next = current.getParentConstruct();
            current = next;
        }
        return null;
    }

    public static @Nullable HierarchicalConstruct findSuperparent(HierarchicalConstruct obj) {
        obj = obj.getParentConstruct();
        for(int x = 0; x < 255; x++) {
            if(obj == null) return null;
            HierarchicalConstruct next = obj.getParentConstruct();
            if(next == null || next == obj) return obj;
            obj = next;
        }
        Mechano.LOGGER.warn("Component hierarchy traversal for " + obj + " failed to identify a superparent.");
        return null;
    }

    public static Set<ServerPlayer> collectPlayersTracking(ServerLevel world, Collection<GridReferent<?>> objs) {
        Set<ServerPlayer> senders = new HashSet<>();
        for(ServerPlayer sp : world.getServer().getPlayerList().getPlayers()) {
            for(GridReferent<?> obj : objs) {
                if(!obj.isBeingTrackedBy(sp)) continue;
                Griddable<?> owner = GridTracking.getReferentOrThrow(world, obj);
                if(owner != null && owner.isBeingTrackedBy(sp)) {
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
            for(GridReferent<?> obj : objs) {
                if(obj.isBeingTrackedBy(sp)) {
                    Griddable<?> owner = GridTracking.getReferentOrThrow(world, obj);
                    if(owner == null) continue;
                    if(owner.isBeingTrackedBy(sp)) {
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
        Griddable<?> owner = GridTracking.getReferentOrThrow(world, obj);
        if(owner == null || owner.getComponent() == null) return false;
        obj = GridTracking.getAddress(obj);
        owner = GridTracking.getReferentOrThrow(world, obj);
        return owner != null;
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
    private final Class<? extends IAttachmentHolder> obj;
    private WeakReference<Constructor<? extends GridUUID<?>>> tagCtor = new WeakReference<>(null);;
    private WeakReference<Constructor<? extends GridUUID<?>>> byteBufCtor = new WeakReference<>(null);
    private WeakReference<Constructor<? extends GridUUID<?>>> dynamicCtor = new WeakReference<>(null);;

    <T extends GridUUID<T>> GridTracking(Class<T> clazz, Class<? extends IAttachmentHolder> obj) {
        this.clazz = clazz;
        this.obj = obj;
    }

    public Class<? extends GridUUID<?>> getUUIDClass() {
        return clazz;
    }

    public boolean permits(IAttachmentHolder holder) {
        return obj.isAssignableFrom(holder.getClass());
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
            public UnexpectedReferentException(HierarchicalConstruct obj, ComponentHierarchy expected) {
                super("Got '" + obj.getClass().getSimpleName() + "' of type '" + obj.getHierarchyType() + "', but operation expected the obj '" + expected + "'");
            }
        }
    }
}