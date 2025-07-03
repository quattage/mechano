package com.quattage.mechano.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.entity.GriddableEntityAttachment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;

@Mixin(Entity.class)
public abstract class EntityMixin  {

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = {@At(value = "TAIL")}, cancellable = false)
    private void mechano$updateSurrogateOnEntityRemove(Entity.RemovalReason reason, CallbackInfo info) {
        if(RemovalReason.UNLOADED_TO_CHUNK.equals(reason)) return;
        if(RemovalReason.UNLOADED_WITH_PLAYER.equals(reason)) return;
        AnchorPointable<?> points = GriddableEntityAttachment.of(((Entity)(Object)this), false);
        if(points == null) return;
        points.destroySurrogate();
    }
}
