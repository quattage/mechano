package com.quattage.mechano.foundation.catenary.meshing;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryGeometry.MutableExtruder;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryGeometry.Stick;

@FunctionalInterface
public interface ProfileExtrusion {


    /**
     * Creates 4 (or more) quads to build a mesh defined by a float array containing 8 shared 
     * corner vertices. 
     * @param buffer The buffer to emit vertices to
     * @param pose The pose used to apply root transformations
     * @param verts An array of 24 floats represent the positions of all 8 corners, organized as (x, y, z... x, y, z... etc)
     * @param light The packed light at the start and end of the extrusion
     * @param matrix An operational matrix of 5 members, defining the extrusion's local frame and its normals (right, up, forward, normalU, normalV)
     */
    public void extrude(VertexConsumer buffer, Pose pose, float[] verts, int[] light, Vector3f[] matrix);

    /**
     * Creates a profile as defined by this ProfileExtrusion's configured winding 
     * order by placing 8 model-space vertices at the ends of a given {@link Stick}. 
     * These vertices are then wound into an arbitrary number of quads as defined 
     * by the {@link #extrude extruder}, and these quads are pushed to the given 
     * <code>buffer</code>. The stick, called <code>current</code>, has 4 vertices 
     * placed at each of its endpoints. The offset positions of these vertices are 
     * averaged across the adjacent sticks, <code>previous</code> and 
     * <code>next</code>, so that adjacent profiles line up exactly at their ends. 
     * Alternatively, endpoint averaging can be ignored by simply passing 
     * <code>null</code> in place of either adjacent stick.
     * @param previous (Optional, can be null) The previous stick in the chain
     * @param current (Required) The stick to create a profile of
     * @param next (Optional, can be null) The next stick in the chain
     * @param light (Optional, can be null) Two packed light values, where the first 
     * integer is the light at the beginning of <code>current</code>, and the second 
     * integer is the light at the end of <code>current</code>. This packed light value
     * is traditionally obtained with {@link net.minecraft.client.renderer.LightTexture#pack LightTexture.pack} and/or a {@link net.minecraft.world.level.BlockAndTintGetter BlockAndTintGetter}
     * @param verts (Optional, can be null) Recyclable float array representing the local
     * position of each displaced vertex. The contents of this array are computed for you as
     * a result of this call, so they'll be written over, but you may provide it here to 
     * prevent continuous re-allocation of 24 single-precision floats.
     * @param matrix (Optional, can be null) An array of 5 {@link Vector3f mutable vectors} 
     * that are computed as a result of this call. Just like the <code>verts</code> array, 
     * this method can re-use these vectors in-place to prevent memory re-allocation.
     * @param computeFreshNormal <code>true</code> if the normal vector should be recomputed
     * as a result of this call, or <code>false</code> if the supplied <code>matrix</code>
     * already contains normal information to use that shouldn't be written over.
     * @param buffer VertexConsumer to push vertices to
     * @param pose Pose whose matrix serves as the basis to apply position and normal transformations
     * @param width The width of the resulting extrusion. Most implementations will divide this by two to obtain a radius.
     */
    default void make(@Nullable Stick previous, Stick current, @Nullable Stick next, @Nullable int[] light, @Nullable float[] verts, @Nullable Vector3f[] matrix, MutableExtruder attributes, VertexConsumer buffer, Pose pose, float width, boolean computeFreshNormal) {

        // sanity checks
        boolean needsNormal = false;
        if(matrix == null || matrix.length != 5) {
            matrix = new Vector3f[] { new Vector3f(),  new Vector3f(),  new Vector3f(),  new Vector3f(),  new Vector3f(), };
            needsNormal = true;
        }
        if(verts == null || verts.length != 24)
            verts = new float[24];
        if(light == null || light.length != 2) 
            light = new int[] { LightTexture.FULL_BLOCK, LightTexture.FULL_SKY };

        // compute matrix and normal as needed
        matrix[1].set(CatenaryAttributes.UP);
        if(previous == null) matrix[2].set(current.getForward());
        else matrix[2].set(previous.getForward()).add(current.getForward()).normalize();
        matrix[0].set(matrix[1]).cross(matrix[2]).normalize();
        matrix[1].set(matrix[2]).cross(matrix[0]).normalize();
        if(needsNormal || computeFreshNormal) {
            matrix[3].set(matrix[0]).add(matrix[1]).normalize();
            matrix[4].set(matrix[0]).sub(matrix[1]).normalize();
        }

        float radius = width / 2f;

        // create 4 vertices at the start of the profile
        createVertices(verts, matrix, current.start.pos, radius, false);
        // move to the end of the profiel
        if(next != null) {
            matrix[2].set(current.getForward()).add(next.getForward()).normalize();
            matrix[0].set(matrix[1]).cross(matrix[2]).normalize();
            matrix[1].set(matrix[2]).cross(matrix[0]).normalize();
        }
        // create 4 vertices at the end of the profile
        createVertices(verts, matrix, current.end.pos, radius, true);

        // emit 4 quads to create the profile segment
        extrude(buffer, pose, radius, radius, verts, light, matrix);
    }

    default void createVertices(float[] verts, Vector3f[] matrix, Vector3f pos, float radius, boolean end) {
        int x = end ? 12 : 0;
        verts[x]      = pos.x + (matrix[0].x * radius);
        verts[x + 1]  = pos.y + (matrix[0].y * radius);
        verts[x + 2]  = pos.z + (matrix[0].z * radius);
        verts[x + 3]  = pos.x + (matrix[1].x * radius);
        verts[x + 4]  = pos.y + (matrix[1].y * radius);
        verts[x + 5]  = pos.z + (matrix[1].z * radius);
        verts[x + 6]  = pos.x - (matrix[0].x * radius);
        verts[x + 7]  = pos.y - (matrix[0].y * radius);
        verts[x + 8]  = pos.z - (matrix[0].z * radius);
        verts[x + 9]  = pos.x - (matrix[1].x * radius);
        verts[x + 10] = pos.y - (matrix[1].y * radius);
        verts[x + 11] = pos.z - (matrix[1].z * radius);
    }

    default void emitQuad(VertexConsumer buffer, Pose pose, int[] light, float[] verts, Vector3f norm, int a, int b, int c, int d, int u, int v, int mU, int mV) {
        buffer.addVertex(pose, verts[(a * 3)], verts[(a * 3) + 1], verts[(a * 3) + 2])
            .setColor(255, 255, 255, 255)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[0])
            .setNormal(pose, norm.x, norm.y, norm.z);
        buffer.addVertex(pose, verts[(b * 3)], verts[(b * 3) + 1], verts[(b * 3) + 2])
            .setColor(255, 255, 255, 255)
            .setUv(u, mV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[1])
            .setNormal(pose, norm.x, norm.y, norm.z);
        buffer.addVertex(pose, verts[(c * 3)], verts[(c * 3) + 1], verts[(c * 3) + 2])
            .setColor(255, 255, 255, 255)
            .setUv(mU, mV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[1])
            .setNormal(pose, norm.x, norm.y, norm.z);
        buffer.addVertex(pose, verts[(d * 3)], verts[(d * 3) + 1], verts[(d * 3) + 2])
            .setColor(255, 255, 255, 255)
            .setUv(mU, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light[0])
            .setNormal(pose, norm.x, norm.y, norm.z);
    }
}
