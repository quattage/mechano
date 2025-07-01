package com.quattage.mechano.foundation.mixin.client;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.quattage.mechano.foundation.api.SidedGridDispatcher.LinkData;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.catenary.CatenariesAccessor;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements CatenariesAccessor {

    @Unique
    public @Nullable ObjectOpenHashSet<GridCatenary> mechano$Catenaries = new ObjectOpenHashSet<GridCatenary>(2);

    @Override
    @OnlyIn(Dist.CLIENT)
    public @NotNull ObjectSet<GridCatenary> getCatenaries() {
        return mechano$Catenaries;
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "tick()V", at = {@At(value = "TAIL")}, cancellable = false)
    private void mechano$updateAttachedCatenaries(CallbackInfo info) {
        Entity cast = (Entity)(Object)this;
        if(!cast.level().isClientSide) return;
        LinkData data = LinkData.getFrom(cast);
        if(data == null) {
            this.mechano$Catenaries.clear();
            this.mechano$Catenaries.trim(2);
            return;
        }
        this.mechano$Catenaries = (ObjectOpenHashSet<GridCatenary>)(Object)data.get();
        for(GridCatenary cat : this.mechano$Catenaries) {
            cat.updateShape(cast.level(), 1);
            cat.updateKinematics(cast.level());
        }
    }
}
