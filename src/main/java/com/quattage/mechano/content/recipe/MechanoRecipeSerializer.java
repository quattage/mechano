package com.quattage.mechano.content.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.Identifier;

public abstract class MechanoRecipeSerializer<R extends Recipe<?>> implements RecipeSerializer<R> {

    @Override
    public R read(Identifier id, JsonObject json) {
        if(ResourceConditions.objectMatchesConditions(json))
			return readFromJson(id, json);
		return readFromJson(id, json);
    }

    protected ItemStack readOutput(JsonElement outputObject) {
		if(outputObject.isJsonObject() && outputObject.getAsJsonObject().has("item"))
			return ShapedRecipe.outputFromJson(outputObject.getAsJsonObject());
		return null;
	}
    
    public abstract R readFromJson(Identifier id, JsonObject json);
}
