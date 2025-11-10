package com.quattage.mechano.api.catenary;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface CatenaryAccess {
    
    @OnlyIn(Dist.CLIENT)
    @Nullable ObjectSet<GridCatenary> getCatenaries();
    
    @OnlyIn(Dist.CLIENT)
    default void forEachCatenary(Consumer<GridCatenary> action) {
        ObjectSet<GridCatenary> cats = getCatenaries();
        if(cats == null || cats.isEmpty()) return;
        for(GridCatenary cat : cats) {
            if(cat == null) continue;
            if(!cat.hasPoints()) continue;
            action.accept(cat);
        }
    }

    static Vec3 getLocalizedOffset(LevelReader world, BlockPos sectionCenter, AnchorPoint anchor) {
        Vec3 realPos = anchor.getPos(world);
        return new Vec3(
            realPos.x - sectionCenter.getX(),
            realPos.y - sectionCenter.getY(),
            realPos.z - sectionCenter.getZ()
        );
    }

    default String getCatenariesAsString() {
        String out = "Catenaries[";
        ObjectSet<GridCatenary> cats = getCatenaries();
        if(cats == null || cats.isEmpty()) {
            return out += "EMPTY]";
        }
        for(GridCatenary cat : cats) 
            out += cat.toString() + ", ";
        return out.substring(0, out.length() - 2) + "]";
    }
}
