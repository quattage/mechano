

package com.quattage.mechano.core.electricity.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.util.VectorHelper;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/***
 * Populates a given PoseStack with a dynamically generated WireModel
 */
public class WireModelRenderer {

    /***
     * Overall scale, or "thickness" of the wire
     */
    private static final float SCALE = 0.9f;

    /***
     * The rotation (in degrees) of the wire's profile
     */
    private static final int SKEW = 45;

    /***
     * How much the wire hangs
     */
    private static final float SAGGINESS = 25;

    /***
     * The wire's Level of Detail
     */
    private static final float LOD = 1.5f;

    /***
     * The maximum amount of iterations for a single wire. Used
     * to prevent lag or stack overflows in extreme edge cases 
     */
    private static final int LOD_LIMIT = 512;

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

        if(Float.isNaN(origin.x()) && Float.isNaN(origin.z())) {
            buildVertical(builder, origin, age, pTicks, SKEW, WireUV.SKEW_A);        //      | 
            buildVertical(builder, origin, age, pTicks, SKEW + 90, WireUV.SKEW_B);   //      —
        } else {
            buildNominal(builder, origin, age, pTicks, SKEW, WireUV.SKEW_A, 1f);         //      |  
            buildNominal(builder, origin, age, pTicks, SKEW + 90, WireUV.SKEW_B, 1f);    //      —
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

        float contextualLength = 1f * LOD;
        float fullWidth = (uv.x1() - uv.x0()) / 16 * SCALE;

        Vector3f unit = new Vector3f((float) Math.cos(Math.toRadians(angle)), 0, (float) Math.sin(Math.toRadians(angle)));
        unit.mul(fullWidth);

        Vector3f vertA1 = new Vector3f(-unit.x() / 2, 0, -unit.z() / 2), 
            vertA2 = vertA1.copy();
        vertA2.add(unit);

        Vector3f vertB1 = new Vector3f(-unit.x() / 2, 0, -unit.z() / 2), 
            vertB2 = vertB1.copy();
        vertA2.add(unit);

        float uvv0 = 0, uvv1 = 0;
        boolean lastIter = false;
        for (int segment = 0; segment < LOD_LIMIT; segment++) {
            if (vertA1.y() + contextualLength >= vec.y()) {
                lastIter = true;
                contextualLength = vec.y() - vertA1.y();
            }

            vertB1.add(0, contextualLength, 0);
            vertB2.add(0, contextualLength, 0);

            uvv1 += contextualLength / SCALE;

            builder.addVertex(vertA1).withUV(uv.x0() / 16f, uvv0).next();
            builder.addVertex(vertA2).withUV(uv.x1() / 16f, uvv0).next();
            builder.addVertex(vertB2).withUV(uv.x1() / 16f, uvv1).next();
            builder.addVertex(vertB1).withUV(uv.x0() / 16f, uvv1).next();

            if (lastIter) break;

            uvv0 = uvv1;

            vertA1.load(vertB1);
            vertA2.load(vertB2);
        }
    }

