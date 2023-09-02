package com.quattage.mechano;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.quattage.mechano.foundation.helper.CreativeTabExcludable;
import com.tterrag.registrate.util.entry.RegistryEntry;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// This is where TileEntities get regisrered.
public class MechanoGroups {
    
    private static final DeferredRegister<CreativeModeTab> TAB_REGISTER =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Mechano.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = newTab(() -> MechanoBlocks.ROTOR.asStack(), "main");

    public static RegistryObject<CreativeModeTab> newTab(Supplier<ItemStack> icon, String name) {
        return TAB_REGISTER.register(name,
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Mechano.MOD_ID + "." + name))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(icon)
            .displayItems(new ExclusionsGenerator(name))
            .build()
        );
    }

    public static void register(IEventBus bus) {
        Mechano.logReg("creative tabs");
        TAB_REGISTER.register(bus);
    }


    public static class ExclusionsGenerator implements DisplayItemsGenerator {

        private static RegistryObject<CreativeModeTab> tab;

        public ExclusionsGenerator(String tabName) {
            boolean found = false;
            for(RegistryObject<CreativeModeTab> thisTab : TAB_REGISTER.getEntries()) {
                if(Mechano.asResource(tabName).equals(thisTab.getId())) {
                    found = true;
                    tab = thisTab;
                }
            }

            if(!found) throw new IllegalStateException("Failed to register DisplayItemsGenerator - '" + tabName + "' was not found in the registry.");
        }

        @Override
        public void accept(ItemDisplayParameters parameters, Output output) {
            Function<Item, ItemStack> stackoWacko = newStackoWacko();

            List<Item> inclusions = new LinkedList<>();
            inclusions.addAll(getBlocks());
            inclusions.addAll(getItems());
            
            apply(output, inclusions, stackoWacko);

        }
        

        public static List<Item> getBlocks() {
            List<Item> out  = new ReferenceArrayList<>();
            for(RegistryEntry<Block> blockEntry : Mechano.REGISTRATE.getAll(Registries.BLOCK)) {
                if(!Mechano.REGISTRATE.isInCreativeTab(blockEntry, tab)) continue;
                if(blockEntry.get() instanceof CreativeTabExcludable) continue;
                Item blockItem = blockEntry.get().asItem();
                if(blockItem == Items.AIR) continue;
                out.add(blockItem);
            }
            return out;
        }

        public static List<Item> getItems() {
            List<Item> out = new ReferenceArrayList<>();
            for(RegistryEntry<Item> itemEntry : Mechano.REGISTRATE.getAll(Registries.ITEM)) {
                if(!Mechano.REGISTRATE.isInCreativeTab(itemEntry, tab)) continue;
                if(itemEntry.get() instanceof CreativeTabExcludable) continue;
                Item item = itemEntry.get();
                if(item instanceof BlockItem) continue;
                out.add(item);
            }
            return out;
        }

        private static Function<Item, ItemStack> newStackoWacko() {
			Map<Item, Function<Item, ItemStack>> functionBase = new Reference2ReferenceOpenHashMap<>();

			return item -> {
				Function<Item, ItemStack> functionOut = functionBase.get(item);
				if (functionOut != null) 
					return functionOut.apply(item);
				return new ItemStack(item);
			};
		}

        public static void apply(Output output, List<Item> allItems, Function<Item, ItemStack> stackoWacko) {
            for(Item item : allItems) {
                output.accept(stackoWacko.apply(item));
            }
        }
    }
}
