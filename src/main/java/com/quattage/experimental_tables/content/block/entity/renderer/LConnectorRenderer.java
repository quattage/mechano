package com.quattage.experimental_tables.content.block.entity.renderer;

import com.mrh0.createaddition.rendering.WireNodeRenderer;
import com.quattage.experimental_tables.content.block.entity.LConnectorTileEntity;

import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;


public class LConnectorRenderer extends WireNodeRenderer<LConnectorTileEntity> {

	public LConnectorRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}
}