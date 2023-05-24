package com.quattage.mechano.content.block.ToolStation;

import com.quattage.mechano.registry.MechanoMenus;
import com.simibubi.create.foundation.gui.menu.MenuBase;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ToolStationMenu extends MenuBase<ToolStationBlockEntity> {

    public ToolStationMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf buffer) {
		super(type, id, inv, buffer);
	}

    public ToolStationMenu(MenuType<?> type, int id, Inventory inv, ToolStationBlockEntity te) {
		super(type, id, inv, te);
	}

    public static ToolStationMenu create(int id, Inventory inv, ToolStationBlockEntity te) {
		return new ToolStationMenu(MechanoMenus.TOOL_STATION.get(), id, inv, te);
	}

    @Override
    protected ToolStationBlockEntity createOnClient(FriendlyByteBuf extraData) {
		ClientLevel world = Minecraft.getInstance().level;
		BlockEntity tileEntity = world.getBlockEntity(extraData.readBlockPos());
		if (tileEntity instanceof ToolStationBlockEntity toolStation) {
			toolStation.readClient(extraData.readNbt());
			return toolStation;
		}
		return null;
    }

    @Override
    protected void initAndReadInventory(ToolStationBlockEntity contentHolder) {
        // no implementation
    }

    @Override
    protected void saveData(ToolStationBlockEntity contentHolder) {
        // no implementation
    }

    @Override
    protected void addSlots() {
        throw new UnsupportedOperationException("Unimplemented method 'addSlots'");
    }

    

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot thisSlot = getSlot(index);

		if (!thisSlot.hasItem())
			return ItemStack.EMPTY;

		ItemStack stack = thisSlot.getItem();
		if (index < 5) {
			moveItemStackTo(stack, 5, slots.size(), false);
		} else {
			if (moveItemStackTo(stack, 0, 1, false) || moveItemStackTo(stack, 2, 3, false)
					|| moveItemStackTo(stack, 4, 5, false));
		}

		return ItemStack.EMPTY;
	}

}
