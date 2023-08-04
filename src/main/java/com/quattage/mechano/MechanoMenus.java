package com.quattage.mechano;

import com.quattage.mechano.content.block.integrated.toolStation.ToolStationMenu;
import com.quattage.mechano.content.block.integrated.toolStation.ToolStationScreen;
import com.tterrag.registrate.builders.MenuBuilder.ForgeMenuFactory;
import com.tterrag.registrate.builders.MenuBuilder.ScreenFactory;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.eventbus.api.IEventBus;

public class MechanoMenus {
    
    public static final MenuEntry<ToolStationMenu> TOOL_STATION =
        register("tool_station", ToolStationMenu::new, () -> ToolStationScreen::new);


    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> MenuEntry<C> register(
        String name, ForgeMenuFactory<C> factory, NonNullSupplier<ScreenFactory<C, S>> screenFactory) {
        return Mechano.REGISTRATE
            .menu(name, factory, screenFactory)
            .register();
    }
    
    public static void register(IEventBus event) {
        Mechano.logReg("menus");

    }
}
