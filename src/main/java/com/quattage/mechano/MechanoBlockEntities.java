package com.quattage.mechano;

import com.quattage.mechano.api.blockEntity.renderer.GriddableBlockEntityRenderer;
import com.quattage.mechano.content.connector.SingleConnectorBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.neoforged.bus.api.IEventBus;

public class MechanoBlockEntities {

    public static final BlockEntityEntry<SingleConnectorBlockEntity> CONNECTOR_SINGLE = 
        Mechano.REGISTRATE.blockEntity("connector_single", SingleConnectorBlockEntity::new)
            .validBlocks(MechanoBlocks.CONNECTOR_SINGLE)
            .renderer(() -> GriddableBlockEntityRenderer::new)
            .register();

    public static void register(IEventBus modBus) {
        Mechano.LOGGER.debug("registering block entities");
    }
}
