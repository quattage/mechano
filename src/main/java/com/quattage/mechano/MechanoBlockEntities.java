package com.quattage.mechano;

import com.quattage.mechano.content.block.integrated.toolStation.ToolStationBlockEntity;
import com.quattage.mechano.content.block.power.alternator.collector.CollectorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlockEntity;
import com.quattage.mechano.content.block.power.transfer.adapter.CouplingNodeBlockEntity;

import com.quattage.mechano.content.block.power.transfer.connector.transmission.TransmissionConnectorBlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.TransmissionConnectorRenderer;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier0Renderer;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier1BlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier1Renderer;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier2BlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier2Renderer;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier3BlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier3Renderer;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier0BlockEntity;

import com.quattage.mechano.content.block.power.transfer.test.TestBlockEntity;
import com.quattage.mechano.content.block.power.transfer.test.TestBlockRenderer;
import com.quattage.mechano.content.block.power.transfer.voltometer.VoltometerBlockEntity;
import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderBlockEntity;
import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderRenderer;
import com.simibubi.create.content.kinetics.base.CutoutRotatingInstance;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.minecraftforge.eventbus.api.IEventBus;

// This is where BlockEntities get regisrered.
public class MechanoBlockEntities {

    public static final BlockEntityEntry<ToolStationBlockEntity> TOOL_STATION = Mechano.REGISTRATE
            .blockEntity("tool_station", ToolStationBlockEntity::new)
            .validBlocks(MechanoBlocks.TOOL_STATION)
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

    public static final BlockEntityEntry<TransmissionConnectorBlockEntity> TRANSMISSION_CONNECTOR = Mechano.REGISTRATE
            .blockEntity("transmission_connector", TransmissionConnectorBlockEntity::new)
            .validBlocks(MechanoBlocks.CONNECTOR_TRANSMISSION)
            .renderer(() -> TransmissionConnectorRenderer::new)
            .register();

    public static final BlockEntityEntry<ConnectorStackedTier0BlockEntity> STACKED_CONNECTOR_ZERO = Mechano.REGISTRATE
            .blockEntity("stacked_connector_zero", ConnectorStackedTier0BlockEntity::new)
            .validBlocks(MechanoBlocks.CONNECTOR_STACKED_ZERO)
            .renderer(() -> ConnectorStackedTier0Renderer::new)
            .register();

    public static final BlockEntityEntry<ConnectorStackedTier1BlockEntity> STACKED_CONNECTOR_ONE = Mechano.REGISTRATE
            .blockEntity("stacked_connector_one", ConnectorStackedTier1BlockEntity::new)
            .validBlocks(MechanoBlocks.CONNECTOR_STACKED_ONE)
            .renderer(() -> ConnectorStackedTier1Renderer::new)
            .register();

    public static final BlockEntityEntry<ConnectorStackedTier2BlockEntity> STACKED_CONNECTOR_TWO = Mechano.REGISTRATE
            .blockEntity("stacked_connector_two", ConnectorStackedTier2BlockEntity::new)
            .validBlocks(MechanoBlocks.CONNECTOR_STACKED_TWO)
            .renderer(() -> ConnectorStackedTier2Renderer::new)
            .register();

    public static final BlockEntityEntry<ConnectorStackedTier3BlockEntity> STACKED_CONNECTOR_THREE = Mechano.REGISTRATE
        .blockEntity("stacked_connector_three", ConnectorStackedTier3BlockEntity::new)
        .validBlocks(MechanoBlocks.CONNECTOR_STACKED_THREE)
        .renderer(() -> ConnectorStackedTier3Renderer::new)
        .register();


    public static final BlockEntityEntry<DiagonalGirderBlockEntity> DIAGONAL_GIRDER = Mechano.REGISTRATE
            .blockEntity("diagonal_girder", DiagonalGirderBlockEntity::new)
            .validBlocks(MechanoBlocks.DIAGONAL_GIRDER)
            .renderer(() -> DiagonalGirderRenderer::new)
            .register();

    public static final BlockEntityEntry<VoltometerBlockEntity> VOLTOMETER = Mechano.REGISTRATE
            .blockEntity("voltometer", VoltometerBlockEntity::new)
            .validBlocks(MechanoBlocks.VOLTOMETER)
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
