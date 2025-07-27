package com.quattage.mechano.foundation.catenary;

import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface Tensionable {
    @OnlyIn(Dist.CLIENT)
    public abstract float getSpan();
    @OnlyIn(Dist.CLIENT)
    public abstract float getMaximumSpan();
    @OnlyIn(Dist.CLIENT)
    public abstract void adjustSpan(LevelReader world, float length);
}
