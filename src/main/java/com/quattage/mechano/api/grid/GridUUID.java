package com.quattage.mechano.api.grid;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.GridConstruct.GridReferent;
import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.foundation.numeric.EsoMath;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap.TrackedEntity;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * An instance of this class can be used to locate a {@link CircuitComponent}
 * object from anywhere in the world, as long as said CircuitComponent
 * belongs to an identifiable {@link Griddable} instance currently loaded
 * by the level. Use the {@link GridTracking} to instantiate, serialize 
 * and use ComponentUUIDs.
 */
public abstract class GridUUID<T extends GridUUID<T>> implements GridReferent<T> {

    protected UUIDComposite[] bindings;

    public GridUUID() {
        this.bindings = new UUIDComposite[0];
    }

    public GridUUID(CompoundTag tag) {
        short bndc = tag.getByte("bndc");
        bindings = new UUIDComposite[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new UUIDComposite(x, tag);
    }

    public GridUUID(ByteBuf buffer) {
        short bndc = buffer.readByte();
        bindings = new UUIDComposite[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new UUIDComposite(x, buffer);
    }

    public GridUUID(Dynamic<?> dyn) {
        short bndc = dyn.get("bndc").asByte((byte)0);
        bindings = new UUIDComposite[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new UUIDComposite(x, dyn);
    }

    public GridUUID(RandomSource random) {
        short bndc = EsoMath.toShortClamped(EsoMath.randomInt(random, 0, 15));
        bindings = new UUIDComposite[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new UUIDComposite(random);
    }

    public abstract T copy();
    public abstract T copyAndClearBindings();

    public void write(CompoundTag tag) {
        tag.putByte("bndc", (byte) bindings.length);
        for(int x = 0; x < bindings.length; x++)
            bindings[x].write(x, tag);
    }

    public void write(ByteBuf buffer) {
        buffer.writeByte((byte) bindings.length);
        for(int x = 0; x < bindings.length; x++)
            bindings[x].write(x, buffer);
    }

    public void write(RecordBuilder<?> builder) {
        builder.add("bndc", (byte) bindings.length, Codec.BYTE);
        for(int x = 0; x < bindings.length; x++)
            bindings[x].write(x, builder);
    }

    @SuppressWarnings("unchecked")
    public T withBinding(int value, ComponentHierarchy target) {
        if(bindings.length >= Byte.MAX_VALUE) throw new IllegalStateException("Couldn't apply binding to " + this + " - This UUID is full!");
        UUIDComposite[] copy = new UUIDComposite[bindings.length + 1];
        System.arraycopy(bindings, 0, copy, 1, bindings.length);
        copy[0] = new UUIDComposite(value, target);
        this.bindings = copy;
        return (T) this;
    }
    
    @SuppressWarnings("unchecked")
    public T withoutBindings() {
        this.bindings = null;
        return (T) this;
    }

    public ComponentHierarchy getTargetType() {
        if(!hasBindings()) return ComponentHierarchy.STRANGER;
        return bindings[bindings.length - 1].getHierarchyType();
    }

    public UUIDComposite getBinding(int index) {
        if(index < 0 || index >= bindings.length) return UUIDComposite.EMPTY;
        UUIDComposite binding = bindings[index];
        return binding == null ? UUIDComposite.EMPTY : binding;
    }

    public int getBindingCount() {
        return bindings == null ? 0 : bindings.length;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getUUID() {
        return (T) this;
    }

    abstract String describeData();

    public boolean hasBindings() {
        return bindings != null && bindings.length > 0;
    }

    protected boolean areBindingsEqual(GridUUID<?> other) {
        if(!hasBindings() && !other.hasBindings()) return true;
        if(this.bindings.length != other.bindings.length) return false;
        for(int x = 0; x < bindings.length; x++) {
            if(!this.bindings[x].equals(other.bindings[x]))
                return false;
        }
        return true;
    }

    public String describeBindings() {
        if(!hasBindings()) return "[No bindings]";
        String out = "[";
        for(int x = 0; x < bindings.length; x++)
            out += bindings[x] + ", ";
        return out.substring(0, out.length() - 2) + "]";
    }

    @Override
    public String toString() {
        return getTrackerScope() + "_UUID: " + describeData() + ", " + describeBindings();
    }

    /**
     * A {@link GridUUID} whose primary coordinate is a 
     * {@link BlockPos} for targeting voxels in the Minecraft level.
     */
    public static class VoxelUUID extends GridUUID<VoxelUUID> {

        private BlockPos pos;

        public VoxelUUID(int x, int y, int z) {
            this(new BlockPos(x, y, z));
        }

        public VoxelUUID(BlockPos pos) {
            Objects.requireNonNull(pos);
            this.pos = pos;
        }

        public VoxelUUID(CompoundTag tag) {
            super(tag);
            this.pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        }

        public VoxelUUID(ByteBuf buffer) {
            super(buffer);
            this.pos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        }

        public VoxelUUID(Dynamic<?> dyn) {
            super(dyn);
            this.pos = new BlockPos(dyn.get("x").asInt(0), dyn.get("y").asInt(0), dyn.get("z").asInt(0));
        }

        public VoxelUUID(RandomSource random) {
            super(random);
            this.pos = new BlockPos(
                EsoMath.randomInt(random, -512, 512), 
                EsoMath.randomInt(random, -64, 64), 
                EsoMath.randomInt(random, -512, 512)
            );
        }

        @Override
        public VoxelUUID copy() {
            VoxelUUID out = copyAndClearBindings();
            out.bindings = new UUIDComposite[this.bindings.length];
            for(int x = 0; x < bindings.length; x++)
                out.bindings[x] = this.bindings[x].copy();
            return out;
        }

        @Override
        public VoxelUUID copyAndClearBindings() {
            return new VoxelUUID(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        }

        @Override
        @SuppressWarnings("unchecked")
        public @Nullable Griddable<VoxelUUID> getProviderSource(LevelReader world) {
            BlockEntity be = world.getBlockEntity(pos);
            return be instanceof Griddable<?> gbe ? (Griddable<VoxelUUID>) gbe : null;
        }

        @Override
        public GridReferent<?> getProviderSource() {
            Mechano.LOGGER.warn("Attempted to get a provider source from a UUID without a reference to the world. This call will not do anything and immediatley return null.");
            return null;
        }

        @Override
        public boolean isBeingTrackedBy(ServerPlayer sp) {
            return sp.getChunkTrackingView().contains(new ChunkPos(this.pos));
        }

        @Override
        public GridTracking getTrackerScope() {
            return GridTracking.VOXEL;
        }

        @Override
        public void write(CompoundTag tag) {
            super.write(tag);
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
        }

        @Override
        public void write(ByteBuf buffer) {
            super.write(buffer);
            buffer.writeInt(pos.getX()).writeInt(pos.getY()).writeInt(pos.getZ());
        }

        @Override
        public void write(RecordBuilder<?> builder) {
            super.write(builder);
            builder.add("x", pos.getX(), Codec.INT).add("y", pos.getY(), Codec.INT).add("z", pos.getZ(), Codec.INT);
        }

        @Override
        String describeData() {
            return "[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(!(obj instanceof VoxelUUID that)) return false;
            if(!this.pos.equals(that.pos)) return false;
            return this.areBindingsEqual(that);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, Arrays.hashCode(bindings));
        }

        @Override
        public BlockPos getBlockPos() {
            return pos;
        }
    }

    /**
     * A {@link GridUUID} whose primary coordinate is a {@link UUID 64-bit UUID} 
     * for refering to entities loaded by the Minecraft level.
     */
    public static class EntityUUID extends GridUUID<EntityUUID> {

        private UUID uuid;

        public EntityUUID(Entity e) {
            Objects.requireNonNull(e);
            this.uuid = e.getUUID();
            if(this.uuid == null) 
                throw new NullPointerException("Couldn't instantiate EntityUUID - The entity '" + e + "' didn't return a valid UUID!");
        }

        public EntityUUID(UUID uuid) {
            Objects.requireNonNull(uuid);
            this.uuid = uuid;
        }

        public EntityUUID(CompoundTag tag) {
            super(tag);
            this.uuid = new UUID(tag.getLong("um"), tag.getLong("ul"));
        }

        public EntityUUID(ByteBuf buffer) {
            super(buffer);
            this.uuid = new UUID(buffer.readLong(), buffer.readLong());
        }

        public EntityUUID(Dynamic<?> dyn) {
            super(dyn);
            this.uuid = new UUID(dyn.get("um").asLong(0), dyn.get("ul").asLong(0));
        }

        public EntityUUID(RandomSource random) {
            super(random);
            this.uuid = UUID.randomUUID();
        }

        @Override
        public EntityUUID copy() {
            EntityUUID out = copyAndClearBindings();
            out.bindings = new UUIDComposite[this.bindings.length];
            for(int x = 0; x < bindings.length; x++)
                out.bindings[x] = this.bindings[x].copy();
            return out;
        }

        @Override
        public EntityUUID copyAndClearBindings() {
            return new EntityUUID(new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
        }

        @Override
        public @Nullable GridReferent<?> getProviderSource(LevelReader world) {
            Entity e = world.isClientSide() 
                // accessible via the access transformer
                ? ((ClientLevel) world).entityStorage.getEntityGetter().get(uuid) 
                : ((ServerLevel) world).getEntity(uuid);
            return e instanceof Griddable<?> ge ? ge : null;
        }

        @Override
        public GridReferent<?> getProviderSource() {
            Mechano.LOGGER.warn("Attempted to get a provider source from a UUID without a reference to the world. This call will not do anything and immediatley return null.");
            return null;
        }

        @Override
        public boolean isBeingTrackedBy(ServerPlayer sp) {
            Objects.requireNonNull(sp);
            ServerLevel world = (ServerLevel) sp.level();
            ServerChunkCache cache = world.getChunkSource();
            if(cache == null) return false;
            Entity e = world.getEntity(uuid);
            if(e == null) return false;
            TrackedEntity tracker = cache.chunkMap.entityMap.get(e.getId());
            return tracker == null ? false : tracker.seenBy.contains(sp.connection);
        }

        @Override
        public GridTracking getTrackerScope() {
            return GridTracking.ENTITY;
        }

        @Override
        public void write(CompoundTag tag) {
            super.write(tag);
            tag.putLong("um", uuid.getMostSignificantBits());
            tag.putLong("ul", uuid.getLeastSignificantBits());
        }

        @Override
        public void write(ByteBuf buffer) {
            super.write(buffer);
            buffer.writeLong(uuid.getMostSignificantBits());
            buffer.writeLong(uuid.getLeastSignificantBits());
        }

        @Override
        public void write(RecordBuilder<?> builder) {
            super.write(builder);
            builder.add("um", uuid.getMostSignificantBits(), Codec.LONG);
            builder.add("ul", uuid.getLeastSignificantBits(), Codec.LONG);
        }

        @Override
        String describeData() {
            return "@" + uuid.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(!(obj instanceof EntityUUID that)) return false;
            if(!this.uuid.equals(that.uuid)) return false;
            return this.areBindingsEqual(that);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, Arrays.hashCode(bindings));
        }

        @Override
        public BlockPos getBlockPos() {
            return null;
        }
    }

    /**
     * A single sub-coordinate with that points towards
     * a specific {@link CircuitComponent} of a speciifc
     * {@link ComponentHierarchy hierarchical type} at an
     * arbitrary index. A series of bindings are used to
     * traverse the hierarchy structure for a specific
     * in-world circuit accessible via the {@link Griddable}.
     */
    public static class UUIDComposite {

        private final short value;
        private final ComponentHierarchy type;

        public static final UUIDComposite EMPTY = new UUIDComposite(-1, ComponentHierarchy.STRANGER);

        private UUIDComposite(int value, ComponentHierarchy target) {
            this.value = EsoMath.toShortClamped(value);
            this.type = target;
        }

        private UUIDComposite(int idx, CompoundTag tag) {
            this.value = tag.getShort("v" + idx);
            this.type = ComponentHierarchy.values()[tag.getByte("t" + idx)];
        }

        private UUIDComposite(int idx, ByteBuf buffer) {
            this.value = buffer.readShort();
            this.type = ComponentHierarchy.values()[buffer.readByte()];
        }

        private UUIDComposite(int idx, Dynamic<?> dyn) {
            this.value = dyn.get("v" + idx).asShort((short) 0);
            this.type = ComponentHierarchy.values()[dyn.get("t" + idx).asByte((byte)(ComponentHierarchy.values().length - 1))];
        }

        private UUIDComposite(RandomSource random) {
            this.value = EsoMath.toShortClamped(EsoMath.randomInt(random, Short.MIN_VALUE, Short.MAX_VALUE));
            this.type = ComponentHierarchy.values()[EsoMath.randomInt(random, 0, ComponentHierarchy.values().length - 1)];
        }

        protected void write(int idx, CompoundTag tag) {
            tag.putShort("v" + idx, value);
            tag.putByte("t" + idx, (byte) type.ordinal());
        }

        protected void write(int idx, ByteBuf buffer) {
            buffer.writeShort(value);
            buffer.writeByte(type.ordinal());
        }

        protected void write(int idx, RecordBuilder<?> builder) {
            builder.add("v" + idx, value, Codec.SHORT);
            builder.add("t" + idx, (byte) type.ordinal(), Codec.BYTE);
        }

        public UUIDComposite copy() {
            return new UUIDComposite(this.value, this.type);
        }

        public boolean isValid() { return value >= 0 && type != null && type != ComponentHierarchy.STRANGER; }
        
        public int get() { return (int) value; }
        public ComponentHierarchy getHierarchyType() { return type; }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof UUIDComposite that)) return false;
            return this.value == that.value && this.type == that.type;
        }

        @Override public int hashCode() { return Objects.hash(value, type); }
        @Override public String toString() { return "(" + type + ": " + value + ")"; }
    }
}
