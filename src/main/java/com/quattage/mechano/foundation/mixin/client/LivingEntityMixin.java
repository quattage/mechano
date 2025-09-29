package com.quattage.mechano.foundation.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridLink;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements CatenaryAccessor {

    @Unique
    public @Nullable ObjectSet<GridCatenary> mechano$Catenaries = new ObjectOpenHashSet<GridCatenary>(2);

    @Override
    @OnlyIn(Dist.CLIENT)
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        return mechano$Catenaries;
    }

    @Inject(method = "tick()V", at = {@At(value = "TAIL")}, cancellable = false)
    private void mechano$updateAttachedCatenaries(CallbackInfo info) {
        Entity cast = (Entity)(Object)this;
        if(!cast.level().isClientSide) {
            LinkDataStorable.Server storage = LinkDataStorable.getAsServer(cast, false);
            if(storage == null) return;
            for(GridLink link : storage.getAll())
                link.tick(cast.level());
            return;
        }
        LinkDataStorable.Client storage = LinkDataStorable.getAsClient(cast, false);
        if(storage == null) {
            if(mechano$Catenaries instanceof ObjectOpenHashSet<GridCatenary> cats) {
                this.mechano$Catenaries.clear();
                cats.trim(2);
            }
            return;
        }
        this.mechano$Catenaries = storage.getAll();
        for(GridCatenary cat : this.mechano$Catenaries) {
            if(!cat.hasPoints()) continue;
            cat.tick(cast.level());
        }
    }
}