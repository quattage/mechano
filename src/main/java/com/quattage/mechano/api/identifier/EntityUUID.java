package com.quattage.mechano.api.identifier;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.api.LinkDataStorage.DataScope;
import com.quattage.mechano.api.anchor.AnchorPoint;
import com.quattage.mechano.api.entity.GriddableEntityAttachment;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.griddable.SurrogateNode;
import com.quattage.mechano.api.switchboard.TrackedConstruct;
import com.quattage.mechano.foundation.math.VectorOperations;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class EntityUUID extends GridUUID {

    private final UUID uuid;
    private final int index;
    private @Nullable Griddable<? extends Entity> points;

    public EntityUUID(Entity e) {
        this(e.getUUID(), 0);
    }

    public EntityUUID(UUID uuid, int index) {
        this.uuid = uuid;
        this.index = clampIndex(index);
    }

    public EntityUUID(CompoundTag tag) {
        this.uuid = tag.getUUID("uid");
        this.index = clampIndex(tag.getByte("i"));
    }

    public EntityUUID(ByteBuf buffer) {
        this.uuid = new UUID(buffer.readLong(), buffer.readLong());
        this.index = clampIndex(buffer.readByte());
    }

    public EntityUUID(Dynamic<?> dyn) {
        this.uuid = new UUID(dyn.get("uida").asLong(0), dyn.get("uidb").asLong(0));
        this.index = clampIndex(dyn.get("i").asInt(0));
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        getDataStorageHolder(player.level());
        if(!(player.level().getChunkSource() instanceof ServerChunkCache chunkCache)) return false;
        if(points.getSource() instanceof ServerPlayer otherPlayer)
            if(otherPlayer.getId() == player.getId()) return true;
        ChunkMap.TrackedEntity tracked = chunkCache.chunkMap.entityMap.get(points.getSource().getId());
        if(tracked == null) return false;
        return tracked.seenBy.contains(player.connection);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        return getDataStorageHolder(world) == null ? false : view.isVisible(points.getSource().getBoundingBox());
    }

    @Override
    public GridUUID indexedCopy(int index) {
        return new EntityUUID(this.uuid, index);
    }

    @Override
    public @Nullable AnchorPoint getAnchor(ClientLevel world) {
        return getOrFindGriddable(world) == null ? null : getOrFindGriddable(world).getAnchors().get(index);
    }

    @Override
    public @Nullable Griddable<? extends Entity> getOrFindGriddable(LevelReader world) {
        if(points != null) return points;
        Entity e = world.isClientSide() 
            ? ((ClientLevel)world).entityStorage.getEntityGetter().get(uuid) 
            : ((ServerLevel)world).getEntity(uuid);
        points = GriddableEntityAttachment.of(e, true);
        return points;
    }

    @Override
    public @Nullable SurrogateNode getSurrogate(LevelReader world) {
        return getOrFindGriddable(world) == null ? null : getOrFindGriddable(world).getSurrogate();
    }

    @Override
    public @Nullable IAttachmentHolder getDataStorageHolder(LevelReader world) {
        return getOrFindGriddable(world) == null ? null : points.getSource();
    }

    @Override
    public String describeDataScope(LevelReader world) {
        if(getDataStorageHolder(world) instanceof Entity e) {
            Vec3 pos = e.getPosition(1);
            return e.getClass().getSimpleName() + "['" +  e.getName().getString() + ",' " + (int)pos.x + ", " + (int)pos.y + ", " + (int)pos.z + "]";
        }
        return "not_applicable";
    }

    @Override
    public void sendLevelUpdates(Level world) {
        return;
    }

    @Override
    public boolean canMoveDynamically(LevelReader world) {
        return true;
    }

    @Override
    public boolean canReceiveVelocity(LevelReader world) {
        return true;
    }

    @Override
    public float getMass(LevelReader world) {
        return getOrFindGriddable(world) == null ? TrackedConstruct.DEFAULT_MASS : (float)points.getSource().getBoundingBox().getSize();
    }

    @Override
    public boolean isAttachedToPlayer(LevelReader world) {
        return getOrFindGriddable(world) == null ? false : points.getSource() instanceof Player;
    }

    @Override
    public void applyForceToAttachment(LevelReader world, Vector3f force, boolean retainVelocity) {
        if(getOrFindGriddable(world) == null) return;
        Entity e = points.getSource();
        if(e == null || !e.isAlive()) return;
        if(!world.isClientSide() && e instanceof Player) return;
        if(retainVelocity) e.push(force.x, force.y, force.z);
        else e.setDeltaMovement(force.x, force.y, force.z);
    }

    @Override
    public Vec3 getAttachmentVelocity(LevelReader world) {
        if(getOrFindGriddable(world) == null) return Vec3.ZERO;
        return points.getSource().getDeltaMovement();
    }

    @Override
    public UUIDDiscriminator getDiscriminatorType() {
        return UUIDDiscriminator.ENTITY;
    }

    @Override
    public BlockPos getBlockPos(LevelReader world) {
        return VectorOperations.toBlockPos(getPos(world));
    }

    @Override
    public Vec3 getPos(LevelReader world, float pTicks) {
        return getOrFindGriddable(world) == null ? Vec3.ZERO : points.getSource().getPosition(pTicks);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz) {
        return getOrFindGriddable(world) == null ? Vec3.ZERO : points.getSource().getRopeHoldPosition(pTicks);
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
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof EntityUUID that)) return false;
        return this.uuid.equals(that.uuid) && this.index == that.index;
    }

    @Override
    public boolean isUnindexed(GridUUID other) {
        if(this == other) return true;
        if(!(other instanceof EntityUUID that)) return false;
        return this.uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDiscriminatorType(), uuid, index);
    }

    @Override
    public String toString() {
        return "EntityUUID[" + uuid + ", " + index + "]";
    }

    @Override
    public DataScope getDataScope(LevelReader world) {
        return DataScope.MOVING_ENTITY;
    }

    @Override
    public void setDataScope(DataScope scope) {
        return;
    }
}
