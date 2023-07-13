package com.quattage.mechano.core.electricity.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.phys.Vec3;

public class WireModelRenderer {

    private static final float SCALE = 1f;
    private static final int LOD = 4;
    private static final int LOD_LIMIT = 512;

    private final Object2ObjectOpenHashMap<BakedModelHashKey, WireModel> modelCache = new Object2ObjectOpenHashMap<>(256);

    public void renderFromCache(VertexConsumer buffer, PoseStack matrix, BakedModelHashKey key, Vector3f origin, 
        int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight) {

        WireModel model = null;

        if(modelCache.containsKey(key)) model = modelCache.get(key);
        else {
            model = buildWireModel(origin);
            modelCache.put(key, model);
        }
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight);
    }

    private WireModel buildWireModel(Vector3f origin) {
        float segmentLength = 1 / LOD;
        int capacity = (int)(2 * new Vec3(origin).lengthSqr());
        WireModel.WireBuilder builder = WireModel.builder(capacity);

        if(Float.isNaN(origin.x()) && Float.isNaN(origin.z())) {
            buildVerticalGeo()
        }
    }

    private void buildFaceVertical(WireModel.WireBuilder builder, Vector3f vec, float angle, WireUV uv) {
        vec.setX(0);
        vec.setZ(0);
        float actualSegmentLength = 1f / LOD;
        float fullWidth = (uv.x1() - uv.x0()) / 16 * SCALE;

        Vector3f normal = new Vector3f((float) Math.cos(Math.toRadians(angle)), 0, (float) Math.sin(Math.toRadians(angle)));
        normal.normalize(fullWidth);

        Vector3f vert00 = new Vector3f(-normal.x() / 2, 0, -normal.z() / 2), 
            vert01 = new Vector3f(vert00.x(), vert00.y(), vert00.z());

        Vector3f vert10 = new Vector3f(-normal.x() / 2, 0, -normal.z() / 2), 
            vert11 = new Vector3f(vert10.x(), vert10.y(), vert10.z());

        float uvv0 = 0, uvv1 = 0;
        boolean lastIter = false;
        for (int segment = 0; segment < LOD_LIMIT; segment++) {
            if (vert00.y() + actualSegmentLength >= v.y()) {
                lastIter = true;
                actualSegmentLength = v.y() - vert00.y();
            }

            vert10.add(0, actualSegmentLength, 0);
            vert11.add(0, actualSegmentLength, 0);

            uvv1 += actualSegmentLength / SCALE;

            builder.addVertex(vert00).withUV(uv.x0() / 16f, uvv0).next();
            builder.addVertex(vert01).withUV(uv.x1() / 16f, uvv0).next();
            builder.addVertex(vert11).withUV(uv.x1() / 16f, uvv1).next();
            builder.addVertex(vert10).withUV(uv.x0() / 16f, uvv1).next();

            if (lastIter) break;

            uvv0 = uvv1;

            vert00.sub(vert10);
            vert01.sub(vert11);
        }
    }

    public static class BakedModelHashKey {
        private final int hash;

        public BakedModelHashKey(Vec3 fromPos, Vec3 toPos) {
            float verticalDistance = (float) (fromPos.y - toPos.y);
            float horizontalDistance = (float)new Vec3(fromPos.x, 0, fromPos.z)
                .distanceTo(new Vec3(toPos.x, 0, toPos.z));
            int hash = Float.floatToIntBits(verticalDistance);
            hash = 31 * hash + Float.floatToIntBits(horizontalDistance);
            this.hash = hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;

            BakedModelHashKey key = (BakedModelHashKey) obj;
            return hash == key.hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
