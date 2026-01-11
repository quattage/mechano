package com.quattage.mechano.api.grid.component;

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

    protected @Nullable ComponentHierarchy type;
    protected ComponentBinding[] bindings;

    public ComponentUUID() {}

    public ComponentUUID(CompoundTag tag) {
        if(tag.contains("cpt")) {
            byte idx = tag.getByte("cpt");
            if(idx >= 0) this.type = ComponentHierarchy.values()[idx];
        }
        short bndc = tag.getByte("bndc");
        bindings = new ComponentBinding[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new ComponentBinding(tag.getShort("bd" + x));
    }

    public ComponentUUID(ByteBuf buffer) {
        byte idx = buffer.readByte();
        if(idx >= 0) this.type = ComponentHierarchy.values()[idx];
        short bndc = buffer.readByte();
        bindings = new ComponentBinding[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new ComponentBinding(buffer.readShort());
    }

    public ComponentUUID(Dynamic<?> dyn) {
        byte idx = dyn.get("cpt").asByte((byte)-1);
        if(idx >= 0) this.type = ComponentHierarchy.values()[idx];
        short bndc = dyn.get("bndc").asByte((byte)0);
        bindings = new ComponentBinding[bndc];
        for(int x = 0; x < bindings.length; x++)
            bindings[x] = new ComponentBinding(dyn.get("bd" + x).asShort((short) 0));
    }

    public abstract T copy();

    public void write(CompoundTag tag) {
        tag.putByte("cpt", type == null ? (byte)-1 : (byte)type.ordinal());
        tag.putByte("bndc", (byte) bindings.length);
        for(int x = 0; x < bindings.length; x++)
            tag.putShort("bd" + x, bindings[x].value);
    }

    public void write(ByteBuf buffer) {
        buffer.writeByte(type == null ? (byte)-1 : (byte)type.ordinal());
        buffer.writeByte((byte) bindings.length);
        for(int x = 0; x < bindings.length; x++)
            buffer.writeShort(bindings[x].value);
    }

    public void write(RecordBuilder<?> builder) {
        builder.add("cpt", type == null ? (byte)-1 : (byte)type.ordinal(), Codec.BYTE);
        builder.add("bndc", (byte) bindings.length, Codec.BYTE);
        for(int x = 0; x < bindings.length; x++)
            builder.add("bd" + x, bindings[x].value, Codec.SHORT);
    }

    @SuppressWarnings("unchecked")
    public T withBinding(int value) {
        if(bindings.length > 254) throw new IllegalStateException("Couldn't apply binding to " + this + " - This UUID is full!");
        ComponentBinding[] copy = new ComponentBinding[bindings.length + 1];
        System.arraycopy(bindings, 0, copy, 0, bindings.length);
        copy[bindings.length] = new ComponentBinding(value);
        this.bindings = copy;
        return (T) this;
    }

    public ComponentHierarchy getHierarchyType() {
        return type;
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
        return "ComponentUUID[" + getTrackerScope() + ", " + getHierarchyType() + ", (" + describeData() + ")]";
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
            out.type = this.type;
            out.bindings = new ComponentBinding[this.bindings.length];
            for(int x = 0; x < bindings.length; x++)
                out.bindings[x] = new ComponentBinding(this.bindings[x].value);
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
            out.type = this.type;
            out.bindings = new ComponentBinding[this.bindings.length];
            for(int x = 0; x < bindings.length; x++)
                out.bindings[x] = new ComponentBinding(this.bindings[x].value);
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
    }

    public static class ComponentBinding {

        private final short value;
        private ComponentBinding(int value) {
            this.value = EsoMath.toShortClamped(value);
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof ComponentBinding that)) return false;
            return this.value == that.value;
        }

        public int get() { return (int) value; }
        @Override public int hashCode() { return value; }
        @Override public String toString() { return "" + value; }
    }
}
