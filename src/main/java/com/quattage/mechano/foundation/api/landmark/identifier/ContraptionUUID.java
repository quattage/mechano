

package com.quattage.mechano.foundation.api.landmark.identifier;

import java.util.Objects;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorage.DataScope;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.entity.GriddableContraptionAttachment;
import com.quattage.mechano.foundation.api.switchboard.TrackedConstruct;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class ContraptionUUID extends GridUUID {

    private final UUID uuid;
    private final BlockPos structurePos;
    private final int index;
    private @Nullable AbstractContraptionEntity cachedACE;

    /**
     * A mirror implementation of {@link AbstractContraptionEntity#toGlobalVector} that
     * lerps the position of the body aas well as the rotation offset. I don't know why the 
     * built in implementation doesn't do this already, but doing this was required
     * @param ace The contraption entity whose anchor position will be used when lerping
     * @param localVec The local offset relatiev to the anchor which will be transformed and returned
     * @param pTicks partial ticks, accessible from most rendering contexts, used for lerping
     * @return A new Vec3 containing the worldly position derived from <code>localVec</code>
     */
    public static Vec3 toGlobalVectorWithPositionalLerping(AbstractContraptionEntity ace, Vec3 localVec, float pTicks) {
		Vec3 anchor = ace.getPrevAnchorVec().lerp(ace.getAnchorVec(), pTicks);
		Vec3 rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
		localVec = ace.applyRotation(localVec.subtract(rotationOffset), pTicks);
		localVec = localVec.add(rotationOffset).add(anchor);
		return localVec;
	}

    public ContraptionUUID(UUID uuid, BlockPos structurePos, int index) {
        this.uuid = uuid;
        this.structurePos = structurePos;
        this.index = index;
    }

    public ContraptionUUID(GriddableContraptionAttachment points, BlockPos structurePos, int index) {
        this.uuid = points.getSource().getUUID();
        this.structurePos = structurePos;
        this.index = index;
        this.cachedACE = (AbstractContraptionEntity)points.getSource();
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
        IAttachmentHolder holder = getDataStorageHolder(player.level());
        if(!(holder instanceof AbstractContraptionEntity ace)) return false;
        ChunkMap.TrackedEntity tracked = chunkCache.chunkMap.entityMap.get(ace.getId());
        if(tracked == null) return false;
        return tracked.seenBy.contains(player.connection);
    }

    @Override
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(!(holder instanceof AbstractContraptionEntity ace)) return false;
        Contraption c = ace.getContraption();
        if(c == null) return false;
        return view.isVisible(c.bounds);
    }

    @Override
    public @Nullable Griddable<?> getOrFindGriddable(LevelReader world) {
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(!(holder instanceof AbstractContraptionEntity ace)) return null;
        if(world.isClientSide()) {
            Contraption c = ace.getContraption();
            if(c == null) return null;
            if(c.presentBlockEntities == null) return null;
            BlockEntity be = c.presentBlockEntities.get(structurePos);
            return be instanceof GriddableBlockEntity gbe ? gbe : null;
        }
        return ace.getData(MechanoData.ANCHOR_ATTACHMENT);
    }

    @Override
    public @Nullable SurrogateNode getSurrogate(LevelReader world) {
        Griddable<?> points = getOrFindGriddable(world);
        return points == null ? null : points.getSurrogate();
    }

    @Override
    public IAttachmentHolder getDataStorageHolder(LevelReader world) {
        if(cachedACE != null) return cachedACE;
        if(world.isClientSide()) {
            Entity e = ((ClientLevel)world).entityStorage.getEntityGetter().get(uuid);
            if(!(e instanceof AbstractContraptionEntity ace)) return null;
            this.cachedACE = ace;
            return ace;
        }
        Entity e = ((ServerLevel)world).getEntity(uuid);
        if(!(e instanceof AbstractContraptionEntity ace)) return null;
        this.cachedACE = ace;
        return ace;
    }

    @Override
    public @Nullable AnchorPoint getAnchor(ClientLevel world) {
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(!(holder instanceof AbstractContraptionEntity ace)) return null;
        Contraption c = ace.getContraption();
        if(c == null || c.presentBlockEntities == null || c.presentBlockEntities.isEmpty()) 
            return null;
        BlockEntity be = c.presentBlockEntities.get(structurePos);
        if(!(be instanceof GriddableBlockEntity gbe)) return null;
        return gbe.getAnchor();
    }

    @Override
    public String describeDataScope(LevelReader world) {
        Entity e = (Entity)getDataStorageHolder(world);
        if(e == null) return "not_applicable";
        BlockPos pos = VectorHelper.toBlockPos(e.getPosition(1));
        return "'" + e.getClass().getSimpleName() + "' at [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]";
    }

    @Override
    public boolean canMoveDynamically(LevelReader world) {
        return true;
    }

    @Override
    public boolean canReceiveVelocity(LevelReader world) {
        IAttachmentHolder holder = getDataStorageHolder(world);
        return holder instanceof AbstractContraptionEntity ace && ace.getVehicle() instanceof AbstractMinecart;
    }

    @Override
    public float getMass(LevelReader world) {
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(!(holder instanceof AbstractContraptionEntity ace)) return TrackedConstruct.DEFAULT_MASS;
        return ace.getVehicle() instanceof AbstractMinecart ? (float)ace.getBoundingBox().getSize() : TrackedConstruct.DEFAULT_MASS;
    }

    @Override
    public Vec3 getAttachmentVelocity(LevelReader world) {
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(!(holder instanceof AbstractContraptionEntity ace)) return Vec3.ZERO;
        return ace.getVehicle() instanceof AbstractMinecart am ? am.getDeltaMovement() : ace.getDeltaMovement();
    }

    @Override public void applyForceToAttachment(LevelReader world, Vector3f force, boolean retainVelocity) { 
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(!(holder instanceof AbstractContraptionEntity ace)) return;
        if(!(ace.getVehicle() instanceof AbstractMinecart am)) return;
        if(retainVelocity) am.push(force.x, force.y, force.z);
        else am.setDeltaMovement(force.x, force.y, force.z);
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
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(!(holder instanceof AbstractContraptionEntity ace)) return null;
        return toGlobalVectorWithPositionalLerping(ace, Vec3.atLowerCornerOf(structurePos), pTicks);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz) {
        Vec3 pos = getPos(world, pTicks);
        IAttachmentHolder holder = getDataStorageHolder(world);
        if(holder instanceof AbstractContraptionEntity ace)
            pos.add(ace.applyRotation(new Vec3(ox, oy, oz), pTicks));
        if(pos == null) return null;
        return pos.add(ox, oy, oz);
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

    public UUID getUUID() {
        return uuid;
    }

    public BlockPos getContraptionOffset() {
        return structurePos;
    }

    public VoxelUUID toVoxel(StructureTransform transform) {
        return new VoxelUUID(transform.apply(structurePos), getIndex());
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
