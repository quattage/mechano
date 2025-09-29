package com.quattage.mechano.foundation.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity.MovingGriddableAccessor;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.world.entity.Entity;

@Mixin(AbstractContraptionEntity.class)
public class AbstractContraptionEntityMixin implements CatenaryAccessor {

    @Unique
    public @Nullable ObjectSet<GridCatenary> mechano$Catenaries = new ObjectOpenHashSet<GridCatenary>(2);
    @Shadow protected Contraption contraption;

    @Inject(
        method = "disassemble", 
        at = @At(value = "TAIL"), 
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = false, remap = false
    )
    private void mechano$transferDataOnEntityRemoved(CallbackInfo info, StructureTransform transform) {
        AbstractContraptionEntity cast = (AbstractContraptionEntity)(Object)this;
        if(contraption == null || cast.level().isClientSide) return;
        ((MovingGriddableAccessor)contraption).invokeDisassemble(cast, transform);
        ((MovingGriddableAccessor)contraption).getTransientGriddables().clear();
    }

    @Inject(method = "tickActors", at = @At(value = "HEAD"), cancellable = false, remap = false)
    private void mechano$tickCatenaries(CallbackInfo info) {
        Entity cast = (Entity)(Object)this;
        if(!cast.level().isClientSide) return;
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

    @Override
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        return mechano$Catenaries;
    }
}
