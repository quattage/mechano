package com.quattage.experimental_tables.content.block.entity.renderer;

import com.quattage.experimental_tables.content.block.entity.InductorBlockEntity;
import com.quattage.experimental_tables.content.block.entity.model.InductorBlockModel;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import software.bernie.geckolib3.renderers.geo.GeoBlockRenderer;

public class InductorRenderer extends GeoBlockRenderer<InductorBlockEntity> {

	public InductorRenderer(BlockEntityRendererFactory.Context rendererDispatcherIn) {
		super(new InductorBlockModel());
	}

	@Override
	public RenderLayer getRenderType(InductorBlockEntity animatable, float partialTick, MatrixStack poseStack,
			VertexConsumerProvider bufferSource, VertexConsumer buffer, int packedLight, Identifier texture) {
		return RenderLayer.getEntityTranslucent(getTextureLocation(animatable));
	}
}