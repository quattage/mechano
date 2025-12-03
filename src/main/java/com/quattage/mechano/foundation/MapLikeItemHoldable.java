package com.quattage.mechano.foundation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.quattage.mechano.foundation.mixin.client.ItemInHandRendererInvoker;
import com.quattage.mechano.foundation.mixin.client.ItemInHandRendererMixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface MapLikeItemHoldable {

    /**
     * @param stack
     * @return <code>true</code> if this item should be rendered like a map in the player's hands
     */
    @OnlyIn(Dist.CLIENT)
    boolean shouldRenderSpecial(ItemStack stack);
    
    /**
     * Overrides the vanilla {@link ItemInHandRenderer} behaviour as invoked by the
     * {@link ItemInHandRendererMixin mixin.} You may implement your own logic here 
     * for determining how the player should hold this item in first person, or 
     * you can simply return <code>false</code> here to do nothing and use the 
     * default pose.
     * @return <code>true</code> if traditional hand rendering should
     * be cancelled in favor of a custom implementation defined within
     * the scope of this method.
     */
    @OnlyIn(Dist.CLIENT)
    static boolean renderInHands(ItemStack item, MultiBufferSource bufferSource, PoseStack matrixStack, AbstractClientPlayer player, ItemInHandRenderer renderer, float swingProgress, float equipProgress, float pitch, float pTicks, int packedLight) {

        float tilt = (((ItemInHandRendererInvoker)renderer).mechano$calculateMapTilt(pitch) * 0.4f) + 0.3f;
        matrixStack.translate(0f, 0.2f + equipProgress * -1.2f + tilt * -0.3f, -0.72f);
        matrixStack.mulPose(Axis.XP.rotationDegrees(tilt * -90f));

        if (!player.isInvisible()) {
            matrixStack.pushPose();
            matrixStack.mulPose(Axis.YP.rotationDegrees(90));
            MapLikeItemHoldable.renderSpoolHand(renderer, player, matrixStack, bufferSource, packedLight, HumanoidArm.RIGHT);
            MapLikeItemHoldable.renderSpoolHand(renderer, player, matrixStack, bufferSource, packedLight, HumanoidArm.LEFT);
            matrixStack.popPose();
        }

        matrixStack.pushPose();
        matrixStack.mulPose(Axis.YP.rotationDegrees(90).mul(Axis.XN.rotationDegrees(290)));
        matrixStack.translate(0.2f, -0.3f, 0.17);
        renderer.renderItem(player, item, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, matrixStack, bufferSource, packedLight);
        matrixStack.popPose();

        return true;
    }

    @OnlyIn(Dist.CLIENT)
    static void renderSpoolHand(ItemInHandRenderer renderer, AbstractClientPlayer player, PoseStack poseStack, MultiBufferSource buffer, int packedLight, HumanoidArm side) {
        PlayerRenderer playerrenderer = (PlayerRenderer)((ItemInHandRendererInvoker)renderer)
            .mechano$getEntityRenderDispatcher().<AbstractClientPlayer>getRenderer(player);
        poseStack.pushPose();
        // yoinked from vanilla but with some fudged numbers to make 
        // the hands look more like what i'm going for here
        float f = side == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(92.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(f * -30.0F));
        poseStack.translate(f * 0.09F, -0.9F, 0.45F);
        if (side == HumanoidArm.RIGHT) 
            playerrenderer.renderRightHand(poseStack, buffer, packedLight, player);
        else playerrenderer.renderLeftHand(poseStack, buffer, packedLight, player);

        poseStack.popPose();
    }
}
