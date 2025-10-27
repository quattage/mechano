package com.quattage.mechano.foundation.block.hitbox;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.numeric.VectorRotationRepresentable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;

/**
 * Wraps a VoxelShape for storage and automatically populates a map
 * with rotated variants of said VoxelShape as they're requested.
 */
@EventBusSubscriber
public final class LazyRotatableHitbox implements HitboxRepresentable {

    // clear all registered orientations when the player logs out to
    // prevent old hitboxes from sitting in memory forever
    private static final ObjectArrayList<LazyRotatableHitbox> all = new ObjectArrayList<>();
    @SubscribeEvent
    public static void clearStale(PlayerLoggedOutEvent evt) {
        for(int x = 0; x < all.size(); x++)
            all.get(x).orientations = null;
    }

    // the unrotated shape at (0, 0, 0), or UP_NORTH
    private final VoxelShape rootShape;
    // map of variants bound to their euler angle rotation
    @Nullable private Object2ObjectOpenHashMap<Vec3i, VoxelShape> orientations;
    
    public LazyRotatableHitbox(VoxelShape rootShape) {
        Objects.requireNonNull(rootShape);
        if(rootShape.isEmpty())
            throw new IllegalArgumentException("Attempted to create a Hitbox from an empty shape!");
        this.rootShape = rootShape.optimize();
        this.orientations = null;
        all.add(this);
    }

    /**
     * Gets the VoxelShape pertaining to the orientation described by the provided tokens.
     * Accepts a BlockState, Vec3i, or {@link VectorRotationRepresentable} instance. 
     * Calls to this method will automatically construct a new {@link VoxelShape}
     * according to the provided input's equivalent {@link com.quattage.mechano.foundation.block.CombinedOrientation orientation}.
     * The rotated VoxelShape result will be cached so that the rotation math doesn't have to
     * be repeated later.
     */
    @Override
    public VoxelShape get(Object... tokens) {
        if(tokens == null || tokens.length == 0) 
            return rootShape;
        if(tokens.length == 1) {
            Object arg = tokens[0];
            if(arg == null) return rootShape;
            if(arg instanceof BlockState state)
                return getAndCache(DirectionTransformer.getAbsoluteRotation(state));
            if(arg instanceof VectorRotationRepresentable vrr)
                return getAndCache(vrr.getRotation());
            if(arg instanceof Vec3i vec) 
                return getAndCache(vec);
            Mechano.LOGGER.error("Couldn't get orientation shape for token '" + arg + "' (" + arg.getClass().getTypeName() + 
                ") - This token is not a valid substitute for an orientation (Expected BlockState, Vec3i, VectorRotationRepresentable) The default shape has been provided as a fallback!");
            return get();
        }
        String report = "";
        for(int x = 1; x < tokens.length; x++ )
            report += tokens[x] + ", ";
        Mechano.LOGGER.warn("Ignored token(s) [" + report + "] while getting hitbox for '" + tokens[0] + "'");
        return get(tokens[0]);
    }

    /**
     * Gets the {@link VoxelShape} rotated face the provided {@link Vec3i} rotation vector.
     * Calls to this method will first refer to an internal cache of
     * rotated VoxelShape variants. If one does not exist, a new one
     * will be created and stored. 
     * @param rotation Euler rotation vector in traditional XYZ order. 
     * Units are in degrees and must be axis-alined multiples of 90.
     */
    public VoxelShape getAndCache(Vec3i rotation) {
        if(rotation.equals(Vec3i.ZERO))
            return get();
        VoxelShape shape = null;
        if(orientations != null) {
            shape = orientations.get(rotation);
            if(shape != null) return shape;
        } else orientations = new Object2ObjectOpenHashMap<>();
        shape = VoxelShapeBuilder.getRotatedCopy(get(), rotation, true);
        orientations.put(rotation, shape);
        return shape;
    }

    /**
     * Gets the {@link VoxelShape} rotated face the provided {@link Vec3i} rotation vector.
     * Differs from {@link #getAndCache} in that this method does not
     * store rotated variants for accelerated access.
     * @param rotation Euler rotation vector in traditional XYZ order. 
     * Units are in degrees and must be axis-alined multiples of 90.
     */
    public VoxelShape get(Vec3i rotation) {
        if(rotation.equals(Vec3i.ZERO))
            return get();
        return VoxelShapeBuilder.getRotatedCopy(get(), rotation, false);
    }

    @Override
    public VoxelShape get() {
        return rootShape;
    }

    @Override
    public VoxelShape get(BlockState state, BlockGetter block, BlockPos pos, CollisionContext collisionContext) {
        return get(state);
    }
}
