package com.quattage.mechano;

import static com.quattage.mechano.Mechano.REGISTRATE;

import com.quattage.mechano.content.spool.EmptySpoolItem;
import com.quattage.mechano.content.spool.HookupSpoolItem;
import com.quattage.mechano.infrastructure.datagen.SpoolDataProvider;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.neoforged.bus.api.IEventBus;

public class MechanoItems {

    static { REGISTRATE.setCreativeTab(MechanoGroups.BASE); }

    public static final ItemEntry<EmptySpoolItem> SPOOL_EMPTY = REGISTRATE.item("spool_empty", EmptySpoolItem::new)
        .register();

    public static final ItemEntry<HookupSpoolItem> SPOOL_HOOKUP = REGISTRATE.item("spool_hookup", HookupSpoolItem::new)
        .setData(ProviderType.ITEM_MODEL, SpoolDataProvider::generate)
        .properties(p -> p.durability(512).stacksTo(1).setNoRepair().craftRemainder(MechanoItems.SPOOL_EMPTY.get().asItem()))
        .register();

    public static void register(IEventBus modBus) {
        Mechano.LOGGER.debug("registering items");
    }
}