    private void buildNominal(WireModel.WireBuilder builder, Vector3f vec, int age, 
        float pTicks, float angle, WireUV uv, float offset) {
        
        float contextualLength = 1f * LOD;
        float distance = VectorHelper.getLength(vec) * 0.7f, distanceXZ = (float) Math.sqrt(vec.x() * vec.x() + vec.z() * vec.z());

        float wrongDistanceFactor = distance / distanceXZ;

        float animatedSag = SAGGINESS;
        
        if(age > -1) {
            float subduedness = 1.6263f + 0.1664f * distance;                                 // variable tamping factor to decrease the wiggle vigor
            float speed = age * (0.576f - 0.003f * age) / (subduedness * 0.6f);               // ramped speed of the wiggleness
            float intensity = SAGGINESS * (float)(Math.sin((age * -0.00773f)) + 0.8f);        // sin ramped wiggle distance value
            animatedSag = SAGGINESS + (float)(Math.cos(speed)) * (intensity / subduedness);   // overall sag value as a cosine wave
        }


        if(distance > 1.4) animatedSag *= (distance * 0.0814) - 0.08461;
        else contextualLength = distance;

        Mechano.logSlow("D: " + distance + "  S:" + animatedSag, 500);

        Vector3f vertA1 = new Vector3f(), vertA2 = new Vector3f(), 
            vertB2 = new Vector3f(), vertB1 = new Vector3f();

        Vector3f normal = new Vector3f(), rotAxis = new Vector3f();
        float fullWidth = (uv.x1() - uv.x0()) / 16 * SCALE;

        float uvv0, uvv1 = 0; 
        float gradient;
        float x, y;
        Vector3f point0 = new Vector3f(), point1 = new Vector3f();

        point0.set(0, (float) VectorHelper.drip2(animatedSag, 0, distance, vec.y()), 0);
        gradient = (float) VectorHelper.drip2prime(animatedSag, 0, distance, vec.y());
        normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
        normal.normalize();

        x = VectorHelper.estimateDeltaX(contextualLength, gradient);
        gradient = (float) VectorHelper.drip2prime(animatedSag, x * wrongDistanceFactor, distance, vec.y());
        y = (float) VectorHelper.drip2(animatedSag, x * wrongDistanceFactor, distance, vec.y());
        point1.set(x, y, 0);

        rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
        rotAxis.normalize();

        Quaternion rotator = rotAxis.rotationDegrees(angle);
        normal.transform(rotator);
        normal.mul(fullWidth);

        vertB1.set(point0.x() - normal.x() / 2, point0.y() - normal.y() / 2, point0.z() - normal.z() / 2);

        vertB2.load(vertB1);
        vertB2.add(normal);

        contextualLength = VectorHelper.distanceBetween(point0, point1);
    
        // thanks to legoatoom's ConnectableChains for most of this code
        boolean lastIter = false;
        for (int segment = 0; segment < LOD_LIMIT; segment++) {

            rotAxis.set(point1.x() - point0.x(), point1.y() - point0.y(), point1.z() - point0.z());
            rotAxis.normalize();
            rotator = rotAxis.rotationDegrees(angle);

            normal.set(-gradient, Math.abs(distanceXZ / distance), 0);
            normal.normalize();
            normal.transform(rotator);
            normal.mul(fullWidth);

            vertA1.load(vertB1);
            vertA2.load(vertB2);

            

            vertB1.set(point1.x() - normal.x() / 2, point1.y() - normal.y() / 2, point1.z() - normal.z() / 2);
            vertB2.load(vertB1);
            vertB2.add(normal);

            uvv0 = uvv1;
            uvv1 = uvv0 + contextualLength / SCALE;

            builder.addVertex(vertA1).withUV(uv.x0() / 16f, uvv0).next();
            builder.addVertex(vertA2).withUV(uv.x1() / 16f, uvv0).next();
            builder.addVertex(vertB2).withUV(uv.x1() / 16f, uvv1).next();
            builder.addVertex(vertB1).withUV(uv.x0() / 16f, uvv1).next();

            if (lastIter) break;

            point0.load(point1);

            x += VectorHelper.estimateDeltaX(contextualLength, gradient);
            if (x >= distanceXZ) {
                lastIter = true;
                x = distanceXZ;
            }

            gradient = (float) VectorHelper.drip2prime(animatedSag, x * wrongDistanceFactor, distance, vec.y());
            y = (float) VectorHelper.drip2(animatedSag, x * wrongDistanceFactor, distance, vec.y());
            point1.set(x, y, 0);

            contextualLength = VectorHelper.distanceBetween(point0, point1);
        }
    }

    public static float getScale() {
        return SCALE;
    }

    public void purgeCache() {
        modelCache.clear();
    }
}
