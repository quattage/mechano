package com.quattage.mechano;

import com.quattage.mechano.content.connector.SingleConnectorBlock;
import com.quattage.mechano.infrastructure.datagen.DynamicStateGenerator;
import com.simibubi.create.foundation.data.ModelGen;
import com.simibubi.create.foundation.data.TagGen;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.nullness.NonnullType;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.neoforged.bus.api.IEventBus;



public class MechanoBlocks {

    static {
        Mechano.REGISTRATE.setCreativeTab(MechanoGroups.BASE);
    }

    public static final BlockEntry<SingleConnectorBlock> CONNECTOR_SINGLE = 
        Mechano.REGISTRATE.block("connector_single", SingleConnectorBlock::new)
            .initialProperties(() -> Blocks.IRON_BARS)
            .properties(@NonnullType Properties::noOcclusion)
            .transform(TagGen.pickaxeOnly())
            .blockstate(new DynamicStateGenerator()::generate)
            .item()
            .transform(ModelGen.customItemModel("connector_single", "base"))
            .register();

    public static void register(IEventBus modBus) {
        Mechano.LOGGER.debug("registering blocks");
    }
}
