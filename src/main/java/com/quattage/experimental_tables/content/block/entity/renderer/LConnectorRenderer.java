package com.quattage.experimental_tables.content.block.entity.renderer;

import com.mrh0.createaddition.rendering.WireNodeRenderer;
import com.quattage.experimental_tables.content.block.entity.LConnectorBlockEntity;

import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;


public class LConnectorRenderer extends WireNodeRenderer<LConnectorBlockEntity> {

	public LConnectorRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}
}