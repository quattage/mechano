package com.quattage.mechano.foundation.tracking;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridHierarchy;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.foundation.tracking.UUIDSourceType.ScopeSpecifier;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A unique identifier which points to a {@link CircuitComponent} object.
 * Serialized using the {@link UUIDSourceType}
 */
public abstract class GridUUID implements ScopeSpecifier, GridIdentifiable<GridUUID> {

    public static void assertValidType(GridUUID identifier, GridHierarchy type) {
        if(identifier == null) 
            throw new NullPointerException("UUID validity assertion failed (got null value)");
        if(identifier.getReferentType() != type)
            throw new IllegalArgumentException("UUID validity assertion failed (bad type, expected '" + type + "', got '" + identifier.getReferentType() + "')");
    }

    protected @Nullable GridHierarchy type;
    protected short bindingA = Short.MIN_VALUE;
    protected short bindingB = Short.MIN_VALUE;

    public GridUUID() {}

    public GridUUID(CompoundTag tag) {
        if(tag.contains("cpt")) {
            byte idx = tag.getByte("cpt");
            if(idx >= 0) this.type = GridHierarchy.values()[idx];
        }
        this.bindingA  = tag.getShort("ba");
        this.bindingB = tag.getShort("bb");
    }

    public GridUUID(ByteBuf buffer) {
        byte idx = buffer.readByte();
        if(idx >= 0) this.type = GridHierarchy.values()[idx];
        this.bindingA = buffer.readShort();
        this.bindingB = buffer.readShort();
    }

    public GridUUID(Dynamic<?> dyn) {
        byte idx = dyn.get("cpt").asByte((byte)-1);
        if(idx >= 0) this.type = GridHierarchy.values()[idx];
        this.bindingA = dyn.get("ba").asShort(Short.MIN_VALUE);
        this.bindingB = dyn.get("bb").asShort(Short.MIN_VALUE);
    }

    public GridUUID withBinding(GridHierarchy type, int binding) {
        this.type = type;
        this.bindingA = clampedUnsigned(binding);
        this.bindingB = Short.MIN_VALUE;
        return this;
    }

    public GridUUID withBinding(GridHierarchy type, int bindingA, int bindingB) {
        this.type = type;
        this.bindingA = clampedUnsigned(bindingA);
        this.bindingB = clampedUnsigned(bindingB);
        return this;
    }

    private short clampedUnsigned(int binding) {
        return (short)(Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (short)Math.abs(binding))) - Short.MAX_VALUE);
    }

    public GridHierarchy getType() {
        return type;
    }

    @Nullable public Griddable<?> getTargetSource(Grid grid) {
        return getTargetSource(grid.getWorld());
    }
    @Override
    @Nullable public abstract Griddable<?> getTargetSource(LevelReader world);

    public final GridHierarchy getReferentType() {
        return type;
    }

    public final int getBindingA() {
        return bindingA + Short.MAX_VALUE;
    }

    public final int getBindingB() {
        return bindingB + Short.MAX_VALUE;
    }

    public void write(CompoundTag tag) {
        tag.putByte("cpt", type == null ? (byte)-1 : (byte)type.ordinal());
        tag.putShort("ba", bindingA);
        tag.putShort("bb", bindingB);
    }

    public void write(ByteBuf buffer) {
        buffer.writeByte(type == null ? (byte)-1 : (byte)type.ordinal());
        buffer.writeShort(bindingA);
        buffer.writeShort(bindingB);
    }

    public void write(RecordBuilder<?> builder) {
        builder.add("cpt", type == null ? (byte)-1 : (byte)type.ordinal(), Codec.BYTE);
        builder.add("ba", bindingA, Codec.SHORT);
        builder.add("bb", bindingB, Codec.SHORT);
    }

    public boolean hasBindings() {
        return getBindingA() != -1 || getBindingB() != -1;
    }

    @Override
    public String toString() {
        return "GridUUID[" + getSourceScope() + ", " + getReferentType() + ", (" + describeData() + ")]";
    }

    abstract String describeData();

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof GridUUID that)) return false;
        return this.bindingA == that.bindingA && this.bindingB == that.bindingB && this.type == that.type;
    }

    @Override
    public GridUUID getUUID() {
        return this;
    }



    public static class VoxelUUID extends GridUUID {

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
        public @Nullable Griddable<?> getTargetSource(LevelReader world) {
            BlockEntity be = world.getBlockEntity(pos);
            return be instanceof Griddable<?> gbe ? gbe : null;
        }

        @Override
        public UUIDSourceType getSourceScope() {
            return UUIDSourceType.VOXEL;
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
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(!(obj instanceof VoxelUUID that)) return false;
            return this.pos.equals(that.pos) && this.bindingA == that.bindingA && this.bindingB == that.bindingB && this.type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.pos, this.bindingA, this.bindingB, this.type);
        }

        @Override
        String describeData() {
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + getBindingA() + ", " + getBindingB();
        }
    }





    public static class EntityUUID extends GridUUID {

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
        public @Nullable Griddable<?> getTargetSource(LevelReader world) {
            Entity e = world.isClientSide() 
                // accessible via the access transformer
                ? ((ClientLevel)world).entityStorage.getEntityGetter().get(uuid) 
                : ((ServerLevel)world).getEntity(uuid);
            return e instanceof Griddable<?> ge ? ge : null;
        }

        @Override
        public UUIDSourceType getSourceScope() {
            return UUIDSourceType.ENTITY;
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
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(!(obj instanceof EntityUUID that)) return false;
            return this.uuid.equals(that.uuid) && this.bindingA == that.bindingA && this.bindingB == that.bindingB && this.type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.uuid, this.bindingA, this.bindingB, this.type);
        }
        
        @Override
        String describeData() {
            return uuid + ", " + getBindingA() + ", " + getBindingB();
        }
    }
}
