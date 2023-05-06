package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MechanoRecipes {
    public static void register() {
        Mechano.log("Registering recipe types");
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(Mechano.newResource("big_rolling"), new RollingRecipeSerializer());
        Registry.register(Registry.RECIPE_TYPE, new Identifier(Mechano.newResource("big_rolling"), RollingRecipe.TYPE);
    }
}
