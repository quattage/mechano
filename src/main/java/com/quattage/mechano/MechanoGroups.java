package com.quattage.mechano;

import com.quattage.mechano.foundation.helper.CreativeTabOverridable;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab.DisplayItemsGenerator;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MechanoGroups {

    private static final DeferredRegister<CreativeModeTab> TAB_REGISTRY = DeferredRegister.
        create(Registries.CREATIVE_MODE_TAB, Mechano.ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BASE = 
        TAB_REGISTRY.register("base", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> MechanoBlocks.TEST_AXIS.asItem().getDefaultInstance())
            .title(Component.translatable("itemGroup." + Mechano.ID + ".base"))
            .displayItems(new GroupExclusionsGenerator(MechanoGroups.BASE))
            .build()
    );

    public static void register(IEventBus modBus) {
        TAB_REGISTRY.register(modBus);
        Mechano.LOGGER.debug("registering groups");
    }

    private static class GroupExclusionsGenerator implements DisplayItemsGenerator {

        private final DeferredHolder<CreativeModeTab, CreativeModeTab> tab;

        public GroupExclusionsGenerator(DeferredHolder<CreativeModeTab, CreativeModeTab> tab) {
            this.tab = tab;
        }

        @Override
        public void accept(ItemDisplayParameters parameters, Output output) {
            Mechano.REGISTRATE.getAll(Registries.ITEM).forEach(entry -> {
                Item item = entry.get();
                if(item == Items.AIR) return;
                if(!CreativeTabOverridable.belongsTo(item, tab)) return;
                output.accept(item);
            });
        }

        // private ReferenceLinkedOpenHashSet<Item> collectBlocks() {
        //     for(RegistryEntry<Block, Block> blockEntry : Mechano.REGISTRATE.getAll(Registries.BLOCK)) {
        //         if(!CreateRegistrate.isInCreativeTab(blockEntry, tab)) continue;
        //         Item blockItem = blockEntry.get().asItem();
        //         if(blockItem == Items.AIR) continue;
        //         if(!CreativeTabOverridable.belongsTo(blockItem, tab)) continue;
        //         collectedItems.add(blockItem);
        //     }
        //     return collectedItems;
        // }

        
    }
}
