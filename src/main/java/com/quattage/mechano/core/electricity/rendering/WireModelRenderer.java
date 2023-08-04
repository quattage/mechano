

package com.quattage.mechano.core.electricity.rendering;

import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.core.util.VectorHelper;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Color;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/***
 * Populates a given PoseStack with a dynamically generated WireModel
 */
public class WireModelRenderer {

    /***
     * Overall scale, or "thickness" of the wire
     */
    private static final float SCALE = 1f;

    /***
     * The rotation (in degrees) of the wire's profile
     */
    private static final int SKEW = 90;

    /***
     * How much the wire hangs
     */
    private static final float SAGGINESS = 3f;

    /***
     * The wire's Level of Detail
     */
    private static final float LOD = 3f;

    /***
     * The maximum amount of iterations for a single wire. Used
     * to prevent lag or stack overflows in extreme edge cases 
     */
    private static final int LOD_LIMIT = 512;

    private Vec3 fromPos = new Vec3(0, 0, 0);

    /***
     * Represents a hash, used as an identifier for a WireModel's place in the cache.
     */
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

        public String toString() {
            return "[" + hash + "]";
        }
    }

    private final Object2ObjectOpenHashMap<BakedModelHashKey, WireModel> modelCache = new Object2ObjectOpenHashMap<>(256);

    /***
     * Renders a WireModel from the cache, or builds a new one if it does not exist.
     * @param buffer
     * @param matrix
     * @param key
     * @param origin
     * @param fromBlockLight
     * @param toBlockLight
     * @param fromSkyLight
     * @param toSkyLight
     */
    public void renderFromCache(VertexConsumer buffer, PoseStack matrix, BakedModelHashKey key, Vector3f origin, 
        int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight) {

        WireModel model;
        if(modelCache.containsKey(key)) 
            model = modelCache.get(key);
        else {
            model = buildWireModel(origin);
            modelCache.put(key, model);
        }
        
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight);
    }

    /***
     * Renders a WireModel while ignoring the cache.
     * @param buffer
     * @param matrices
     * @param chainVec
     * @param blockLight0
     * @param blockLight1
     * @param skyLight0
     * @param skyLight1
     */
    public void renderFrequent(VertexConsumer buffer, PoseStack matrix, Vector3f origin, 
        int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight) {
            
        WireModel model = buildWireModel(origin);
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight);
    }

    /***
     * Renders a WireModel with a step value and partial ticks, used to drive pre-defined animations.
     * In this case, A wiggly wire will jiggle (and wiggle) until you giggle.
     * @param buffer
     * @param matrices
     * @param chainVec
     * @param age
     * @param blockLight0
     * @param blockLight1
     * @param skyLight0
     * @param skyLight1
     */
    public void renderWiggly(VertexConsumer buffer, PoseStack matrix, Vector3f origin, 
        int age, float pTicks, int fromBlockLight, int toBlockLight, int fromSkyLight, int toSkyLight) {
        
        WireModel model = buildWireModel(age, pTicks, origin);
        model.render(buffer, matrix, fromBlockLight, toBlockLight, fromSkyLight, toSkyLight);
    }

    public void setFrom(Vec3 fromPos) {
        this.fromPos = fromPos;
    }

    /***
     * Adds vertices to a WireModel based on the given Origin vector
     * @param origin
     * @param age - Optional, to drive animations
     * @param pTicks - Optional, to lerp animations
     * @return
     */
    private WireModel buildWireModel(Vector3f origin) {
        return buildWireModel(-1, 0, origin);
    }

    /***
     * Adds vertices to a WireModel based on the given Origin vector
     * @param origin
     * @param age - Optional, to drive animations
     * @param pTicks - Optional, to lerp animations
     * @return
     */
    private WireModel buildWireModel(int age, float pTicks, Vector3f origin) {
        int capacity = (int)(2 * new Vec3(origin).lengthSqr());
        WireModel.WireBuilder builder = WireModel.builder(capacity);

        float dXZ = (float)Math.sqrt(origin.x() * origin.x() + origin.z() * origin.z());
        if(dXZ < 0.3) {
            buildVertical(builder, origin, age, pTicks, SKEW, WireUV.SKEW_A);
            buildVertical(builder, origin, age, pTicks, SKEW + 90, WireUV.SKEW_B);
        } else {
            buildNominal(builder, origin, age, pTicks, SKEW, false, WireUV.SKEW_A, 1f, dXZ);
            buildNominal(builder, origin, age, pTicks, SKEW + 90, false, WireUV.SKEW_B, 1f, dXZ);
        }
        return builder.build();
    }

    /***
     * Builds a model, but only when the wire is perfectly vertical.
     * This is done for optimization purposes, and because {@link #buildNominal buildNominal}
     * breaks down when rendering perfectly vertical wires.
     * @param builder
     * @param vec
     * @param angle
     * @param uv
     */
    private void buildVertical(WireModel.WireBuilder builder, Vector3f vec, int age, 
        float pTicks, float angle, WireUV uv) {

        float contextualLength = 1f / LOD;
        float chainWidth = (uv.x1() - uv.x0()) / 16 * SCALE;

        Vector3f normal = new Vector3f((float)Math.cos(Math.toRadians(angle)), 0, (float)Math.sin(Math.toRadians(angle)));
        normal.mul(chainWidth);

        Vector3f vert00 = new Vector3f(-normal.x()/2, 0, -normal.z()/2), vert01 = new Vector3f(vert00);
        vert01.add(normal);
        Vector3f vert10 = new Vector3f(-normal.x()/2, 0, -normal.z()/2), vert11 = new Vector3f(vert10);
        vert11.add(normal);

        float uvv0 = 0, uvv1 = 0;
        boolean lastIter_ = false;
        for (int segment = 0; segment < LOD_LIMIT; segment++) {
            if(vert00.y() + contextualLength >= vec.y()) {
                lastIter_ = true;
                contextualLength = vec.y() - vert00.y();
            }

            vert10.add(0, contextualLength, 0);
            vert11.add(0, contextualLength, 0);

            uvv1 += contextualLength / SCALE;

            builder.addVertex(vert00).withUV(uv.x0() / 16f, uvv0).next();
            builder.addVertex(vert01).withUV(uv.x1() / 16f, uvv0).next();
            builder.addVertex(vert11).withUV(uv.x1() / 16f, uvv1).next();
            builder.addVertex(vert10).withUV(uv.x0() / 16f, uvv1).next();

            if(lastIter_) break;

            uvv0 = uvv1;

            vert00.set(vert10);
            vert01.set(vert11);
        }
    }

    private void buildNominal(WireModel.WireBuilder builder, Vector3f vec, int age, 
        float pTicks, float angle, boolean inv, WireUV uv, float offset, float distanceXZ) {
        
        float animatedSag = SAGGINESS;
        float distance = VectorHelper.getLength(vec);
        float realLength, desiredLength = 1 / LOD;

        Vector3f vertA1 = new Vector3f(), vertA2 = new Vector3f(), 
            vertB2 = new Vector3f(), vertB1 = new Vector3f();

        Vector3f normal = new Vector3f(), rotAxis = new Vector3f(), 
            point0 = new Vector3f(), point1 = new Vector3f();

        float width = (uv.x1() - uv.x0()) / 16 * SCALE;
        float wrongDistanceFactor = distance / distanceXZ;
        animatedSag *= distance;

        float uvv0, uvv1 = 0, gradient, x, y;

        point0.set(0, (float) VectorHelper.drip2(0, distance, vec.y()), 0);
        gradient = (float) VectorHelper.drip2prime(0, distance, vec.y());
        normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
        normal.normalize();

        x = VectorHelper.estimateDeltaX(desiredLength, gradient);
        gradient = (float) VectorHelper.drip2prime(animatedSag, x * wrongDistanceFactor, distance, vec.y());
        y = (float) VectorHelper.drip2(animatedSag, x * wrongDistanceFactor, distance, vec.y());
        point1.set(x, y, 0);

        rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
        rotAxis.normalize();

        normal.rotateAxis(angle, rotAxis.x, rotAxis.y, rotAxis.z);
        normal.mul(width);
        vertA1.set(point0.x() - normal.x() / 2, point0.y() - normal.y() / 2, point0.z() - normal.z() / 2);
        vertB1.set(vertA1);
        vertB1.add(normal);

        realLength = VectorHelper.distanceBetween(point0, point1);
    
        // thanks to legoatoom's ConnectableChains for most of this code
        boolean lastIter = false;
        for (int segment = 0; segment < LOD_LIMIT; segment++) {

            rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
            rotAxis.normalize();

            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.rotateAxis(angle, rotAxis.x, rotAxis.y, rotAxis.z);
            normal.mul(width);

            vertA1.set(vertB1);
            vertA2.set(vertB2);

            vertB1.set(point1.x() - normal.x() / 2, point1.y() - normal.y() / 2, point1.z() - normal.z() / 2);
            vertB2.set(vertB1);
            vertB2.add(normal);

            uvv0 = uvv1;
            uvv1 = uvv0 + realLength / SCALE;

            //drawPoint(vertA1, new Color(255, 255, 255));
            //drawPoint(vertB1, new Color(255, 0, 0));
            //drawPoint(vertA2, new Color(0, 255, 0));
            //drawPoint(vertB2, new Color(0, 0, 255));

            //drawPoint(vertC1, new Color(0, 0, 0));

            
            builder.addVertex(vertA1).withUV(uv.x0() / 16f, uvv0).next();
            builder.addVertex(vertA2).withUV(uv.x1() / 16f, uvv0).next();

            builder.addVertex(vertB2).withUV(uv.x1() / 16f, uvv1).next();
            builder.addVertex(vertB1).withUV(uv.x0() / 16f, uvv1).next();

            if(lastIter) break;

            point0.set(point1);

            x += VectorHelper.estimateDeltaX(desiredLength, gradient);
            if (x >= distanceXZ) {
                lastIter = true;
                x = distanceXZ;
            }

            gradient = (float) VectorHelper.drip2prime(animatedSag, x * wrongDistanceFactor, distance, vec.y());
            y = (float) VectorHelper.drip2(animatedSag, x * wrongDistanceFactor, distance, vec.y());

            point1.set(x, y, 0);
            realLength = VectorHelper.distanceBetween(point0, point1);
        }
    }
    
    private void drawPoint(Vector3f point) {
        drawPoint(point, new Color(255, 255, 255));
    }

    private void drawPoint(Vector3f point, Color c) {
        Vec3 p = VectorHelper.toVec(point);
        CreateClient.OUTLINER.showAABB(point, boxFromPos(fromPos.add(p), 0.01f))
            .disableLineNormals()
            .lineWidth(0.01f)
            .colored(c);
    }

    private AABB boxFromPos(Vec3 pos, float s) {
        Vec3 size = new Vec3(s, s, s);
        return new AABB(pos.subtract(size), pos.add(size));
    }

    public static float getScale() {
        return SCALE;
    }

    public void purgeCache() {
        modelCache.clear();
    }
}
