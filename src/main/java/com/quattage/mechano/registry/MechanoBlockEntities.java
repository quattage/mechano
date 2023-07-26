package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.integrated.toolStation.ToolStationBlockEntity;
import com.quattage.mechano.content.block.machine.inductor.InductorBlockEntity;
import com.quattage.mechano.content.block.machine.inductor.InductorBlockRenderer;
import com.quattage.mechano.content.block.power.alternator.collector.CollectorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlockEntity;
import com.quattage.mechano.content.block.power.transfer.adapter.CouplingNodeBlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.HeapConnectorBlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.HeapConnectorRenderer;
import com.quattage.mechano.content.block.power.transfer.connector.HeapConnectorStackedBlockEntity;
import com.quattage.mechano.content.block.power.transfer.test.TestBlockEntity;
import com.quattage.mechano.content.block.power.transfer.test.TestBlockRenderer;
import com.quattage.mechano.content.block.power.transfer.voltometer.VolotmeterRenderer;
import com.quattage.mechano.content.block.power.transfer.voltometer.VoltometerBlockEntity;
import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderBlockEntity;
import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderRenderer;
import com.simibubi.create.content.kinetics.base.CutoutRotatingInstance;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.minecraftforge.eventbus.api.IEventBus;

// This is where BlockEntities get regisrered.
public class MechanoBlockEntities {
    public static final BlockEntityEntry<InductorBlockEntity> INDUCTOR = Mechano.REGISTRATE
            .blockEntity("inductor", InductorBlockEntity::new)
            .validBlocks(MechanoBlocks.INDUCTOR)
            .renderer(() -> InductorBlockRenderer::new)
            .register();

    public static final BlockEntityEntry<ToolStationBlockEntity> TOOL_STATION = Mechano.REGISTRATE
            .blockEntity("tool_station", ToolStationBlockEntity::new)
            .validBlocks(MechanoBlocks.INDUCTOR)
            .register();

    public static final BlockEntityEntry<RotorBlockEntity> ROTOR = Mechano.REGISTRATE
            .blockEntity("rotor", RotorBlockEntity::new)
            .instance(() -> CutoutRotatingInstance::new, false)
            .validBlocks(MechanoBlocks.ROTOR)
            .renderer(() -> KineticBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<CollectorBlockEntity> COLLECTOR = Mechano.REGISTRATE
            .blockEntity("collector", CollectorBlockEntity::new)
            .instance(() -> CutoutRotatingInstance::new, false)
            .validBlocks(MechanoBlocks.COLLECTOR)
            .renderer(() -> KineticBlockEntityRenderer::new)
            .register();

    public static final BlockEntityEntry<StatorBlockEntity> STATOR = Mechano.REGISTRATE
            .blockEntity("stator", StatorBlockEntity::new)
            .validBlocks(MechanoBlocks.STATOR)
            .register();

    public static final BlockEntityEntry<CouplingNodeBlockEntity> COUPLING_NODE = Mechano.REGISTRATE
            .blockEntity("coupling_node", CouplingNodeBlockEntity::new)
            .validBlocks(MechanoBlocks.COUPLING_NODE)
            .register();

    public static final BlockEntityEntry<HeapConnectorBlockEntity> HEAP_CONNECTOR = Mechano.REGISTRATE
            .blockEntity("heap_connector", HeapConnectorBlockEntity::new)
            .validBlocks(MechanoBlocks.HEAP_CONNECTOR)
            .renderer(() -> HeapConnectorRenderer::new)
            .register();

    public static final BlockEntityEntry<HeapConnectorStackedBlockEntity> HEAP_CONNECTOR_STACKED = Mechano.REGISTRATE
            .blockEntity("heap_connector_stacked", HeapConnectorStackedBlockEntity::new)
            .validBlocks(MechanoBlocks.HEAP_CONNECTOR_STACKED)
            .register();

    public static final BlockEntityEntry<DiagonalGirderBlockEntity> DIAGONAL_GIRDER = Mechano.REGISTRATE
            .blockEntity("diagonal_girder", DiagonalGirderBlockEntity::new)
            .validBlocks(MechanoBlocks.DIAGONAL_GIRDER)
            .renderer(() -> DiagonalGirderRenderer::new)
            .register();

    public static final BlockEntityEntry<VoltometerBlockEntity> VOLTOMETER = Mechano.REGISTRATE
            .blockEntity("voltometer", VoltometerBlockEntity::new)
            .validBlocks(MechanoBlocks.VOLTOMETER)
            .renderer(() -> VolotmeterRenderer::new)
            .register();

    public static final BlockEntityEntry<TestBlockEntity> TEST_BLOCK = Mechano.REGISTRATE
            .blockEntity("test", TestBlockEntity::new)
            .validBlocks(MechanoBlocks.TEST_BLOCK)
            .renderer(() -> TestBlockRenderer::new)
            .register();

    public static void register(IEventBus event) {
        Mechano.logReg("block entities");
    }
}
