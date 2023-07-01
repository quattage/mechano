package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.spool.EmptySpool;
import com.quattage.mechano.content.item.spool.HookupWireSpool;
import com.quattage.mechano.content.item.wire.HookupWire;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraftforge.eventbus.api.IEventBus;

// This is where Items go to get registered.
public class MechanoItems {
    public static CreateRegistrate REGISTRATE = Mechano.REGISTRATE.creativeModeTab(() -> MechanoGroup.PRIMARY);

    public static final ItemEntry<EmptySpool> EMPTY_SPOOL = REGISTRATE.item("empty_spool", EmptySpool::new)
        .register();

    public static final ItemEntry<HookupWire> HOOKUP_WIRE = REGISTRATE.item("hookup_wire", HookupWire::new)
        .register();

    public static final ItemEntry<HookupWireSpool> HOOKUP_WIRE_SPOOL = REGISTRATE.item("hookup_spool", HookupWireSpool::new)
        .register();

    public static void register(IEventBus event) {
        Mechano.logReg("items");
    }
}
