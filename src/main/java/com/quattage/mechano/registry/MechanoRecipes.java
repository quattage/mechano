package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.recipe.RollingRecipe.RollingRecipe;
import com.quattage.mechano.content.recipe.RollingRecipe.RollingRecipeSerializer;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class MechanoRecipes {
    public static void register() {
        Mechano.log("Registering recipe types");
        Registry.register(Registry.RECIPE_SERIALIZER, new Identifier(Mechano.MOD_ID, "rolling"), new RollingRecipeSerializer());
        Registry.register(Registry.RECIPE_TYPE, new Identifier(Mechano.MOD_ID, "rolling"), RollingRecipe.RECIPE_TYPE);
    }
}
