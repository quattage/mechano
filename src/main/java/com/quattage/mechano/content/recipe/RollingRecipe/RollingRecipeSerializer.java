package com.quattage.mechano.content.recipe.RollingRecipe;

import com.google.gson.JsonObject;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.recipe.MechanoRecipeSerializer;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

public class RollingRecipeSerializer extends MechanoRecipeSerializer<RollingRecipe> {

    public RollingRecipeSerializer() {
        
    }

    @Override
    public RollingRecipe read(Identifier id, PacketByteBuf buffer) {
        ItemStack output = buffer.readItemStack();
        Ingredient input = Ingredient.fromPacket(buffer);
        int processTime = buffer.readInt();
        return new RollingRecipe(input, output, processTime, id);
    }

    @Override
    public void write(PacketByteBuf buffer, RollingRecipe recipe) {
        buffer.writeItemStack(recipe.result);
        buffer.writeInt(recipe.processTime);
        recipe.ingredient.write(buffer);
    }

    @Override
    public RollingRecipe readFromJson(Identifier id, JsonObject json) {
        ItemStack result = readOutput(json.get("result"));
        int processTime = 50; //TOOD unfuck
        Ingredient input = Ingredient.fromJson(json.get("input"));
        return new RollingRecipe(input, result, processTime, id);
    }
    
}
