package com.quattage.mechano.foundation.api.landmark.classifier;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.anchor.DispatchedAnchorNode;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class VoxelUUID extends GridUUID {

    private BlockPos pos;
    private int index;

    public VoxelUUID(BlockPos pos, int index) {
        this.pos = pos;
        this.index = index;
    }

    public VoxelUUID(CompoundTag tag) {
        this.pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        this.index = tag.getByte("i");
    }

    public VoxelUUID(ByteBuf buffer) {
        this.pos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        this.index = buffer.readByte();
    }

    public VoxelUUID(Dynamic<?> dyn) {
        this.pos = new BlockPos(dyn.get("x").asInt(0), dyn.get("y").asInt(0), dyn.get("z").asInt(0));
        this.index = dyn.get("i").asInt(0);
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        List<ServerPlayer> playersNearby = ((ServerLevel)player.level())
            .getChunkSource().chunkMap.getPlayers(new ChunkPos(getBlockPos(player.level())), false);
        if(playersNearby == null || playersNearby.isEmpty()) return false;
        for(ServerPlayer sp : playersNearby)
            if(sp.getId() == player.getId()) return true;
        return false;
    }

    @Override
    public GridUUID indexedCopy(int index) {
        return new VoxelUUID(this.pos, index);
    }

    @Override
    public UUIDDiscriminator getDiscriminatorType() {
        return UUIDDiscriminator.VOXEL;
    }

    @Override
    public BlockPos getBlockPos(LevelReader world) {
        return pos;
    }

    @Override
    public Vec3 getPos(LevelReader world) {
        return Vec3.atCenterOf(pos);
    }
    
    @Override
    public Vec3 getPos(LevelReader world, float pTicks) {
        return getPos(world);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float ox, float oy, float oz) {
        return new Vec3(
            pos.getX() + ox,
            pos.getY() + oy,
            pos.getZ() + oz
        );
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz) {
        return getOffsetPos(world, ox, oy, oz);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public @Nullable AnchorPoint getAnchor(ClientLevel world) {
        BlockEntity be = world.getBlockEntity(getBlockPos(world));
        if(!(be instanceof AnchorPointable aph)) return null;
        if(getIndex() < 0 || getIndex() > aph.getAnchors().size()) 
            return null;
        return aph.getAnchor(getIndex());
    }

    @Override
    public @Nullable AnchorPointable<?> getAnchorPoints(LevelReader world) {
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof AnchorPointable aph ? aph : null;
    }

    @Override
    public @Nullable DispatchedAnchorNode getSurrogate(LevelReader world) {
        BlockEntity be = world.getBlockEntity(pos);
        if(!(be instanceof AnchorPointable aph)) return null;
        return aph.getSurrogate();
    }

    @Override
    public @Nullable IAttachmentHolder getDataHolder(LevelReader world) {
        return world.getChunk(getBlockPos(world));
    }

    @Override
    public String describeDataHolder(LevelReader world) {
        IAttachmentHolder holder = getDataHolder(world);
        if(holder instanceof LevelChunk chunk) {
            ChunkPos pos = chunk.getPos();
            return "LevelChunk[" + pos.x + ", " + pos.z + "]";
        }
        return "not_applicable";
    }

    @Override
    public boolean canMoveDynamically() {
        return false;
    }

    @Override
    public float getAttachedSizeFactor(LevelReader world) {
        BlockEntity be = world.getBlockEntity(pos);
        if(be == null) return 1;
        BlockState state = be.getBlockState();
        if(state == null) return 1;
        return (float)state.getShape(world, pos).bounds().getSize();
    }

    @Override
    public void writeTo(CompoundTag tag) {
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putByte("i", (byte)index);
    }

    @Override
    public void writeTo(ByteBuf buffer) {
        buffer
            .writeInt(pos.getX())
            .writeInt(pos.getY())
            .writeInt(pos.getZ())
            .writeByte(index);
    }

    @Override
    public void writeTo(RecordBuilder<?> builder) {
        builder.add("x", pos.getX(), Codec.INT);
        builder.add("y", pos.getY(), Codec.INT);
        builder.add("z", pos.getZ(), Codec.INT);
        builder.add("i", (byte)index, Codec.BYTE);
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof VoxelUUID that)) return false;
        if(this.pos == null) return false;
        return this.pos.equals(that.pos) && this.index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDiscriminatorType(), pos, index);
    }


}
