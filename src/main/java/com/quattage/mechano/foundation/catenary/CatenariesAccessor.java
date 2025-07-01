package com.quattage.mechano.foundation.catenary;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.foundation.api.landmark.GridCatenary;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface CatenariesAccessor {
    @OnlyIn(Dist.CLIENT)
    public abstract @NotNull ObjectSet<GridCatenary> getCatenaries();
}
