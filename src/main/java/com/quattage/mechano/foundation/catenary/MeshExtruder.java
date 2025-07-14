package com.quattage.mechano.foundation.catenary;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.foundation.catenary.CatenaryMesher.Stick;

@FunctionalInterface
public interface MeshExtruder {
    /**
     * Defines a method for lofting a profile across a  given 
     * {@link Stick}. Implementations can define unique geometry,
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
     * @param geo {@link CatenaryMesher} to store and process vertex data
     * @param previous (Optional, can be null) The previous stick in the chain
     * @param current (Required) The stick to create a profile of
     * @param next (Optional, can be null) The next stick in the chain
     * @param iteration An arbitrary integer for when this method is being called as part of a larger
     * iterative process, where multiple extrusions are created in one mesh and need to distinguish
     * between one another. This is particularly useful for panning UVs across an atlas.
     * @param faceNormals <code>true</code> if new face normals should be computed here. If <code>false</code>,
     * the normals contained in <code>geo</code> will not be recomputed, but reused. 
     * @param pTicks partial ticks
     */
    void make(VertexConsumer buffer, Pose pose, CatenaryMesher geo, @Nullable Stick previous, Stick current, @Nullable Stick next, int iteration, boolean faceNormals, float pTicks);
}
