package com.quattage.mechano;

import com.quattage.mechano.content.block.integrated.toolStation.ToolStationBlock;
import com.quattage.mechano.content.block.integrated.toolStation.UpgradeBlock;
import com.quattage.mechano.content.block.power.alternator.collector.CollectorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlock;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlock;
import com.quattage.mechano.content.block.power.transfer.adapter.CouplingNodeBlock;
import com.quattage.mechano.content.block.power.transfer.adapter.TransmissionNodeBlock;
import com.quattage.mechano.content.block.power.transfer.connector.HeapConnectorBlock;
import com.quattage.mechano.content.block.power.transfer.connector.HeapConnectorStackedBlock;
import com.quattage.mechano.content.block.power.transfer.test.TestBlock;
import com.quattage.mechano.content.block.power.transfer.voltometer.VoltometerBlock;
import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderBlock;
import com.simibubi.create.content.kinetics.BlockStressDefaults;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.eventbus.api.IEventBus;

import static com.quattage.mechano.Mechano.REGISTRATE;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;


//This is where blocks go to get registered
public class MechanoBlocks {
    static {
		REGISTRATE.useCreativeTab(MechanoGroups.MAIN_TAB);
	}

    public static final BlockEntry<UpgradeBlock> FORGE_UPGRADE = REGISTRATE.block("tool_forge", UpgradeBlock::new)
        .initialProperties(CommonProperties::hardMetal)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<ToolStationBlock> TOOL_STATION = REGISTRATE.block("tool_station", ToolStationBlock::new)
        .initialProperties(CommonProperties::wood)
        .properties(props -> props
            .sound(SoundType.WOOD)
            .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<RotorBlock> ROTOR = REGISTRATE.block("rotor", RotorBlock::new)
        .initialProperties(CommonProperties::copper)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(BlockStressDefaults.setImpact(48.0))
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<CollectorBlock> COLLECTOR = REGISTRATE.block("collector", CollectorBlock::new)
        .initialProperties(CommonProperties::softMetal)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(BlockStressDefaults.setImpact(48.0))
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<StatorBlock> STATOR = REGISTRATE.block("stator", StatorBlock::new)
        .initialProperties(CommonProperties::hardMetal)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<CouplingNodeBlock> COUPLING_NODE = REGISTRATE.block("coupling_node", CouplingNodeBlock::new)
        .initialProperties(CommonProperties::softMetal)
        .properties(props -> props
        .sound(SoundType.NETHERITE_BLOCK)
        .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();


    public static final BlockEntry<TransmissionNodeBlock> TRANSMISSION_NODE = REGISTRATE.block("transmission_node", TransmissionNodeBlock::new)
        .initialProperties(CommonProperties::softMetal)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<HeapConnectorBlock> HEAP_CONNECTOR = REGISTRATE.block("heap_connector", HeapConnectorBlock::new)
        .initialProperties(CommonProperties::softMetal)
        .transform(pickaxeOnly())
        .simpleItem()
        .register();

    public static final BlockEntry<HeapConnectorStackedBlock> HEAP_CONNECTOR_STACKED = REGISTRATE.block("heap_connector_stacked", HeapConnectorStackedBlock::new)
        .initialProperties(CommonProperties::softMetal)
        .transform(pickaxeOnly())
        .simpleItem()
        .register();


    public static final BlockEntry<DiagonalGirderBlock> DIAGONAL_GIRDER = REGISTRATE.block("diagonal_girder", DiagonalGirderBlock::new)
		.initialProperties(CommonProperties::softMetal)
		.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
		.transform(pickaxeOnly())
		.item()
		.transform(customItemModel())
		.register();



    public static final BlockEntry<VoltometerBlock> VOLTOMETER = REGISTRATE.block("voltometer", VoltometerBlock::new)
		.initialProperties(CommonProperties::softMetal)
		.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
		.transform(pickaxeOnly())
		.item()
		.transform(customItemModel())
		.register();
    

    public static final BlockEntry<TestBlock> TEST_BLOCK = REGISTRATE.block("test_block", TestBlock::new)
		.initialProperties(CommonProperties::softMetal)
		.properties(p -> p
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
		.transform(pickaxeOnly())
		.item()
		.transform(customItemModel())
		.register();

    public static void register(IEventBus event) {
        Mechano.logReg("blocks");
    }

    public static class CommonProperties{

        public static Block hardMetal() {
            return Blocks.NETHERITE_BLOCK;
        }

        public static Block softMetal() {
            return Blocks.GOLD_BLOCK;
        }

        public static Block copper() {
            return Blocks.COPPER_BLOCK;
        }

        public static Block wood() {
            return Blocks.SPRUCE_WOOD;
        }
    }
}
