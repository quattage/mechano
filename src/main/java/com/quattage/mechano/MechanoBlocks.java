package com.quattage.mechano;

import com.quattage.mechano.content.block.integrated.toolStation.ToolStationBlock;
import com.quattage.mechano.content.block.integrated.toolStation.ToolStationGenerator;
import com.quattage.mechano.content.block.integrated.toolStation.UpgradeBlock;
import com.quattage.mechano.content.block.power.alternator.collector.CollectorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlock;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlock;
import com.quattage.mechano.content.block.power.transfer.adapter.CouplingNodeBlock;
import com.quattage.mechano.content.block.power.transfer.adapter.TransmissionNodeBlock;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.StackedConnectorGenerator;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.TransmissionConnectorBlock;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier0Block;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier1Block;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier2Block;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier3Block;
import com.quattage.mechano.content.block.power.transfer.test.TestBlock;
import com.quattage.mechano.content.block.power.transfer.voltometer.VoltometerBlock;
import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderBlock;
import com.quattage.mechano.foundation.block.orientation.DynamicStateGenerator;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.eventbus.api.IEventBus;

import static com.quattage.mechano.Mechano.REGISTRATE;
import static com.quattage.mechano.Mechano.defer;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;


//This is where blocks go to get registered
public class MechanoBlocks {
    static {
		REGISTRATE.useCreativeTab(MechanoGroups.MAIN_TAB);
	}

