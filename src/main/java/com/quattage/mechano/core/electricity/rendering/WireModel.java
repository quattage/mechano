package com.quattage.mechano.core.electricity.rendering;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.quattage.mechano.Mechano;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;


public record WireModel(float[] vertices, float[] uvs) {

    public static WireBuilder builder(int capacity) {
        return new WireBuilder(capacity);
    }
    
    /***
     * Adds vertices to the buffer 
     * @param buffer VertexConsumer
     * @param matrix PoseStack
     * @param blockLightFrom int blocklight level at the starting BlockPos
     * @param blockLightTo  int blocklight level at the ending BlockPos
     * @param skyLightFrom int skylight level at the starting BlockPos
     * @param skyLightTo int skylight level at the ending BlockPos
     */
    public void render(VertexConsumer buffer, PoseStack matrix, 
        int blockLightFrom, int blockLightTo, int skyLightFrom, int skyLightTo) {

        Matrix4f modelMatrix = matrix.last().pose();
        Matrix3f normalMatrix = matrix.last().normal();
        
        // step through in deliniations of 3 ([x,y,z,x,y,z,x,y,z...] etc)
        int count = vertices.length / 3;

        for (int i = 0; i < count; i++) {
            float iter = (i % (count / 2f)) / (count / 2f);
            int light = lightmapPack(iter, blockLightFrom, blockLightTo, skyLightFrom, skyLightTo);

            buffer
                .vertex(modelMatrix, vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2])
                .color(255, 255, 255, 255)                           // vertex color doesn't matter but is required anyway
                .uv(uvs[i * 2], uvs[i * 2 + 1])                      // texture UVs
                .overlayCoords(OverlayTexture.NO_OVERLAY)            // i have no idea what this is
                .uv2(light)                                          // lightmap UVs
                .normal(normalMatrix, 1, 0.35f, 0)                   // normal (arbitrary numbers)
                .endVertex();                                        // mojang mappings suck balls
        }
    }

    public int lightmapPack(float iter, int blockLightFrom, int blockLightTo, int skyLightFrom, int skyLightTo) {
        return LightTexture.pack(
            (int)Mth.lerp(iter, (float)blockLightFrom, (float)blockLightTo),
            (int)Mth.lerp(iter, (float)skyLightFrom, (float)skyLightTo)
        );
    }

    /***
     * Constructs a WireModel
     */
    public static class WireBuilder {

        private final List<Float> uvs;
        private final List<Float> vertices;
        private int currentSize;

        public WireBuilder(int capacity) {
            uvs = new ArrayList<>(capacity * 2);
            vertices = new ArrayList<>(capacity * 3);
        }

        public WireBuilder withUV(float u, float v) {
            uvs.add(u);
            uvs.add(v);
            return this;
        }

        public WireBuilder addVertex(Vector3f vert) {
            vertices.add(vert.x());
            vertices.add(vert.y());
            vertices.add(vert.z());
            return this;
        }


        public WireModel build() {
            if(uvs.size() != currentSize * 2) throw new IllegalArgumentException("WireModel of size " 
                + currentSize * 2 + " is incompatable with a UV list of size " + uvs.size() + ".");

            if(vertices.size() != currentSize * 3) throw new IllegalArgumentException("WireModel of size " 
                + currentSize * 3 + " is incompatable with a Vertex list of size " + vertices.size() + ".");

            return new WireModel(makeBoxedArray(vertices), makeBoxedArray(uvs));
        }

        private float[] makeBoxedArray(List<Float> array) {
            int i = 0;
            float[] out = new float[array.size()];
            for (float f : array) {
                out[i++] = f;
            }

            return out;
        }

        public void next() {
            currentSize++;
        }
    }
}
