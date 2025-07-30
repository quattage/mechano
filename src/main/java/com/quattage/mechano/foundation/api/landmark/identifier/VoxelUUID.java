package com.quattage.mechano.foundation.api.landmark.identifier;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class VoxelUUID extends GridUUID {

    protected BlockPos pos;
    protected int index;
    private DataScope scope;

    public VoxelUUID(BlockPos pos, int index) {
        this(pos, index, DataScope.STATIC_CHUNK);
    }

    public VoxelUUID(BlockPos pos, int index, DataScope target) {
        this.pos = pos;
        this.index = clampIndex(index);
        this.scope = target;
    }

    public VoxelUUID(CompoundTag tag) {
        this.pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        this.index = clampIndex(tag.getByte("i"));
        this.scope = DataScope.values()[tag.getByte("s")];
    }

    public VoxelUUID(ByteBuf buffer) {
        this.pos = new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        this.index = buffer.readByte();
        this.scope = DataScope.values()[buffer.readByte()];
    }

    public VoxelUUID(Dynamic<?> dyn) {
        this.pos = new BlockPos(dyn.get("x").asInt(0), dyn.get("y").asInt(0), dyn.get("z").asInt(0));
        this.index = clampIndex(dyn.get("i").asInt(0));
        this.scope = DataScope.values()[dyn.get("s").asInt(0)];
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
    @OnlyIn(Dist.CLIENT)
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        BlockEntity be = world.getBlockEntity(pos);
        if(be == null) return false;
        VoxelShape shape = be.getBlockState().getShape(world, pos);
        if(shape == null) return false;
        return view.isVisible(shape.bounds());
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
        if(!(be instanceof Griddable host)) return null;
        if(getIndex() < 0 || getIndex() > host.getAnchors().size()) 
            return null;
        return host.getAnchor(getIndex());
    }

    @Override
    public @Nullable Griddable<?> getAnchorPoints(LevelReader world) {
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof Griddable aph ? aph : null;
    }

    @Override
    public @Nullable SurrogateNode getSurrogate(LevelReader world) {
        BlockEntity be = world.getBlockEntity(pos);
        if(!(be instanceof Griddable aph)) return null;
        return aph.getSurrogate();
    }

    @Override
    public @Nullable IAttachmentHolder getDataStorageHolder(LevelReader world) {
        if(scope == DataScope.STATIC_CHUNK)
            return world.getChunk(pos);
        scope = DataScope.BLOCKENTITY;
        return world.getBlockEntity(pos);
    }

    @Override
    public String describeDataScope(LevelReader world) {
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(scope == DataScope.STATIC_CHUNK) {
            if(holder instanceof LevelChunk chunk) {
                ChunkPos pos = chunk.getPos();
                return "LevelChunk[" + pos.x + ", " + pos.z + "]";
            }
            return "data scope mismatch (" + scope + ")";
        }
        if(scope == DataScope.BLOCKENTITY) {
            if(holder instanceof BlockEntity be)
                return "BlockEntity '" + be.getClass().getSimpleName() + ", [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
            return "data scope mismatch (" + scope + ")";
        }
        return "not_applicable";
    }

    @Override
    public DataScope getDataScope(LevelReader world) {
        return this.scope;
    }

    @Override
    public void setDataScope(DataScope scope) {
        this.scope = scope;
    }

    @Override
    public boolean canMoveDynamically(LevelReader world) {
        return scope != DataScope.STATIC_CHUNK;
    }

    @Override
    public void sendLevelUpdates(Level world) {
        BlockState state = world.getBlockState(pos);
        world.sendBlockUpdated(pos, state, state, 3);
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
        tag.putByte("s", (byte)scope.ordinal());
    }

    @Override
    public void writeTo(ByteBuf buffer) {
        buffer
            .writeInt(pos.getX())
            .writeInt(pos.getY())
            .writeInt(pos.getZ())
            .writeByte(index)
            .writeByte(scope.ordinal());
    }

    @Override
    public void writeTo(RecordBuilder<?> builder) {
        builder
            .add("x", pos.getX(), Codec.INT)
            .add("y", pos.getY(), Codec.INT)
            .add("z", pos.getZ(), Codec.INT)
            .add("i", (byte)index, Codec.BYTE)
            .add("s", (byte)scope.ordinal(), Codec.BYTE);
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

    @Override
    public String toString() {
        return "VoxelUUID[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ", " + index + ", '" + scope + "']";
    }
}
