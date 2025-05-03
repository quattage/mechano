package com.quattage.mechano;

import com.quattage.mechano.content.spool.HookupWireSpoolItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.neoforged.bus.api.IEventBus;

import static com.quattage.mechano.Mechano.REGISTRATE;

public class MechanoItems {

    static {
        REGISTRATE.setCreativeTab(MechanoGroups.BASE);
    }

    public static final ItemEntry<HookupWireSpoolItem> HOOKUP_WIRE_SPOOL = REGISTRATE.item("hookup_spool", HookupWireSpoolItem::new)
        .register();
    
    public static void register(IEventBus modBus) {
        Mechano.LOGGER.debug("registering items");
    }
}
