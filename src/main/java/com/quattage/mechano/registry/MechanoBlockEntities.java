package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.Inductor.InductorBlockEntity;
import com.quattage.mechano.content.block.ToolStation.ToolStationBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.minecraftforge.eventbus.api.IEventBus;

// This is where TileEntities get regisrered.
public class MechanoBlockEntities {
    public static final BlockEntityEntry<InductorBlockEntity> INDUCTOR = Mechano.REGISTRATE
        .tileEntity("inductor", InductorBlockEntity::new)
        .validBlocks(MechanoBlocks.INDUCTOR)
        .register();

    public static final BlockEntityEntry<ToolStationBlockEntity> TOOL_STATION = Mechano.REGISTRATE
        .tileEntity("tool_station", ToolStationBlockEntity::new)
        .validBlocks(MechanoBlocks.INDUCTOR)
        .register();
    

    public static void register(IEventBus event) {
        Mechano.log("Registering Mechano block entities");
    }
}
