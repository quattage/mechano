package com.quattage.mechano.content.block.ToolStation;

import net.minecraftforge.items.ItemStackHandler;

public class ToolStationInventory extends ItemStackHandler {
    private final ToolStationBlockEntity station;

    public ToolStationInventory(ToolStationBlockEntity station) {
        super(5);
        this.station = station;
    }

    @Override
	protected void onContentsChanged(int slot) {
		super.onContentsChanged(slot);
		station.setChanged();
	}
}
