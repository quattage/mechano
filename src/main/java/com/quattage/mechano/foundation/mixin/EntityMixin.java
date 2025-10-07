

package com.quattage.mechano.foundation.mixin;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorage;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccess;
import com.quattage.mechano.foundation.api.entity.GriddableEntityAttachment;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;

@Mixin(Entity.class)
public abstract class EntityMixin implements CatenaryAccess {

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = {@At(value = "TAIL")}, cancellable = false)
    private void mechano$updateSurrogateOnEntityRemove(Entity.RemovalReason reason, CallbackInfo info) {
        Entity cast = (Entity)(Object)this;
        if(cast.level().isClientSide || cast instanceof AbstractContraptionEntity 
            || reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER) 
                return;
        Griddable<?> points = GriddableEntityAttachment.of(cast, false);
        if(points == null) return;
        points.destroySurrogate();
    }

    @Inject(method = "tick()V", at = {@At(value = "TAIL")}, cancellable = false)
    private void mechano$updateAttachedCatenaries(CallbackInfo info) {
        Entity cast = (Entity)(Object)this;
        LinkDataStorage<?> storage = LinkDataStorage.getUnsided(cast);
        if(storage == null) return;
        storage.forEach(connection -> {
            if(connection == null || !connection.hasPoints()) return;
            connection.tick(cast.level());
        });
        return;
    }


    // TODO rendering code may have to call this method at the framerate of the game, so is the overhead worth it? should this be cached?
    @Override
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        Entity cast = (Entity)(Object)this;
        if(cast.level().isClientSide()) return null;
        LinkDataStorage.Client storage = LinkDataStorage.getAsClient(cast, false);
        if(storage == null) return null;
        return storage.getAll();
    }

    @Override
    public void forEachCatenary(Consumer<GridCatenary> action) {
        Entity cast = (Entity)(Object)this;
        LinkDataStorage.Client storage = LinkDataStorage.getAsClient(cast, false);
        if(storage == null) return;
        storage.forEach(link -> action.accept(link));
    }
}