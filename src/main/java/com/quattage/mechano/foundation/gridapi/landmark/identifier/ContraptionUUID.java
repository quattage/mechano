package com.quattage.mechano.foundation.gridapi.landmark.identifier;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.gridapi.Griddable;
import com.quattage.mechano.foundation.gridapi.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.anchor.SurrogateNode;
import com.quattage.mechano.foundation.gridapi.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.gridapi.entity.GriddableContraptionAttachment;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class ContraptionUUID extends GridUUID {

    private UUID uuid;
    private BlockPos structurePos;
    private int index;
    private @Nullable Griddable<?> points;
    private @Nullable Contraption cachedContraption;

    public ContraptionUUID(UUID uuid, BlockPos structurePos, int index) {
        this.uuid = uuid;
        this.structurePos = structurePos;
        this.index = index;
    }

    public ContraptionUUID(GriddableContraptionAttachment points, BlockPos structurePos, int index) {
        this.uuid = points.getSource().getUUID();
        this.structurePos = structurePos;
        this.index = index;
        forceHost(points);
    }

    public ContraptionUUID(CompoundTag tag) {
        this.uuid = tag.getUUID("uid"); 
        this.structurePos = BlockPos.of(tag.getLong("sp"));
        this.index = clampIndex(tag.getByte("i"));
    }

    public ContraptionUUID(ByteBuf buffer) {
        this.uuid = new UUID(buffer.readLong(), buffer.readLong());
        this.structurePos = BlockPos.of(buffer.readLong());
        this.index = clampIndex(buffer.readByte());
    }

    public ContraptionUUID(Dynamic<?> dyn) {
        this.uuid = new UUID(dyn.get("uida").asLong(0), dyn.get("uidb").asLong(0));
        this.structurePos = BlockPos.of(dyn.get("sp").asLong(0));
        this.index = clampIndex(dyn.get("i").asInt(0));
    }

    @Override
    public GridUUID indexedCopy(int index) {
        return new ContraptionUUID(this.uuid, this.structurePos, index);
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        if(!(player.level().getChunkSource() instanceof ServerChunkCache chunkCache)) return false;
        AbstractContraptionEntity ace = tryGetEntity(player.level());
        if(ace == null) return false;
        ChunkMap.TrackedEntity tracked = chunkCache.chunkMap.entityMap.get(ace.getId());
        if(tracked == null) return false;
        return tracked.seenBy.contains(player.connection);
    }

    @Override
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        if(getOrFindGriddable(world) == null || cachedContraption.bounds == null) 
            return false;
        return view.isVisible(cachedContraption.bounds);
    }

    public void forceHost(GriddableContraptionAttachment points) {
        Objects.requireNonNull(points);
        this.points = points;
        this.cachedContraption = points.getContraption();
    }

    @Override
    public @Nullable Griddable<?> getOrFindGriddable(LevelReader world) {
        if(points != null) return points;
        if(world.isClientSide()) points = searchContraptionAsClient(world);
        else points = searchContraptionAsServer(world);
        return points;        
    }

    public GriddableBlockEntity searchContraptionAsClient(LevelReader world) {
        Entity e = ((ClientLevel)world).entityStorage.getEntityGetter().get(uuid);
        if(!(e instanceof AbstractContraptionEntity ace)) return null;
        this.cachedContraption = ace.getContraption();
        if(cachedContraption == null) return null;
        BlockEntity be = cachedContraption.presentBlockEntities.get(structurePos);
        return be instanceof GriddableBlockEntity gbe ? gbe : null;
    }

    public GriddableContraptionAttachment searchContraptionAsServer(LevelReader world) {
        Entity e = ((ServerLevel)world).getEntity(uuid);
        if(!(e instanceof AbstractContraptionEntity ace)) return null;
        return (GriddableContraptionAttachment)ace.getExistingDataOrNull(MechanoData.ANCHOR_ATTACHMENT);
    }

    private @Nullable AbstractContraptionEntity tryGetEntity(LevelReader world) {
        if(cachedContraption != null && cachedContraption.entity != null) 
            return cachedContraption.entity;
        getOrFindGriddable(world);
        return cachedContraption == null ? null : cachedContraption.entity;
    }

    @Override
    public @Nullable SurrogateNode getSurrogate(LevelReader world) {
        return getOrFindGriddable(world) == null ? null : points.getSurrogate();
    }

    @Override
    public IAttachmentHolder getDataStorageHolder(LevelReader world) {
        return tryGetEntity(world);
    }

    @Override
    public @Nullable AnchorPoint getAnchor(ClientLevel world) {
        return getOrFindGriddable(world) == null ? null : getOrFindGriddable(world).getAnchor(index);
    }

    @Override
    public String describeDataScope(LevelReader world) {
        if(getDataStorageHolder(world) == null) return "not_applicable";
        return "Contraption [" + uuid + ", " + structurePos.getX() + ", " + structurePos.getY() + ", " + structurePos.getZ() + "]";
    }

    @Override
    public boolean canMoveDynamically(LevelReader world) {
        return true;
    }

    @Override
    public float getAttachedSizeFactor(LevelReader world) {
        AbstractContraptionEntity ace = tryGetEntity(world);
        if(ace == null) return 0;
        AABB box = cachedContraption.entity.getBoundingBox();
        return box == null ? 0 : (float)box.getSize();
    }

    @Override
    public Vec3 getAttachmentVelocity(LevelReader world) {
        AbstractContraptionEntity ace = tryGetEntity(world);
        return ace == null ? Vec3.ZERO : ace.getDeltaMovement();
    }

    // TODO base contraptions can't recieve velocity this way, but landlord voxel domains can
    @Override public void setAttachmentVelocity(LevelReader world, Vec3 vec) { return; }
    @Override public void applyForceToAttachment(LevelReader world, Vec3 force) { return; }
    // --

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
        AbstractContraptionEntity ace = tryGetEntity(world);
        if(ace == null) return Vec3.ZERO;
        return ace.toGlobalVector(Vec3.atLowerCornerOf(structurePos), pTicks);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz) {
        return getPos(world, pTicks).add(ox, oy, oz);
    }

    @Override
    public UUIDDiscriminator getDiscriminatorType() {
        return UUIDDiscriminator.CONTRAPTION;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public void writeTo(CompoundTag tag) {
        tag.putUUID("uid", uuid);
        tag.putLong("sp", structurePos.asLong());
        tag.putByte("i", (byte)index);
    }

    @Override
    public void writeTo(ByteBuf buffer) {
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
        buffer.writeLong(structurePos.asLong());
        buffer.writeByte((byte)index);
    }

    @Override
    public void writeTo(RecordBuilder<?> builder) {
        builder.add("uida", uuid.getMostSignificantBits(), Codec.LONG);
        builder.add("uidb", uuid.getLeastSignificantBits(), Codec.LONG);
        builder.add("sp", structurePos.asLong(), Codec.LONG);
        builder.add("i", (byte)index, Codec.BYTE);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) return true;
        if(!(obj instanceof ContraptionUUID that)) return false;
        return this.uuid.equals(that.uuid) && this.structurePos.equals(that.structurePos) && this.index == that.index;
    }

    @Override
    public boolean isUnindexed(GridUUID other) {
        if(this == other) return true;
        if(!(other instanceof ContraptionUUID that)) return false;
        return this.uuid.equals(that.uuid) && this.structurePos.equals(that.structurePos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDiscriminatorType(), uuid, structurePos, index);
    }

    @Override
    public DataScope getDataScope(LevelReader world) {
        return DataScope.MOVING_ENTITY;
    }

    @Override
    public void setDataScope(DataScope scope) {
        return;
    }

    @Override
    public void sendLevelUpdates(Level world) {
        return;
    }

    @Override
    public String toString() {
        return "ContraptionUUID[" + uuid + ", " + structurePos.getX() + ", " + structurePos.getY() + ", " + structurePos.getZ() + ", " + index + "]";
    }
}
