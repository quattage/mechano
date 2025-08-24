package com.quattage.mechano.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.entity.GriddableEntityAttachment;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/**
 * Destroys the virtualized node belonging to an Entity when said Entity is removed.
 * I tried to use the {@link EntityLeaveLevelEvent} for this purpose but it wouldn't work
 * because I need to do this before the entity is removed, not after
 */
@Mixin(Entity.class)
public abstract class EntityMixin  {
    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = {@At(value = "TAIL")}, cancellable = false)
    private void mechano$updateSurrogateOnEntityRemove(Entity.RemovalReason reason, CallbackInfo info) {
        if(RemovalReason.UNLOADED_TO_CHUNK.equals(reason)) return;
        if(RemovalReason.UNLOADED_WITH_PLAYER.equals(reason)) return;
        Griddable<?> points = GriddableEntityAttachment.of(((Entity)(Object)this), false);
        if(points == null) return;
        points.destroySurrogate();
    }
}