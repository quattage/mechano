package com.quattage.mechano.infrastructure.hitbox;

import java.util.Map;
import java.util.Objects;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.helper.VoxelShapeBuilder;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HitboxStateTree implements HitboxRepresentable {


    // 2 members
    public HitboxStateTree(
        String al1, HitboxRepresentable hr1, 
        String al2, HitboxRepresentable hr2
    ) {
        stateTree.put(al1, hr1);
        stateTree.put(al2, hr2);
    }


    // 3 members
    public HitboxStateTree(
        String al1, HitboxRepresentable hr1, 
        String al2, HitboxRepresentable hr2, 
        String al3, HitboxRepresentable hr3
    ) {
        stateTree.put(al1, hr1);
        stateTree.put(al2, hr2);
        stateTree.put(al3, hr3);
    }


    //4 members
    public HitboxStateTree(
        String al1, HitboxRepresentable hr1, 
        String al2, HitboxRepresentable hr2, 
        String al3, HitboxRepresentable hr3, 
        String al4, HitboxRepresentable hr4
    ) {
        stateTree.put(al1, hr1);
        stateTree.put(al2, hr2);
        stateTree.put(al3, hr3);
        stateTree.put(al4, hr4);
    }


    // 5 members
    public HitboxStateTree(
        String al1, HitboxRepresentable hr1, 
        String al2, HitboxRepresentable hr2, 
        String al3, HitboxRepresentable hr3, 
        String al4, HitboxRepresentable hr4,
        String al5, HitboxRepresentable hr5
    ) {
        stateTree.put(al1, hr1);
        stateTree.put(al2, hr2);
        stateTree.put(al3, hr3);
        stateTree.put(al4, hr4);
        stateTree.put(al5, hr5);
    }


    // 6 members
    public HitboxStateTree(
        String al1, HitboxRepresentable hr1, 
        String al2, HitboxRepresentable hr2, 
        String al3, HitboxRepresentable hr3, 
        String al4, HitboxRepresentable hr4,
        String al5, HitboxRepresentable hr5,
        String al6, HitboxRepresentable hr6
    ) {
        stateTree.put(al1, hr1);
        stateTree.put(al2, hr2);
        stateTree.put(al3, hr3);
        stateTree.put(al4, hr4);
        stateTree.put(al5, hr5);
        stateTree.put(al6, hr6);
    }

    // 7 members
    public HitboxStateTree(
        String al1, HitboxRepresentable hr1, 
        String al2, HitboxRepresentable hr2, 
        String al3, HitboxRepresentable hr3, 
        String al4, HitboxRepresentable hr4,
        String al5, HitboxRepresentable hr5,
        String al6, HitboxRepresentable hr6,
        String al7, HitboxRepresentable hr7
    ) {
        stateTree.put(al1, hr1);
        stateTree.put(al2, hr2);
        stateTree.put(al3, hr3);
        stateTree.put(al4, hr4);
        stateTree.put(al5, hr5);
        stateTree.put(al6, hr6);
        stateTree.put(al7, hr7);
    }


    // 8 members
    public HitboxStateTree(
        String al1, HitboxRepresentable hr1, 
        String al2, HitboxRepresentable hr2, 
        String al3, HitboxRepresentable hr3, 
        String al4, HitboxRepresentable hr4,
        String al5, HitboxRepresentable hr5,
        String al6, HitboxRepresentable hr6,
        String al7, HitboxRepresentable hr7,
        String al8, HitboxRepresentable hr8
    ) {
        stateTree.put(al1, hr1);
        stateTree.put(al2, hr2);
        stateTree.put(al3, hr3);
        stateTree.put(al4, hr4);
        stateTree.put(al5, hr5);
        stateTree.put(al6, hr6);
        stateTree.put(al7, hr7);
        stateTree.put(al8, hr8);
    }








    // the actual implementation starts here
    private final Map<String, HitboxRepresentable> stateTree = new Object2ObjectOpenHashMap<>();

    @Override
    public VoxelShape get(Object... tokens) {
        Objects.requireNonNull(tokens);
        if(tokens.length == 0) {
            Mechano.LOGGER.error("Couldn't acquire tokenized VoxelShape from an empty key!");
            return VoxelShapeBuilder.CUBE;
        }
        if(tokens.length < 2) {
            Mechano.LOGGER.error("Couldn't acquire tokenized VoxelShape from single token '" + tokens + "'");
            return VoxelShapeBuilder.CUBE;
        }

        Object arg = tokens[0];
        HitboxRepresentable acquired = stateTree.get(tokens[0]);
        if(arg instanceof String str) 
            acquired = stateTree.get(str);
        else if(arg instanceof StringRepresentable sr)
            acquired = stateTree.get(sr.getSerializedName());
        else if(arg instanceof EnumProperty prop)
            acquired = stateTree.get(prop.toString());
        

        if(acquired != null) return acquired.get(minusFirst(tokens));

        Mechano.LOGGER.error("Couldn't acquire VoxelShape from unknown token '" + tokens[0] + "' (" + tokens[0].getClass().getTypeName() + ")");
        return VoxelShapeBuilder.CUBE;
    }

    @Override
    public VoxelShape get() {
        Mechano.LOGGER.error("Couldn't acquire VoxelShape - At least one token must be provied to traverse this state tree!");
        return VoxelShapeBuilder.CUBE;
    }
}
