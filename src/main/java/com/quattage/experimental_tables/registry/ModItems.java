package com.quattage.experimental_tables.registry;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import com.quattage.experimental_tables.ExperimentalTables;

public class ModItems {
    public static void register() {
        ExperimentalTables.LOGGER.info("Registering Mod Items");
        
    }

    private static Item registerItem(String name, ItemGroup group) {
        Item item = Registry.register(Registry.ITEM, new Identifier(ExperimentalTables.MOD_ID, name),
            new Item(new FabricItemSettings().group(group)));
        return item;
    }
}
