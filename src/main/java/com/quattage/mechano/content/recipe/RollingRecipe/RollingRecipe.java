package com.quattage.mechano.content.recipe.RollingRecipe;

import com.quattage.mechano.Mechano;

import io.github.fabricators_of_create.porting_lib.transfer.item.RecipeWrapper;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

@SuppressWarnings("deprecation")
public class RollingRecipe implements Recipe<Inventory> {

    protected Identifier id;
	protected Ingredient ingredient;
	protected ItemStack result;
    protected int processTime;

    public static final RecipeType<RollingRecipe> RECIPE_TYPE = new RollingRecipeType();
	public static final RecipeSerializer<?> SERIALIZER = Registry.RECIPE_SERIALIZER.get(new Identifier(Mechano.MOD_ID, "rolling"));

    protected RollingRecipe(Ingredient ingredient, ItemStack result, int processTime, Identifier id) {
		this.id = id;
        this.ingredient = ingredient;
        this.result = result;
        this.processTime = processTime;
	}

    public ItemStack getResult() {
        return this.result;
    }

    public Ingredient getIngredient() {
        return this.ingredient;
    }

    public int getProcessTime() {
        return this.processTime;
    }

    @Override
    public boolean matches(Inventory inv, World world) {
        if(inv.isEmpty())
            return false;
        return ingredient.test(inv.getStack(0));
    }

    @Override
    public ItemStack craft(Inventory var1) {
        return this.result;
    }

    @Override
    public boolean fits(int x, int y) {
        return x * y > 0;
    }

    @Override
    public ItemStack getOutput() {
        return this.result;
    }

    @Override
    public Identifier getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RECIPE_TYPE;
    }

    @Override
    public ItemStack createIcon() {
        return this.result;
    }
    

    @Override
    public boolean isIgnoredInRecipeBook() {
        return true;
    }
}
