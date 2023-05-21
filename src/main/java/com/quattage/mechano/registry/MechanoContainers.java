package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.ToolStation.ToolStationContainer;
import com.quattage.mechano.content.block.ToolStation.ToolStationScreen;
import com.tterrag.registrate.builders.MenuBuilder.ForgeMenuFactory;
import com.tterrag.registrate.builders.MenuBuilder.ScreenFactory;
import com.tterrag.registrate.util.entry.MenuEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.eventbus.api.IEventBus;

public class MechanoContainers {
    
    public static final MenuEntry<ToolStationContainer> TOOL_STATION =
        register("tool_station", ToolStationContainer::new, () -> ToolStationScreen::new);


    private static <C extends AbstractContainerMenu, S extends Screen & MenuAccess<C>> MenuEntry<C> register(
        String name, ForgeMenuFactory<C> factory, NonNullSupplier<ScreenFactory<C, S>> screenFactory) {
        return Mechano.REGISTRATE
            .menu(name, factory, screenFactory)
            .register();
    }
    
    public static void register(IEventBus event) {
        Mechano.log("Registering Mechano containers");
    }
}
