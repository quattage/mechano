package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.integrated.toolStation.ToolStationBlockEntity;
import com.quattage.mechano.content.block.power.Inductor.InductorBlockEntity;
import com.quattage.mechano.content.block.power.Inductor.InductorBlockRenderer;
import com.quattage.mechano.content.block.power.alternator.collector.CollectorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlockEntity;
import com.quattage.mechano.content.block.power.alternator.stator.StatorBlockEntity;
import com.simibubi.create.content.kinetics.base.CutoutRotatingInstance;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.minecraftforge.eventbus.api.IEventBus;

// This is where TileEntities get regisrered.
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

    public static void register(IEventBus event) {
        Mechano.logReg("block entities");
    }


}
