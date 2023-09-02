package com.quattage.mechano.content.block.power.transfer.connector.transmission;

import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier1BlockEntity;
import com.quattage.mechano.foundation.electricity.rendering.ElectricBlockRenderer;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

public class TransmissionConnectorRenderer extends ElectricBlockRenderer<TransmissionConnectorBlockEntity> {

    public TransmissionConnectorRenderer(Context context) {
        super(context);
    }
    
}
