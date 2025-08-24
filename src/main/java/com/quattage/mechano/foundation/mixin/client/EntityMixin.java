package com.quattage.mechano.foundation.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Mixin(Entity.class)
public abstract class EntityMixin implements CatenaryAccessor {

    @Override
    @OnlyIn(Dist.CLIENT)
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        LinkDataStorable.Client storage = LinkDataStorable.getAsClient((Entity)(Object)this, false);
        return storage == null ? null : storage.getAll();
    }
}
