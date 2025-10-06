package com.quattage.mechano.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.item.MapLikeItemHoldable;
import com.quattage.mechano.foundation.item.SpoolItem;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Inject(method = "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = {@At(value = "HEAD")}, cancellable = true)
    public void mechano$specialSpoolFirstPersonRendering(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equippedProgress, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, CallbackInfo info) {
        // TODO registry for this
        if(!(stack.getItem() instanceof SpoolItem schpool)) return;
        if(!stack.has(UUIDDiscriminator.ATTACHMENT)) return;
        if(MapLikeItemHoldable.renderInHands(stack, buffer, poseStack, player, (ItemInHandRenderer)(Object)this, swingProgress, equippedProgress, pitch, partialTicks, combinedLight));
            info.cancel();
    }
}
