package com.quattage.mechano.catenary;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.catenary.Catenaries.Stick;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Allows API users to define unique meshing behaviour
 * for catenaries. Instances should provide
 * a unique implementation of the {@link #extrude} method.
 */
@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface MeshExtruder {
    /**
     * Defines a method for lofting a profile across a given 
     * {@link Stick}. Implementations can define unique topology,
     * lighting, and normal behaviours for the resulting geometry.
     * The stick, called <code>current</code>, has 4 or more 
     * vertices placed at each of its endpoints. The offset 
     * positions of these vertices are averaged across the 
     * adjacent sticks, <code>previous</code> and 
     * <code>next</code>, so that adjacent profiles line up 
     * exactly at their ends. Alternatively, endpoint averaging 
     * can be ignored by simply passing  <code>null</code> in 
     * place of either adjacent stick.
     * @param buffer VertexConsumer to push geometry to
     * @param pose Pose to use for transforming
     * @param geo {@link CatenaryMeshBuffer} to store and process vertex data. You don't actually have to use this if you don't want to - it's a helper that aids in emiting quads based on stick dimensions quickly.
     * @param previous (Optional, can be null) The previous stick in the chain
     * @param current (Required) The stick to create a profile of
     * @param next (Optional, can be null) The next stick in the chain
     * @param loftLength An arbitrary float representing the total arclength covered during successive calls to this method. 
     * Useful for when multiple extrusions are created in one mesh and need to distinguish between one another or for 
     * panning UVs across an atlas.
     * @param recomputeNormals <code>true</code> if new face normals should be computed here. If <code>false</code>,
     * the normals contained in <code>geo</code> will not be recomputed, but reused. 
     * @param pTicks Partial ticks (accessible in most rendering contexts) for lerping from a fixed update cycle.
     */
    void extrude(VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo, @Nullable Stick previous, Stick current, @Nullable Stick next, float loftLength, boolean recomputeNormals, float pTicks);
}
