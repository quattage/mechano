package com.quattage.mechano;

import com.quattage.mechano.content.test.TestAxisBlockEntity;
import com.quattage.mechano.foundation.SimpleBlockEntityRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.neoforged.bus.api.IEventBus;

public class MechanoBlockEntities {

    public static final BlockEntityEntry<TestAxisBlockEntity> TEST_AXIS = Mechano.REGISTRATE
		.blockEntity("test_axis", TestAxisBlockEntity::new)
		.validBlocks(MechanoBlocks.TEST_AXIS)
		.renderer(() -> SimpleBlockEntityRenderer::new)
		.register();

    public static void register(IEventBus modBus) {
        Mechano.LOGGER.debug("registering block entities");
    }
}
