package com.quattage.mechano;

import static com.quattage.mechano.Mechano.REGISTRATE;
import static com.simibubi.create.api.behaviour.movement.MovementBehaviour.movementBehaviour;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import com.quattage.mechano.content.connector.SingleConnectorBlock;
import com.quattage.mechano.content.test.TestAxisBlock;
import com.quattage.mechano.foundation.gridapi.blockEntity.GriddableBlockEntity.GriddableMovementBehaviour;
import com.quattage.mechano.infrastructure.datagen.DynamicStateGenerator;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonnullType;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.bus.api.IEventBus;



public class MechanoBlocks {

    static {
        REGISTRATE.setCreativeTab(MechanoGroups.BASE);
    }

    public static final BlockEntry<TestAxisBlock> TEST_AXIS = 
        REGISTRATE.block("test_axis", TestAxisBlock::new)
            .initialProperties(SharedProperties::netheriteMetal)
            .properties(@NonnullType Properties::noOcclusion)
            .transform(pickaxeOnly())
            .blockstate(new DynamicStateGenerator()::generate)
            .item()
            .transform(customItemModel("test_axis", "cube"))
            .register();

    public static final BlockEntry<SingleConnectorBlock> CONNECTOR_SINGLE = 
        REGISTRATE.block("connector_single", SingleConnectorBlock::new)
            .initialProperties(() -> Blocks.IRON_BARS)
            .properties(@NonnullType Properties::noOcclusion)
            .transform(pickaxeOnly())
            .blockstate(new DynamicStateGenerator()::generate)
            .item()
            .transform(customItemModel("connector_single", "base"))
            .onRegister(movementBehaviour(new GriddableMovementBehaviour()))
            .register();

    public static void register(IEventBus modBus) {
        Mechano.LOGGER.debug("registering blocks");
    }
}
