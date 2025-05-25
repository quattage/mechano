package com.quattage.mechano.infrastructure.hitbox;

import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.helper.VectorRotationRepresentable;
import com.quattage.mechano.foundation.helper.VoxelShapeBuilder;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class LazyRotatableHitbox implements HitboxRepresentable {

    private final VoxelShape rootShape;
    @Nullable private Map<Vec3i, VoxelShape> orientations;
    
    public LazyRotatableHitbox(VoxelShape rootShape) {
        Objects.requireNonNull(rootShape);
        if(rootShape.isEmpty())
            throw new IllegalArgumentException("Attempted to create a Hitbox from an empty shape!");
        this.rootShape = rootShape.optimize();
        this.orientations = null;
    }

    @Override
    public VoxelShape get(Object... tokens) {

        if(tokens == null || tokens.length == 0) 
            return rootShape;

        if(tokens.length == 1) {
            Object arg = tokens[0];
            if(arg == null) return rootShape;
            if(arg instanceof BlockState state)
                return getOrCreate(DirectionTransformer.getRotation(state));
            if(arg instanceof VectorRotationRepresentable vrr)
                return getOrCreate(vrr.getRotation());
            if(arg instanceof Vec3i vec) 
                return getOrCreate(vec);
            Mechano.LOGGER.error("Couldn't get orientation shape for token '" + arg + "' (" + arg.getClass().getTypeName() + 
                ") - This token is not a valid substitute for an orientation (Expected BlockState, Vec3i, VectorRotationRepresentable) The default shape has been provided as a fallback!");
            return rootShape;
        }

        String report = "";
        for(int x = 1; x < tokens.length; x++ )
            report += tokens[x] + ", ";
        Mechano.LOGGER.warn("Ignored token(s) [" + report + "] while getting hitbox for '" + tokens[0] + "'");
        return get(tokens[0]);
    }
    

    private VoxelShape getOrCreate(Vec3i rotation) {
        if(rotation.equals(Vec3i.ZERO))
            return rootShape;
        VoxelShape shape = orientations.get(rotation);
        if(shape != null) return shape;

        shape = VoxelShapeBuilder.getRotatedCopy(shape, rotation);
        orientations.put(rotation, shape);
        return shape;
    }

    @Override
    public VoxelShape get() {
        return rootShape;
    }
}
