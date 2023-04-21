package com.quattage.experimental_tables.content.block.entity.renderer;

import com.mrh0.createaddition.rendering.WireNodeRenderer;
import com.quattage.experimental_tables.content.block.entity.HConnectorTileEntity;

import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;


public class HConnectorRenderer extends WireNodeRenderer<HConnectorTileEntity> {

	public HConnectorRenderer(BlockEntityRendererFactory.Context context) {
		super(context);
	}
}