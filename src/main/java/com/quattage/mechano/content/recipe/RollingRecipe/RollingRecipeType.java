package com.quattage.mechano.content.recipe.RollingRecipe;

import com.quattage.mechano.Mechano;

import net.minecraft.recipe.RecipeType;

public class RollingRecipeType implements RecipeType<RollingRecipe> {

	@Override
	public String toString() {
		return Mechano.MOD_ID+":rolling";
	}
}
