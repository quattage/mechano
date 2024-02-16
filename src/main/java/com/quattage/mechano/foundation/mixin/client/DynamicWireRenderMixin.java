package com.quattage.mechano.foundation.mixin.client;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.rendering.WireModelRenderer;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.quattage.mechano.foundation.electricity.system.SVID;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

// Renders unconfirmed wires as part of the third-person player model.
// Usually seen by other players, or you in third person.
// Uses a simplified, non-contextual rendering pipeline for optimization purposes.
@Mixin(PlayerRenderer.class)
public class DynamicWireRenderMixin {

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = {@At(value = "TAIL")}, cancellable = true)
    public void renderWireThirdPerson(AbstractClientPlayer player, float yaw, float pTicks, PoseStack matrixStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo info) { 
        ItemStack spool = WireSpool.getHeldSpool(player);
        if(spool == null) return;
        CompoundTag spoolTag = spool.getOrCreateTag();
        if(!SVID.isValidTag(spoolTag)) return;

        ClientLevel world = player.clientLevel;
        SVID connectID = SVID.of(spoolTag);

        Vec3 wireFrom = AnchorPoint.getAnchorAt(world, connectID).getFirst().getPos();
        Vec3 wireTo = player.getPosition(pTicks);
        Vec3 leashOffset = player.getRopeHoldPosition(pTicks);
;        
        renderWire(world, leashOffset, wireFrom, wireTo, (WireSpool)spool.getItem(), pTicks, matrixStack, bufferSource);
    }

    private void renderWire(ClientLevel world, Vec3 entityHoldPos, Vec3 fromPos, Vec3 toPos, WireSpool type, float pTicks, PoseStack matrixStack, MultiBufferSource bufferSource) {

        matrixStack.pushPose();
        matrixStack.translate(fromPos.x - toPos.x, fromPos.y - toPos.y, fromPos.z - toPos.z);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(type.asResource()));

        Vector3f offset = WireModelRenderer.getWireOffset(fromPos, toPos);
        int[] lightmap = WireModelRenderer.deriveLightmap(world, fromPos, toPos);
        matrixStack.translate(offset.x(), 0, offset.z());
        
        Vec3 entityLeashOffset = entityHoldPos.subtract(toPos);

        Vec3 startPos = fromPos.add(offset.x(), 0, offset.z());
        Vec3 endPos = toPos.add(entityLeashOffset).add(-offset.x(), 0, -offset.z());
        Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

        float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
        matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

        WireModelRenderer.INSTANCE.renderDynamic(buffer, matrixStack, wireOrigin, lightmap[0], lightmap[1], lightmap[2], lightmap[3]);
        matrixStack.popPose();
    }
}
