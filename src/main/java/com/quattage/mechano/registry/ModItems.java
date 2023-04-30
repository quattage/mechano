package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.Inductor.InductorBlockItem;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;

public class ModItems {
    public static final Item INDUCTOR_ITEM = registerItem("inductor_item", new InductorBlockItem(ModBlocks.INDUCTOR.get(), new FabricItemSettings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, Mechano.newResource(name), item);
    }

    public static void register() {
        Mechano.LOGGER.info("Registering Mod Items");
    }
}
