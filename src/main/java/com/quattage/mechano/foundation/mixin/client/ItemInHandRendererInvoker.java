package com.quattage.mechano.foundation.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.HumanoidArm;

@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererInvoker {
    
    @Invoker("renderMapHand")
    public abstract void mechano$renderMapHand(PoseStack poseStac, MultiBufferSource buffer, int packedLight, HumanoidArm side);

    @Invoker("calculateMapTilt")
    public abstract float mechano$calculateMapTilt(float pitch);
}
