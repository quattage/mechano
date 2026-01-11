package com.quattage.mechano.api.grid.component;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.ComponentUUID.EntityUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.VoxelUUID;
import com.quattage.mechano.api.grid.component.GridConstruct.GridReferent;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.api.grid.topology.vertex.Terminal;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
public enum ComponentTracker {
    
    VOXEL(VoxelUUID.class, BlockEntity.class),
    CHUNK(VoxelUUID.class, LevelChunk.class),
    CONTRAPTION(EntityUUID.class, AbstractContraptionEntity.class),
    ENTITY(EntityUUID.class, Entity.class);

    private static final String PREFIX = "type";

    public static final Codec<ComponentUUID<?>> CODEC = new Codec<>() {
        @Override public <T> DataResult<T> encode(ComponentUUID<?> input, DynamicOps<T> ops, T prefix) {
            ComponentTracker type = input.getTrackerScope();
            RecordBuilder<T> builder = ops.mapBuilder();
            builder.add(ComponentTracker.PREFIX, type.ordinal(), Codec.INT);
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
            int ordinal = dyn.get(ComponentTracker.PREFIX).asInt(-1);
            ComponentTracker[] types = ComponentTracker.values();
            if(ordinal < 0 || ordinal >= types.length)
                return DataResult.error(() -> "Ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            ComponentTracker datasource = types[ordinal];
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
            return ComponentTracker.values()[buffer.readInt()].createUUID(buffer);
        }
    };

    // TODO make this use a stream instead because this could have really bad iteration performance in worst case scenarios
    public static Set<ServerPlayer> collect(ServerLevel world, Collection<GridReferent<?>> objs) {
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

    public static Set<ServerPlayer> collect(ServerLevel world, GridReferent<?>... objs) {
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

    public static void forEachSourceType(Consumer<ComponentTracker> cons) {
        for(int x = 0; x < ComponentTracker.values().length; x++) {
            ComponentTracker type = ComponentTracker.values()[x];
            cons.accept(type);
        }
    }

    public static CompoundTag write(ComponentUUID<?> addr, CompoundTag tag) {
        tag.putInt(ComponentTracker.PREFIX, addr.getTrackerScope().ordinal());
        addr.write(tag);
        return tag;
    }

    public static ByteBuf write(ComponentUUID<?> addr, ByteBuf buffer) {
        buffer.writeInt(addr.getTrackerScope().ordinal());
        addr.write(buffer);
        return buffer;
    }

    public static void write(ComponentUUID<?> addr, RecordBuilder<?> builder) {
        builder.add(ComponentTracker.PREFIX, addr.getTrackerScope().ordinal(), Codec.INT);
        addr.write(builder);
    }

    public static ComponentUUID<?> read(CompoundTag tag) {
        int ordinal = tag.getInt(ComponentTracker.PREFIX);
        ComponentTracker[] types = ComponentTracker.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + tag 
                +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(tag);
    }

    public static ComponentUUID<?> read(ByteBuf buffer) {
        int ordinal = buffer.readInt();
        ComponentTracker[] types = ComponentTracker.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + buffer 
                + " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(buffer);
    }

    public static ComponentUUID<?> read(Dynamic<?> dyn) {
        int ordinal = dyn.get(ComponentTracker.PREFIX).asInt(-1);
        ComponentTracker[] types = ComponentTracker.values();
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
        for(int x = 0; x < ComponentTracker.values().length; x++)
            ComponentTracker.values()[x].clear();
    }

    private final Class<? extends ComponentUUID<?>> clazz; 
    private final Class<? extends IAttachmentHolder> referent;
    private WeakReference<Constructor<? extends ComponentUUID<?>>> tagCtor = new WeakReference<>(null);;
    private WeakReference<Constructor<? extends ComponentUUID<?>>> byteBufCtor = new WeakReference<>(null);
    private WeakReference<Constructor<? extends ComponentUUID<?>>> dynamicCtor = new WeakReference<>(null);;

    <T extends ComponentUUID<T>> ComponentTracker(Class<T> clazz, Class<? extends IAttachmentHolder> referent) {
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
        if(clazz == null) throw new NullPointerException("Cannot instantiate GridUUID '" + this + "' because this enum member hasn't been configured!");
        try {
            if(tagCtor.refersTo(null)) tagCtor = new WeakReference<>(clazz.getDeclaredConstructor(CompoundTag.class));
            return tagCtor.get().newInstance(tag);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + tag.getClass().getName());
        }
    }

    private ComponentUUID<?> createUUID(ByteBuf buffer) {
        if(clazz == null) throw new NullPointerException("Cannot instantiate GridUUID " + this + " because this enum member hasn't been configured!");
        try {
            if(byteBufCtor.refersTo(null)) byteBufCtor = new WeakReference<>(clazz.getDeclaredConstructor(ByteBuf.class));
            return byteBufCtor.get().newInstance(buffer);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + buffer.getClass().getName());
        }
    }

    private ComponentUUID<?> createUUID(Dynamic<?> dyn) {
        if(clazz == null) throw new NullPointerException("Cannot instantiate GridUUID " + this + " because this enum member hasn't been configured!");
        try {
            if(dynamicCtor.refersTo(null)) dynamicCtor = new WeakReference<>(clazz.getDeclaredConstructor(Dynamic.class));
            return dynamicCtor.get().newInstance(dyn);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + dyn.getClass().getName());
        }
    }

    public enum ComponentHierarchy implements StringRepresentable {


        COMPONENT_LINK(2, ComponentLink.class),
        COMPOSING_CIRCUIT(0, Circuit.class, COMPONENT_LINK),
        EMITTER_NODE(3, Node.class, COMPOSING_CIRCUIT),
        ANCILLARY_NODE(4, AncillaryNode.class, EMITTER_NODE),
        DISCRETE_COMPONENT(1, DiscreteComponent.class, COMPOSING_CIRCUIT, COMPONENT_LINK),
        TERMINAL(5, Terminal.class, DISCRETE_COMPONENT),
        NONE(20, null);

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

        public static class ComponentUnqueryableException extends RuntimeException {
            public ComponentUnqueryableException(ComponentUUID<?> address) {
                super("Component of type '" + address.getHierarchyType() + "' cannot be queried!");
            }
        }

        public static class UnexpectedReferentException extends RuntimeException {
            public UnexpectedReferentException(GridConstruct referent, ComponentHierarchy expected) {
                super("Got '" + referent.getClass().getSimpleName() + "' of type '" + referent.getHierarchyType() + "', but operation expected the referent '" + expected + "'");
            }
        }
    }
}