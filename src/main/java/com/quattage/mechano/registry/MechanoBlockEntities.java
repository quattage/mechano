package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.Alternator.Rotor.RotorBlockEntity;
import com.quattage.mechano.content.block.Inductor.InductorBlockEntity;
import com.quattage.mechano.content.block.Inductor.InductorBlockRenderer;
import com.quattage.mechano.content.block.ToolStation.ToolStationBlockEntity;
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

    public static void register(IEventBus event) {
        Mechano.logReg("block entities");
    }


}
