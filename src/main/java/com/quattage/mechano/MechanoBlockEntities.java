package com.quattage.mechano;

import com.quattage.mechano.content.block.power.alternator.rotor.BigRotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.rotor.SmallRotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.rotor.dummy.BigRotorDummyBlockEntity;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.TieredConnectorRenderer;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.ConnectorTier0BlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.ConnectorTier1BlockEntity;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.ConnectorTier2BlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.minecraftforge.eventbus.api.IEventBus;

// This is where BlockEntities get registered.
public class MechanoBlockEntities {

    // public static final BlockEntityEntry<ToolStationBlockEntity> TOOL_STATION = Mechano.REGISTRATE
	// 	.blockEntity("tool_station", ToolStationBlockEntity::new)
	// 	.validBlocks(MechanoBlocks.TOOL_STATION)
	// 	.register();

    public static final BlockEntityEntry<SmallRotorBlockEntity> SMALL_ROTOR = Mechano.REGISTRATE
		.blockEntity("small_rotor", SmallRotorBlockEntity::new)
		.visual(MechanoDynamicResources::ofSmallRotor, false)
		.validBlocks(MechanoBlocks.SMALL_ROTOR)
		.renderer(() -> KineticBlockEntityRenderer::new)
		.register();

	public static final BlockEntityEntry<BigRotorBlockEntity> BIG_ROTOR = Mechano.REGISTRATE
		.blockEntity("big_rotor", BigRotorBlockEntity::new)
		.visual(MechanoDynamicResources::ofBigRotor, false)
		.validBlocks(MechanoBlocks.BIG_ROTOR)
		.renderer(() -> KineticBlockEntityRenderer::new)
		.register();

	public static final BlockEntityEntry<BigRotorDummyBlockEntity> BIG_ROTOR_DUMMY = Mechano.REGISTRATE
		.blockEntity("big_rotor_dummy", BigRotorDummyBlockEntity::new)
		.validBlocks(MechanoBlocks.BIG_ROTOR_DUMMY)
		.register();

    public static final BlockEntityEntry<SlipRingShaftBlockEntity> SLIP_RING_SHAFT = Mechano.REGISTRATE
		.blockEntity("slip_ring_shaft", SlipRingShaftBlockEntity::new)
		.visual(MechanoDynamicResources::ofSlipRingShaft, false)
		.validBlocks(MechanoBlocks.SLIP_RING_SHAFT)
		.renderer(() -> KineticBlockEntityRenderer::new)
		.register();

    public static final BlockEntityEntry<ConnectorTier0BlockEntity> CONNECTOR_T0 = Mechano.REGISTRATE
		.blockEntity("connector_tier_zero", ConnectorTier0BlockEntity::new)
		.validBlocks(MechanoBlocks.CONNECTOR_T0)
		.renderer(() -> TieredConnectorRenderer::new)
		.register();

    public static final BlockEntityEntry<ConnectorTier1BlockEntity> CONNECTOR_T1 = Mechano.REGISTRATE
		.blockEntity("connector_tier_one", ConnectorTier1BlockEntity::new)
		.validBlocks(MechanoBlocks.CONNECTOR_T1)
		.renderer(() -> TieredConnectorRenderer::new)
		.register();

    public static final BlockEntityEntry<ConnectorTier2BlockEntity> CONNECTOR_T2 = Mechano.REGISTRATE
		.blockEntity("connector_tier_two", ConnectorTier2BlockEntity::new)
		.validBlocks(MechanoBlocks.CONNECTOR_T2)
		.renderer(() -> TieredConnectorRenderer::new)
		.register();

    // public static final BlockEntityEntry<DiagonalGirderBlockEntity> DIAGONAL_GIRDER = Mechano.REGISTRATE
	// 	.blockEntity("diagonal_girder", DiagonalGirderBlockEntity::new)
	// 	.validBlocks(MechanoBlocks.DIAGONAL_GIRDER)
	// 	.renderer(() -> DiagonalGirderRenderer::new)
	// 	.register();

    // public static final BlockEntityEntry<VoltometerBlockEntity> VOLTOMETER = Mechano.REGISTRATE
	// 	.blockEntity("voltometer", VoltometerBlockEntity::new)
	// 	.validBlocks(MechanoBlocks.VOLTOMETER)
	// 	.register();

    // public static final BlockEntityEntry<TestBlockEntity> TEST_BLOCK = Mechano.REGISTRATE
	// 	.blockEntity("test", TestBlockEntity::new)
	// 	.validBlocks(MechanoBlocks.TEST_BLOCK)
	// 	.renderer(() -> TestBlockRenderer::new)
	// 	.register();

    public static void register(IEventBus event) {
	Mechano.logReg("block entities");
    }
}
