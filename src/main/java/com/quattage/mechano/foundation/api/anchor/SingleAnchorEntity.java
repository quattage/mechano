package com.quattage.mechano.foundation.api.anchor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.landmark.classifier.EntityUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SingleAnchorEntity extends Entity implements AnchorPointable<SingleAnchorEntity> {

    private AnchorArray anchor;
    private final DispatchedAnchorNode surrogate = new DispatchedAnchorNode(this);

    public SingleAnchorEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData(Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {}

    @Override
    public void constructAnchors(AnchorArray.Builder anchors) {
        anchor = AnchorArray.ofSingle((new AnchorPoint(new EntityUUID(getUUID(), 0), 0, 0, 0, 1.7f, true, 2)));
    }

    @Override
    public AnchorArray getAnchors() {
        return anchor;
    }

    @Override
    public Level getWorld() {
        return level();
    }

    @Override
    public DispatchedAnchorNode getSurrogate() {
        return surrogate;
    }

    @Override
    public GridUUID createAddress() {
        return anchor.getByIndex(0).getAddress();
    }

    @Override
    public String describeState() {
        return "SingleAnchor[" + getPosition(1) + ", (" + getUUID() + ")]";
    }

    @Override
    public SingleAnchorEntity getSource() {
        return this;
    }

    public static class SingleAnchorEntityRenderer extends EntityRenderer<SingleAnchorEntity> {

        public SingleAnchorEntityRenderer(Context context) {
            super(context);
        }

        @Override
        public boolean shouldRender(SingleAnchorEntity livingEntity, Frustum camera, double camX, double camY, double camZ) { return false; }

        @Override
        public void render(SingleAnchorEntity p_entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {}

        @Override
        public ResourceLocation getTextureLocation(SingleAnchorEntity entity) {
            return null;
        }

    }
}
