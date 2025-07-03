package com.quattage.mechano.foundation.api.landmark.classifier;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.anchor.DispatchedAnchorNode;
import com.quattage.mechano.foundation.entity.GriddableEntityAttachment;
import com.quattage.mechano.foundation.helper.VectorHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class EntityUUID extends GridUUID {

    private UUID uuid;
    private int index;
    private @Nullable AnchorPointable<? extends Entity> holder;

    public EntityUUID(UUID uuid, int index) {
        this.uuid = uuid;
        this.index = index;
    }

    public EntityUUID(CompoundTag tag) {
        this.uuid = tag.getUUID("uid");
        this.index = tag.getByte("i");
    }

    public EntityUUID(ByteBuf buffer) {
        this.uuid = new UUID(buffer.readLong(), buffer.readLong());
        this.index = buffer.readByte();
    }

    public EntityUUID(Dynamic<?> dyn) {
        this.uuid = new UUID(dyn.get("uida").asLong(0), dyn.get("uidb").asLong(0));
        this.index = dyn.get("i").asInt(0);
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        getDataHolder(player.level());
        if(!(player.level().getChunkSource() instanceof ServerChunkCache chunkCache)) return false;
        ChunkMap.TrackedEntity tracked = chunkCache.chunkMap.entityMap.get(holder.getSource().getId());
        if(tracked == null) return false;
        return tracked.seenBy.contains(player.connection);
    }

    @Override
    public GridUUID indexedCopy(int index) {
        return new EntityUUID(this.uuid, index);
    }

    @Override
    public @Nullable AnchorPoint getAnchor(ClientLevel world) {
        return getAnchorPoints(world) == null ? null : getAnchorPoints(world).getAnchor(index);
    }

    @Override
    public @Nullable AnchorPointable<? extends Entity> getAnchorPoints(LevelReader world) {
        if(holder == null) {
            Entity e = world.isClientSide() 
                ? ((ClientLevel)world).entityStorage.getEntityGetter().get(uuid) 
                : ((ServerLevel)world).getEntity(uuid);
            holder = GriddableEntityAttachment.of(e, true);
        }
        return holder;
    }

    @Override
    public @Nullable DispatchedAnchorNode getSurrogate(LevelReader world) {
        return getAnchorPoints(world) == null ? null : getAnchorPoints(world).getSurrogate();
    }

    @Override
    public @Nullable IAttachmentHolder getDataHolder(LevelReader world) {
        return getAnchorPoints(world) == null ? null : holder.getSource();
    }

    @Override
    public String describeDataHolder(LevelReader world) {
        if(getDataHolder(world) instanceof Entity e) {
            Vec3 pos = e.getPosition(1);
            return e.getClass().getSimpleName() + "['" +  e.getName().getString() + ",' " + pos.x + ", " + pos.y + ", " + pos.z + "]";
        }
        return "not_applicable";
    }

    @Override
    public boolean canMoveDynamically() {
        return true;
    }

    @Override
    public float getAttachedSizeFactor(LevelReader world) {
        return getAnchorPoints(world) == null ? 0 : (float)holder.getSource().getBoundingBox().getSize();
    }

    @Override
    public boolean isAttachedToPlayer(LevelReader world) {
        return getAnchorPoints(world) == null ? false : holder.getSource() instanceof Player;
    }

    @Override
    public void applyForceToAttachment(LevelReader world, Vec3 force, boolean retainVelocity) {
        if(getAnchorPoints(world) == null) return;
        holder.getSource().setDeltaMovement(retainVelocity ? holder.getSource().getDeltaMovement().add(force) : force);
    }

    @Override
    public Vec3 getAttachmentVelocity(LevelReader world) {
        if(getAnchorPoints(world) == null) return Vec3.ZERO;
        return holder.getSource().getDeltaMovement();
    }

    @Override
    public void setAttachmentVelocity(LevelReader world, Vec3 vec) {
        if(getAnchorPoints(world) == null) return;
        holder.getSource().setDeltaMovement(vec);
    }

    @Override
    public UUIDDiscriminator getDiscriminatorType() {
        return UUIDDiscriminator.ENTITY;
    }

    @Override
    public BlockPos getBlockPos(LevelReader world) {
        return VectorHelper.toBlockPos(getPos(world));
    }

    @Override
    public Vec3 getPos(LevelReader world) {
        return getPos(world, 1);
    }

    @Override
    public Vec3 getPos(LevelReader world, float pTicks) {
        return getAnchorPoints(world) == null ? Vec3.ZERO : holder.getSource().getPosition(pTicks);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float ox, float oy, float oz) {
        return getOffsetPos(world, 1, ox, oy, oz);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz) {
        return getAnchorPoints(world) == null ? Vec3.ZERO : holder.getSource().getRopeHoldPosition(pTicks);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void writeTo(CompoundTag tag) {
        tag.putUUID("uid", uuid);
        tag.putByte("i", (byte)index);
    }

    @Override
    public void writeTo(ByteBuf buffer) {
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
        buffer.writeByte((byte)index);
    }

    @Override
    public void writeTo(RecordBuilder<?> builder) {
        builder.add("uida", uuid.getMostSignificantBits(), Codec.LONG);
        builder.add("uidb", uuid.getLeastSignificantBits(), Codec.LONG);
        builder.add("i", (byte)index, Codec.BYTE);
    }

    @Override
    public String toString() {
        return "EntityUUID[" + uuid + ", " + index + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof EntityUUID that)) return false;
        return this.uuid.equals(that.uuid) && this.index == that.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDiscriminatorType(), uuid, index);
    }
}
