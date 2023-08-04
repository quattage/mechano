package com.quattage.mechano;

import com.quattage.mechano.content.item.spool.EmptySpool;
import com.quattage.mechano.content.item.spool.HookupWireSpool;
import com.quattage.mechano.content.item.wire.HookupWire;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraftforge.eventbus.api.IEventBus;
import static com.quattage.mechano.Mechano.REGISTRATE;

// This is where Items go to get registered.
public class MechanoItems {
    
    static {
		REGISTRATE.useCreativeTab(MechanoGroups.MAIN_TAB);
	}

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
