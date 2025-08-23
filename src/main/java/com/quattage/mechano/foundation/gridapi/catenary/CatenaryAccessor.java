package com.quattage.mechano.foundation.gridapi.catenary;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.landmark.GridCatenary;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface CatenaryAccessor {
    
    @OnlyIn(Dist.CLIENT)
    public abstract @Nullable ObjectSet<GridCatenary> getCatenaries();
    
    @OnlyIn(Dist.CLIENT)
    public default void forEachCatenary(Consumer<GridCatenary> action) {
        ObjectSet<GridCatenary> cats = getCatenaries();
        if(cats == null || cats.isEmpty()) return;
        for(GridCatenary cat : cats) {
            if(cat == null) continue;
            if(!cat.hasPoints()) continue;
            action.accept(cat);
        }
    }

    public static Vec3 getLocalizedOffset(LivingEntity e, float pTicks) {
        return e.getRopeHoldPosition(pTicks).subtract(e.getPosition(pTicks));
    }

    public static Vec3 getLocalizedOffset(LevelReader world, BlockPos sectionCenter, AnchorPoint anchor) {
        Vec3 realPos = anchor.getPos(world);
        return new Vec3(
            realPos.x - sectionCenter.getX(),
            realPos.y - sectionCenter.getY(),
            realPos.z - sectionCenter.getZ()
        );
    }

    public default String getCatenariesAsString() {
        String out = "Catenaries[";
        ObjectSet<GridCatenary> cats = getCatenaries();
        if(cats == null || cats.isEmpty()) {
            return out += "EMPTY]";
        }
        for(GridCatenary cat : cats) 
            out += cat.toString() + ", ";
        return out.substring(0, out.length() - 2) + "]";
    }

    public static Vec3 getLocalizedOffset(BlockEntity be) {
        return be.getBlockPos().getCenter();
    }
}
