package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.integrated.toolStation.ToolStationBlock;
import com.quattage.mechano.content.block.integrated.toolStation.UpgradeBlock;
import com.quattage.mechano.content.block.machine.inductor.InductorBlock;
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
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.eventbus.api.IEventBus;


//This is where blocks go to get registered
public class MechanoBlocks {
    public static CreateRegistrate REGISTRATE = Mechano.REGISTRATE.creativeModeTab(() -> MechanoGroup.PRIMARY);

    public static final BlockEntry<UpgradeBlock> FORGE_UPGRADE = REGISTRATE.block("tool_forge", UpgradeBlock::new)
        .initialProperties(Material.METAL)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<InductorBlock> INDUCTOR = REGISTRATE.block("inductor", InductorBlock::new)
        .initialProperties(Material.METAL)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<ToolStationBlock> TOOL_STATION = REGISTRATE.block("tool_station", ToolStationBlock::new)
        .initialProperties(Material.WOOD)
        .properties(props -> props
            .sound(SoundType.WOOD)
            .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<RotorBlock> ROTOR = REGISTRATE.block("rotor", RotorBlock::new)
        .initialProperties(Material.METAL)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .color(MaterialColor.COLOR_ORANGE)
            .noOcclusion()
        )
        .transform(BlockStressDefaults.setImpact(48.0))
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<CollectorBlock> COLLECTOR = REGISTRATE.block("collector", CollectorBlock::new)
        .initialProperties(Material.METAL)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .color(MaterialColor.COLOR_ORANGE)
            .noOcclusion()
        )
        .transform(BlockStressDefaults.setImpact(48.0))
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<StatorBlock> STATOR = REGISTRATE.block("stator", StatorBlock::new)
        .initialProperties(Material.METAL)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .color(MaterialColor.COLOR_GRAY)
            .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<CouplingNodeBlock> COUPLING_NODE = REGISTRATE.block("coupling_node", CouplingNodeBlock::new)
        .initialProperties(Material.METAL)
        .properties(props -> props
        .sound(SoundType.NETHERITE_BLOCK)
        .color(MaterialColor.COLOR_GRAY)
        .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();


    public static final BlockEntry<TransmissionNodeBlock> TRANSMISSION_NODE = REGISTRATE.block("transmission_node", TransmissionNodeBlock::new)
        .initialProperties(Material.METAL)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .color(MaterialColor.COLOR_GRAY)
            .noOcclusion()
        )
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<HeapConnectorBlock> HEAP_CONNECTOR = REGISTRATE.block("heap_connector", HeapConnectorBlock::new)
        .initialProperties(SharedProperties::softMetal)
        .transform(pickaxeOnly())
        .simpleItem()
        .register();

    public static final BlockEntry<HeapConnectorStackedBlock> HEAP_CONNECTOR_STACKED = REGISTRATE.block("heap_connector_stacked", HeapConnectorStackedBlock::new)
        .initialProperties(SharedProperties::softMetal)
        .transform(pickaxeOnly())
        .simpleItem()
        .register();


    public static final BlockEntry<DiagonalGirderBlock> DIAGONAL_GIRDER = REGISTRATE.block("diagonal_girder", DiagonalGirderBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.properties(p -> p.color(MaterialColor.COLOR_GRAY))
		.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
		.transform(pickaxeOnly())
		.item()
		.transform(customItemModel())
		.register();



    public static final BlockEntry<VoltometerBlock> VOLTOMETER = REGISTRATE.block("voltometer", VoltometerBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.properties(p -> p.color(MaterialColor.COLOR_GRAY))
		.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
		.transform(pickaxeOnly())
		.item()
		.transform(customItemModel())
		.register();
    

    public static final BlockEntry<TestBlock> TEST_BLOCK = REGISTRATE.block("test_block", TestBlock::new)
		.initialProperties(SharedProperties::softMetal)
		.properties(p -> p
            .sound(SoundType.NETHERITE_BLOCK)
            .color(MaterialColor.COLOR_GRAY)
            .noOcclusion()
        )
		.transform(pickaxeOnly())
		.item()
		.transform(customItemModel())
		.register();

    public static void register(IEventBus event) {
        Mechano.logReg("blocks");
    }
}
