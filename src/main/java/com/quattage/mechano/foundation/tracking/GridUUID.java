package com.quattage.mechano.foundation.tracking;

import java.util.Objects;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.foundation.tracking.DataSourceIdentifier.ScopeSpecifier;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public abstract class GridUUID implements ScopeSpecifier {

    public static void assertValidType(GridUUID identifier, CircuitComponent.Type type) {
        if(identifier == null) 
            throw new NullPointerException("UUID validity assertion failed (got null value)");
        if(identifier.getReferentType() != type)
            throw new IllegalArgumentException("UUID validity assertion failed (bad type, expected '" + type + "', got '" + identifier.getReferentType() + "')");
    }

    protected CircuitComponent.Type targetType;
    protected int data;

    public GridUUID(CompoundTag tag) {
        this.targetType = CircuitComponent.Type.values()[tag.getByte("cpt")];
        this.data  = tag.getInt("exd");
    }

    public GridUUID(ByteBuf buffer) {
        this.targetType = CircuitComponent.Type.values()[buffer.readByte()];
        this.data  = buffer.readInt();
    }

    public GridUUID(Dynamic<?> dyn) {
        this.targetType = CircuitComponent.Type.values()[dyn.get("cpt").asInt(0)];
        this.data  = dyn.get("exd").asInt(0);
    }

    public final CircuitComponent.Type getReferentType() {
        return targetType;
    }

    public final int getAdditionalData() {
        return data;
    }

    public void write(CompoundTag tag) {
        tag.putByte("cpt", (byte)targetType.ordinal());
        tag.putInt("exd", data);
    }

    public void write(ByteBuf buffer) {
        buffer.writeByte((byte)targetType.ordinal());
        buffer.writeInt(data);
    }

    public void write(RecordBuilder<?> builder) {
        builder.add("cpt", (byte)targetType.ordinal(), Codec.BYTE);
        builder.add("exd", data, Codec.INT);
    }

    @Override
    public String toString() {
        return "GridUUID[" + getSourceScope() + ", " + getReferentType() + ", (" + describeData() + ")]";
    }

    abstract String describeData();

    





    public static class VoxelUUID extends GridUUID {

        private BlockPos pos;

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
        public DataSourceIdentifier getSourceScope() {
            return DataSourceIdentifier.VOXEL;
        }

        @Override
        public void write(CompoundTag tag) {
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
        }

        @Override
        public void write(ByteBuf buffer) {
            buffer.writeInt(pos.getX()).writeInt(pos.getY()).writeInt(pos.getZ());
        }

        @Override
        public void write(RecordBuilder<?> builder) {
            builder.add("x", pos.getX(), Codec.INT).add("y", pos.getY(), Codec.INT).add("z", pos.getZ(), Codec.INT);
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(!(obj instanceof VoxelUUID that)) return false;
            return this.pos.equals(that.pos) && this.data == that.data && this.targetType == that.targetType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.pos, this.data, this.targetType);
        }

        @Override
        String describeData() {
            return pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + data;
        }
    }





    public static class EntityUUID extends GridUUID {

        private UUID uuid;

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
        public DataSourceIdentifier getSourceScope() {
            return DataSourceIdentifier.ENTITY;
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
            return uuid + ", " + data;
        }
    }
}
