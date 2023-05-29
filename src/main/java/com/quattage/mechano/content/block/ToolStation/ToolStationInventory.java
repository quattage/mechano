package com.quattage.mechano.content.block.ToolStation;

import net.minecraftforge.items.ItemStackHandler;

public class ToolStationInventory extends ItemStackHandler {
    private final ToolStationBlockEntity attachedBE;

    public ToolStationInventory(ToolStationBlockEntity attachedBE) {
		super(5);
		this.attachedBE = attachedBE;
	}
}
