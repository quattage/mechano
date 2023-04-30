package com.quattage.mechano.content.block.Inductor;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class InductorBlockRenderer extends GeoBlockRenderer<InductorBlockEntity> {

	public InductorBlockRenderer(BlockEntityRendererFactory.Context rendererDispatcherIn) {
		super(new InductorBlockModel());
	}

	@Override
	public RenderLayer getRenderType(InductorBlockEntity animatable, float partialTick, MatrixStack poseStack,
			VertexConsumerProvider bufferSource, VertexConsumer buffer, int packedLight, Identifier texture) {
		return RenderLayer.getEntityTranslucent(getTextureLocation(animatable));
	}
}