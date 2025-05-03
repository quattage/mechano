package com.quattage.mechano;

import com.quattage.mechano.foundation.helper.CreativeTabOverridable;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
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
            .title(Component.translatable("itemGroup." + Mechano.ID + ".base"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> MechanoBlocks.TEST_AXIS.asStack())
            .displayItems(new GroupExclusionsGenerator(MechanoGroups.BASE))
            .build()
    );

    public static void register(IEventBus modBus) {
        TAB_REGISTRY.register(modBus);
        Mechano.LOGGER.debug("registering groups");
    }

    private static class GroupExclusionsGenerator implements DisplayItemsGenerator {

        private final DeferredHolder<CreativeModeTab, CreativeModeTab> tab;
        private final ReferenceLinkedOpenHashSet<Item> collectedItems  = new ReferenceLinkedOpenHashSet<>();

        public GroupExclusionsGenerator(DeferredHolder<CreativeModeTab, CreativeModeTab> tab) {
            this.tab = tab;
        }

        @Override
        public void accept(ItemDisplayParameters parameters, Output output) {
            collectedItems.clear();
            collectBlocks();
            collectItems();
            for(Item item : collectedItems)
                output.accept(new ItemStack(item));
            collectedItems.clear();
            collectedItems.trim();
        }

        private ReferenceLinkedOpenHashSet<Item> collectBlocks() {
            for(RegistryEntry<Block, Block> blockEntry : Mechano.REGISTRATE.getAll(Registries.BLOCK)) {
                if(!CreateRegistrate.isInCreativeTab(blockEntry, tab)) continue;
                Item blockItem = blockEntry.get().asItem();
                if(blockItem == Items.AIR) continue;
                if(tab.get().contains(new ItemStack(blockItem))) continue;
                if(!CreativeTabOverridable.belongsTo(blockItem, tab)) continue;
                if(blockItem == Items.AIR) continue;
                collectedItems.add(blockItem);
            }
            return collectedItems;
        }

        private ReferenceLinkedOpenHashSet<Item> collectItems() {
            for(RegistryEntry<Item, Item> itemEntry : Mechano.REGISTRATE.getAll(Registries.ITEM)) {
                if(!CreateRegistrate.isInCreativeTab(itemEntry, tab)) continue;
                Item item = itemEntry.get();
                if(item instanceof BlockItem) continue;
                if(item == Items.AIR) continue;
                if(tab.get().contains(new ItemStack(item))) continue;
                if(!CreativeTabOverridable.belongsTo(item, tab)) continue;
                collectedItems.add(item);
            }
            return collectedItems;
        }
    }
}
