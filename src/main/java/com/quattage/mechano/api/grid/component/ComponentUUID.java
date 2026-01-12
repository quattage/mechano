package com.quattage.mechano.api.grid.component;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.ComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.component.GridConstruct.GridReferent;
import com.quattage.mechano.foundation.numeric.EsoMath;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap.TrackedEntity;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A unique identifier which points to a {@link CircuitComponent} object.
 * Serialized using the {@link ComponentTracker#CODEC tracker codec}
 */
public abstract class ComponentUUID<T extends ComponentUUID<T>> implements GridReferent<T> {

    protected ComponentBinding[] bindings;

    public ComponentUUID() {
        this.bindings = new ComponentBinding[0];
    }

    public ComponentUUID(CompoundTag tag) {
        short bndc = tag.getByte("bndc");
        bindings = new ComponentBinding[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new ComponentBinding(x, tag);
    }

    public ComponentUUID(ByteBuf buffer) {
        short bndc = buffer.readByte();
        bindings = new ComponentBinding[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new ComponentBinding(x, buffer);
    }

    public ComponentUUID(Dynamic<?> dyn) {
        short bndc = dyn.get("bndc").asByte((byte)0);
        bindings = new ComponentBinding[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new ComponentBinding(x, dyn);
    }

    public abstract T copy();

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
        ComponentBinding[] copy = new ComponentBinding[bindings.length + 1];
        System.arraycopy(bindings, 0, copy, 0, bindings.length);
        copy[bindings.length] = new ComponentBinding(value, target);
        this.bindings = copy;
        return (T) this;
    }

    public ComponentBinding getBinding(int index) {
        if(index <= 0 || index >= bindings.length) return ComponentBinding.EMPTY;
        ComponentBinding binding = bindings[index];
        return binding == null ? ComponentBinding.EMPTY : binding;
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

    protected boolean areBindingsEqual(ComponentUUID<?> other) {
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
        return out.substring(0, out.length() - 1) + "]";
    }

    @Override
    public String toString() {
        return getTrackerScope() + "_UUID: " + describeData() + ", " + describeBindings();
    }

    /**
     * A {@link ComponentUUID} whose primary coordinate is a 
     * {@link BlockPos} for targeting voxels in the Minecraft level.
     */
    public static class VoxelUUID extends ComponentUUID<VoxelUUID> {

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

        @Override
        public VoxelUUID copy() {
            VoxelUUID out = new VoxelUUID(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
            out.bindings = new ComponentBinding[this.bindings.length];
            for(int x = 0; x < bindings.length; x++)
                out.bindings[x] = this.bindings[x].copy();
            return out;
        }

        @Override
        @SuppressWarnings("unchecked")
        public @Nullable Griddable<VoxelUUID> getProviderSource(LevelReader world) {
            BlockEntity be = world.getBlockEntity(pos);
            return be instanceof Griddable<?> gbe ? (Griddable<VoxelUUID>) gbe : null;
        }

        @Override
        public boolean isBeingTrackedBy(ServerPlayer sp) {
            Objects.requireNonNull(sp);
            ChunkTrackingView view = sp.getChunkTrackingView();
            return view != null && view.contains(pos.getX(), pos.getZ());
        }

        @Override
        public ComponentTracker getTrackerScope() {
            return ComponentTracker.VOXEL;
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
    }

    /**
     * A {@link ComponentUUID} whose primary coordinate is a {@link UUID 64-bit UUID} 
     * for refering to entities loaded by the Minecraft level.
     */
    public static class EntityUUID extends ComponentUUID<EntityUUID> {

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

        @Override
        public EntityUUID copy() {
            EntityUUID out = new EntityUUID(new UUID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits()));
            out.bindings = new ComponentBinding[this.bindings.length];
            for(int x = 0; x < bindings.length; x++)
                out.bindings[x] = this.bindings[x].copy();
            return out;
        }

        @Override
        public @Nullable Griddable<?> getProviderSource(LevelReader world) {
            Entity e = world.isClientSide() 
                // accessible via the access transformer
                ? ((ClientLevel) world).entityStorage.getEntityGetter().get(uuid) 
                : ((ServerLevel) world).getEntity(uuid);
            return e instanceof Griddable<?> ge ? ge : null;
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
        public ComponentTracker getTrackerScope() {
            return ComponentTracker.ENTITY;
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
    }

    public static class ComponentBinding {

        private final short value;
        private final ComponentHierarchy type;

        public static final ComponentBinding EMPTY = new ComponentBinding(-1, ComponentHierarchy.NONE);

        private ComponentBinding(int value, ComponentHierarchy target) {
            this.value = EsoMath.toShortClamped(value);
            this.type = target;
        }

        private ComponentBinding(int idx, CompoundTag tag) {
            this.value = tag.getShort("cbv" + idx);
            this.type = ComponentHierarchy.values()[tag.getByte("cbt" + idx)];
        }

        private ComponentBinding(int idx, ByteBuf buffer) {
            this.value = buffer.readShort();
            this.type = ComponentHierarchy.values()[buffer.readByte()];
        }

        private ComponentBinding(int idx, Dynamic<?> dyn) {
            this.value = dyn.get("cbv" + idx).asShort((short) 0);
            this.type = ComponentHierarchy.values()[dyn.get("cbt" + idx).asByte((byte)(ComponentHierarchy.values().length - 1))];
        }

        protected void write(int idx, CompoundTag tag) {
            tag.putShort("cbv" + idx, value);
            tag.putByte("cbt" + idx, (byte) type.ordinal());
        }

        protected void write(int idx, ByteBuf buffer) {
            buffer.writeShort(value);
            buffer.writeByte(type.ordinal());
        }

        protected void write(int idx, RecordBuilder<?> builder) {
            builder.add("cbv" + idx, value, Codec.SHORT);
            builder.add("cbt" + idx, (byte) type.ordinal(), Codec.BYTE);
        }

        public ComponentBinding copy() {
            return new ComponentBinding(this.value, this.type);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof ComponentBinding that)) return false;
            return this.value == that.value;
        }

        public boolean isValid() { return value >= 0 && type != null && type != ComponentHierarchy.NONE; }
        
        public int get() { return (int) value; }
        public ComponentHierarchy getHierarchyType() { return type; }

        @Override public int hashCode() { return value; }
        @Override public String toString() { return value + ", " + type; }
    }
}
