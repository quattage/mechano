package com.quattage.mechano.content.item.spool;

import com.quattage.mechano.MechanoItems;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;

import net.minecraft.world.item.Item;

public class HookupWireSpool extends WireSpool {

    public HookupWireSpool(Properties properties) {
        super(properties);
    }

    @Override
    protected int setRate() {
        return 4096;
    }

    @Override
    protected Item setRawDrop() {
        return MechanoItems.HOOKUP_WIRE.get();
    }

    @Override
    public String setSpoolName() {
        return "hookup";
    }
}
