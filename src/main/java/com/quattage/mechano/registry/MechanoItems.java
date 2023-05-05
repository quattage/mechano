package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.IndrumIngot.IndrumIngotItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;


public class MechanoItems {
    private static final CreateRegistrate REGISTRATE = Mechano.registrate().creativeModeTab(() -> MechanoGroups.PRIMARY);

    public static final ItemEntry<IndrumIngotItem> INDRUM_INGOT = REGISTRATE.item("indrum_ingot", IndrumIngotItem::new).register();

    public static void register() {
        Mechano.log("Registering Mod Items");
    }
}
