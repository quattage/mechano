

package com.quattage.mechano.foundation.api.landmark.base;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.GridNode.Tracker;

import io.netty.buffer.ByteBuf;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

/**
 * Basic implementation of {@link NodeIdentifiable} that provides hashing, 
 * equivalence, and serialization methods for a block position and an index value.
 * This abstract class is used by {@link GridNode}, {@link GridNode.Address}, {@link GridNode.Tracker},
 * and {@link com.quattage.mechano.foundation.api.landmark.client.AnchorPoint AnchorPoint} - All of these classes
 * share the same hashing, equivalence, and serialization methods, so they can all be used to query {@link  com.quattage.mechano.foundation.api.PowerGrid PowerGrid}
 */
public abstract class NodeIdentifier implements NodeIdentifiable {


    /**
     * The maximum amount of nodes that can occupy the same single block space
     */
    public static final int MAX_OCCUPANCY = 8; 

    protected final BlockPos pos;
    protected byte index;

    public NodeIdentifier(BlockPos pos, int index) {
        Objects.requireNonNull(pos);
        this.pos = pos;
        this.index = (byte)(index < 0 ? 0 : (index >= NodeIdentifier.MAX_OCCUPANCY ? NodeIdentifier.MAX_OCCUPANCY - 1 : index));
    }

    public NodeIdentifier(int x, int y, int z, int index) {
        this.pos = new BlockPos(x, y, z);
        this.index = (byte)(index < 0 ? 0 : (index >= NodeIdentifier.MAX_OCCUPANCY ? NodeIdentifier.MAX_OCCUPANCY - 1 : index));
    }

    @Override
    public BlockPos getPos() {
        return this.pos;
    }

    public String toString() {
        return "Address [" + getX() + ", " + getY() + ", " + getZ() + ", " + getIndex() + "]";
    }

    @Override
    public int getIndex() {
        return index;
    }

    public boolean isLocatedAt(BlockPos pos) {
        return this.pos.equals(pos);
    }

    public boolean isLocatedAt(int x, int y, int z) {
        return this.pos.getX() == x && this.pos.getY() == y && this.pos.getZ() == z;
    }

    public boolean isIndex(int index) {
        return this.index == getIndex();
    }

    public boolean isIndex(byte index) {
        return this.index == index;
    }

    public boolean equals(Object other) {
        if(this == other) return true;
        if(!(other instanceof NodeIdentifiable that)) return false;
        return this.pos.equals(that.getPos()) && this.index == that.getIndex();
    }

    public int hashCode() {
        return pos.hashCode() * 31 + index;
    }








    /**
     * A dummy implementation of {@link NodeIdentifier} useful as
     * a stand-in replacement for {@link GridNode} or {@link com.quattage.mechano.foundation.api.landmark.client.AnchorPoint AnchorPoint} instances when
     * retrieving them from the {@link com.quattage.mechano.foundation.api.PowerGrid PowerGrid}
     * or when sending packets.
     */
    public static class Key extends NodeIdentifier {

        public static final Codec<Key> CODEC = Codec.INT_STREAM.comapFlatMap(
            read -> Util.fixedSize(read, 4).map(data -> new Key(data[0], data[1], data[2], data[3])),
            write -> IntStream.of(write.getX(), write.getY(), write.getZ(), write.getIndex())
        );

        public static final StreamCodec<ByteBuf, Key> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Key decode(ByteBuf buffer) {
                return new Key(
                    FriendlyByteBuf.readBlockPos(buffer),
                    buffer.readByte()
                );
            }
            @Override
            public void encode(ByteBuf buffer, Key value) {
                FriendlyByteBuf.writeBlockPos(buffer, value.pos);
                buffer.writeByte(value.index);
            }
        };

        public static Key loadFrom(CompoundTag tag) {
            if(tag == null) throw new NullPointerException("Cannot instantiate a Provisional GridNode from a null CompoundTag!");
            if(!(tag.contains("x") && tag.contains("y") && tag.contains("z") && tag.contains("i"))) 
                throw new IllegalArgumentException("Cannot instantiate a Provisional GridNode from compound '" + tag + "' - this CompoundTag is missing the required data!");
            BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            return new Key(pos, tag.getByte("i"));
        }

        public Key(BlockPos pos) {
            super(pos, 0);
        }

        public Key(BlockPos pos, int index) {
            super(pos, index);
        }

        public Key(int x, int y, int z, int index) {
            super(x, y, z, index);
        }

        public void setIndex(int index) {
            this.index = (byte)(index < 0 ? 0 : (index >= NodeIdentifier.MAX_OCCUPANCY ? NodeIdentifier.MAX_OCCUPANCY - 1 : index));
        }

		@Override
		public Tracker makeTrackable() {
			throw new UnsupportedOperationException("Dummy addresses aren't trackable!");
		}
    }




    /**
     * A hashable 
     */
    public static class UniversalKey extends Key {

        private UUID entityTarget;

        public UniversalKey(BlockPos pos, UUID entityTarget) {
            super(pos);
            this.entityTarget = entityTarget;
        }

        public UniversalKey(BlockPos pos, int index, UUID entityTarget) {
            super(pos, index);
            this.entityTarget = entityTarget;
        }

        public UniversalKey(int x, int y, int z, int index, UUID entityTarget) {
            super(x, y, z, index);
            this.entityTarget = entityTarget;
        }

        public UUID getEntityUUID() {
            return entityTarget;
        }

        public Entity getEntity(ServerLevel world) {
            return world.getEntity(entityTarget);
        }

        @Override
        public int hashCode() {
            return super.hashCode() * 31 + entityTarget.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if(this == other) return true;
            if(!(other instanceof UniversalKey that)) return false;
            return this.index == that.index && this.getPos().equals(that.getPos()) && this.entityTarget.equals(that.entityTarget);
        }
    }
}
