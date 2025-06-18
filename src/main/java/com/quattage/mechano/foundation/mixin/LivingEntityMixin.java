package com.quattage.mechano.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = {@At(value = "HEAD")}, cancellable = false)
    private void mechano$updateSurrogateOnEntityKill(DamageSource damageSource, CallbackInfo info) {
        AnchorPointable holder = ((Entity)(Object)this).getCapability(MechanoData.ANCHOR_CAPABILITY);
        if(holder == null) return;
        holder.destroySurrogate();
    }
}