    public static final BlockEntry<UpgradeBlock> FORGE_UPGRADE = REGISTRATE.block("tool_forge", UpgradeBlock::new)
        .initialProperties(CommonProperties::dense)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(pickaxeOnly())
        .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.getEntry(), prov.models()
            .getExistingFile(defer(ctx, "tool_station", "forge")), 180))
        .simpleItem()
        .register();

    public static final BlockEntry<ToolStationBlock> TOOL_STATION = REGISTRATE.block("tool_station", ToolStationBlock::new)
        .initialProperties(CommonProperties::wooden)
        .properties(props -> props
            .sound(SoundType.WOOD)
            .noOcclusion()
        )
        .transform(pickaxeOnly())
        .blockstate(new ToolStationGenerator()::generate)
        .item()
        .transform(customItemModel("tool_station", "base"))
        .register();

    public static final BlockEntry<RotorBlock> ROTOR = REGISTRATE.block("rotor", RotorBlock::new)
        .initialProperties(CommonProperties::ductile)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(BlockStressDefaults.setImpact(48.0))
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(RotorBlock.MODEL_TYPE)::generate)
        .item()
        .transform(customItemModel("rotor", "single"))
        .register();

    public static final BlockEntry<CollectorBlock> COLLECTOR = REGISTRATE.block("collector", CollectorBlock::new)
        .initialProperties(CommonProperties::malleable)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(BlockStressDefaults.setImpact(48.0))
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(CollectorBlock.MODEL_TYPE)::generate)
        .item()
        .transform(customItemModel("collector", "base"))
        .register();

    public static final BlockEntry<StatorBlock> STATOR = REGISTRATE.block("stator", StatorBlock::new)
        .initialProperties(CommonProperties::dense)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(StatorBlock.MODEL_TYPE)::generate)
        .item()
        .transform(customItemModel("stator", "base"))
        .register();

    public static final BlockEntry<CouplingNodeBlock> COUPLING_NODE = REGISTRATE.block("coupling_node", CouplingNodeBlock::new)
        .initialProperties(CommonProperties::malleable)
        .properties(props -> props
        .sound(SoundType.NETHERITE_BLOCK)
        .noOcclusion()
        )
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(CouplingNodeBlock.MODEL_TYPE)::generate)
        .item()
        .transform(customItemModel("coupling_node", "base"))
        .register();


    public static final BlockEntry<TransmissionNodeBlock> TRANSMISSION_NODE = REGISTRATE.block("transmission_node", TransmissionNodeBlock::new)
        .initialProperties(CommonProperties::malleable)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(TransmissionNodeBlock.MODEL_TYPE)::generate)
        .item()
        .transform(customItemModel("transmission_node", "base"))
        .register();

    public static final BlockEntry<TransmissionConnectorBlock> CONNECTOR_TRANSMISSION = REGISTRATE.block("connector_transmission", TransmissionConnectorBlock::new)
        .initialProperties(CommonProperties::malleable)
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(TransmissionConnectorBlock.MODEL_TYPE)::generate)
        .item()
        .transform(customItemModel("connector_transmission", "base"))
        .register();

    public static final BlockEntry<ConnectorStackedTier0Block> CONNECTOR_STACKED_ZERO = REGISTRATE.block("connector_stacked_zero", ConnectorStackedTier0Block::new)
        .initialProperties(CommonProperties::malleable)
        .transform(pickaxeOnly())
        .blockstate(new StackedConnectorGenerator()::generate)
        .loot((lt, block) -> lt.dropOther(block, Blocks.AIR))
        .item()
        .transform(customItemModel("connector_stacked", "tier0"))
        .register();

    public static final BlockEntry<ConnectorStackedTier1Block> CONNECTOR_STACKED_ONE = REGISTRATE.block("connector_stacked_one", ConnectorStackedTier1Block::new)
        .initialProperties(CommonProperties::malleable)
        .transform(pickaxeOnly())
        .blockstate(new StackedConnectorGenerator()::generate)
        .loot((lt, block) -> lt.dropOther(block, Blocks.AIR))
        .item()
        .transform(customItemModel("connector_stacked", "tier1"))
        .register();
    
    public static final BlockEntry<ConnectorStackedTier2Block> CONNECTOR_STACKED_TWO = REGISTRATE.block("connector_stacked_two", ConnectorStackedTier2Block::new)
        .initialProperties(CommonProperties::malleable)
        .transform(pickaxeOnly())
        .blockstate(new StackedConnectorGenerator()::generate)
        .loot((lt, block) -> lt.dropOther(block, Blocks.AIR))
        .item()
        .transform(customItemModel("connector_stacked", "tier2"))
        .register();

    public static final BlockEntry<ConnectorStackedTier3Block> CONNECTOR_STACKED_THREE = REGISTRATE.block("connector_stacked_three", ConnectorStackedTier3Block::new)
        .initialProperties(CommonProperties::malleable)
        .transform(pickaxeOnly())
        .blockstate(new StackedConnectorGenerator()::generate)
        .loot((lt, block) -> lt.dropOther(block, Blocks.AIR))
        .item()
        .transform(customItemModel("connector_stacked", "tier3"))
        .register();


    public static final BlockEntry<DiagonalGirderBlock> DIAGONAL_GIRDER = REGISTRATE.block("diagonal_girder", DiagonalGirderBlock::new)
		.initialProperties(CommonProperties::malleable)
		.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
		.transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(DiagonalGirderBlock.MODEL_TYPE)::generate)
		.item()
        .transform(customItemModel("diagonal_girder", "item"))
		.register();



    public static final BlockEntry<VoltometerBlock> VOLTOMETER = REGISTRATE.block("voltometer", VoltometerBlock::new)
		.initialProperties(CommonProperties::malleable)
		.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
		.transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(VoltometerBlock.MODEL_TYPE)::generate)
		.item()
        .transform(customItemModel("voltometer", "base"))
		.register();
    

    public static final BlockEntry<TestBlock> TEST_BLOCK = REGISTRATE.block("test_block", TestBlock::new)
		.initialProperties(CommonProperties::malleable)
		.properties(p -> p
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .blockstate(new DynamicStateGenerator()::generate)
		.transform(pickaxeOnly())
		.item()
        .transform(customItemModel("test_block", "base"))
		.register();

    public static void register(IEventBus event) {
        Mechano.logReg("blocks");
    }

    public static class CommonProperties{

        public static Block malleable() {
            return Blocks.GOLD_BLOCK;
        }

        public static Block soft() {
            return Blocks.WHITE_WOOL;
        }

        public static Block ductile() {
            return Blocks.COPPER_BLOCK;
        }

        public static Block dense() {
            return Blocks.NETHERITE_BLOCK;
        }

        public static Block wooden() {
            return Blocks.SPRUCE_WOOD;
        }
    }
}
