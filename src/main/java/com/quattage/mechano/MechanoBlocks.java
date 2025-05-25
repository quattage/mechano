package com.quattage.mechano;

import static com.quattage.mechano.Mechano.REGISTRATE;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import com.quattage.mechano.content.connector.SingleConnectorBlock;
import com.quattage.mechano.content.test.TestAxisBlock;
import com.quattage.mechano.foundation.block.orientation.DynamicStateGenerator;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;


public class MechanoBlocks {

    static {
        REGISTRATE.setCreativeTab(MechanoGroups.BASE);
    }

    public static final BlockEntry<TestAxisBlock> TEST_AXIS = 
        REGISTRATE.block("test_axis", TestAxisBlock::new)
            .initialProperties(SharedProperties::netheriteMetal)
            .properties(p -> p.noOcclusion())
            .transform(pickaxeOnly())
            .blockstate(new DynamicStateGenerator()::generate)
            .item()
            .transform(customItemModel("test_axis", "cube"))
            .register();

    public static final BlockEntry<SingleConnectorBlock> CONNECTOR_SINGLE = 
        REGISTRATE.block("connector_single", SingleConnectorBlock::new)
            .initialProperties(() -> { return Blocks.IRON_BARS; })
            .properties(p -> p.noOcclusion())
            .transform(pickaxeOnly())
            .blockstate(new DynamicStateGenerator()::generate)
            .item()
            .transform(customItemModel("connector_single", "base"))
            .register();

    public static void register(IEventBus modBus) {
        Mechano.LOGGER.debug("registering blocks");
    }
}
