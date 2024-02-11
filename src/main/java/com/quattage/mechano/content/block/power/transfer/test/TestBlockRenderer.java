package com.quattage.mechano.content.block.power.transfer.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.foundation.electricity.rendering.WireAnchorBlockRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlockRenderer extends WireAnchorBlockRenderer<TestBlockEntity> {

    public TestBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }
}
