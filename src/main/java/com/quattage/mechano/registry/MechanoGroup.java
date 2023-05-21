package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

// This is where TileEntities get regisrered.
public class MechanoGroup extends CreativeModeTab {
    public static MechanoGroup PRIMARY;
	
	public MechanoGroup(String id) {
		super(Mechano.MOD_ID+":" + id);
		Mechano.logReg("group: '" + id + "'");
		PRIMARY = this;
	}

	@Override
	public ItemStack makeIcon() {
		return new ItemStack(Blocks.DIRT);
	}
}
