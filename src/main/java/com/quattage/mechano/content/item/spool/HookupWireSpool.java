package com.quattage.mechano.content.item.spool;

import com.quattage.mechano.registry.MechanoItems;

import net.minecraft.world.item.Item;

public class HookupWireSpool extends WireSpool {

    public HookupWireSpool(Properties properties) {
        super(properties);
    }

    @Override
    protected String setName() {
        return "hookup";
    }

    @Override
    protected int setRate() {
        return 4096;
    }

    @Override
    protected Item setRawDrop() {
        return MechanoItems.HOOKUP_WIRE.get();
    }
}
