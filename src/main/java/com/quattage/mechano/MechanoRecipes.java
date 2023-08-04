package com.quattage.mechano;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

// This is where RecipeTypes and RecipeSerializers go to be registered.
public class MechanoRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Mechano.MOD_ID);


    public static void register(IEventBus event) {
        TYPES.register(event);
        Mechano.logReg("recipes");
    }
}
