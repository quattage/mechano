package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public class MechanoGroups extends ItemGroup {
    public static MechanoGroups PRIMARY;

    public MechanoGroups(int index, String id) {
        super(index, Mechano.MOD_ID + ":" + id);
    }
    @Override
    public ItemStack createIcon() {
        return new ItemStack(MechanoBlocks.INDUCTOR.get());
    }
}
