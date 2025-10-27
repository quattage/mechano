package com.quattage.mechano.foundation;

import java.util.ArrayList;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.item.SpoolItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.ItemBuilder;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class MechanoItemProperties {
    public static final ResourceLocation FULLNESS = Mechano.asResource("full");
    public static ArrayList<ItemBuilder<SpoolItem<?>, CreateRegistrate>> spools = new ArrayList<>();
    @SuppressWarnings("deprecation")
    public static class SpoolFullnessProperty implements ItemPropertyFunction {
        @Override
        public float call(ItemStack stack, ClientLevel level, LivingEntity entity, int seed) {
            float out = 1f - ((float)stack.getDamageValue() / stack.getOrDefault(DataComponents.MAX_DAMAGE, 512));
            return out;
        }
    }
}
