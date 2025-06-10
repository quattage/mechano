package com.quattage.mechano.foundation.catenary.meshing;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.CatenaryAttributeHolder;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.ModelType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Thickness;
import com.quattage.mechano.foundation.helper.VectorHelper;


public class CatenaryGeometry {

    public static MutableExtruder begin(ModelType model) {
        return new MutableExtruder(model);
    }

    public static class MutableExtruder extends CatenaryAttributeHolder {

        private Vec3 basis;
        private MutableBlockPos lightLookup;
        private final int[] light = new int[2];
        private @Nullable BlockAndTintGetter world;

        public final UVWalker uvs;
        private final float[] data = new float[44];

		private MutableExtruder(ModelType model) {
			super(model);
            this.world = null;
            this.lightLookup = new MutableBlockPos();
            this.data[0] = thick.get() / 2f;
            uvs = new UVWalker();
            uvs.setWidth(thick.getPixels());
		}

        public MutableExtruder in(BlockAndTintGetter world) {
            this.world = world;
            return this;
        } 

        public MutableExtruder at(Vec3 basis) {
            this.basis = basis;
            return this;
        }

        public MutableExtruder withSprite(TextureAtlasSprite sprite) {
            uvs.applyAtlas(sprite);
            return this;
        }

        @Override
        public MutableExtruder withThickness(Thickness thick) {
            this.data[0] = thick.get() / 2f;
            this.uvs.setWidth(thick.getPixels());
            return this;
        }

        @Override
        public MutableExtruder withTension(Tension tension) {
            return (MutableExtruder)super.withTension(tension);
        }

        public void resetMatrix() {
            data[1]  = 0; data[2]  = 0; data[3]  = 0; // right
            data[4]  = 0; data[5]  = 0; data[6]  = 0; // up
            data[7]  = 0; data[8]  = 0; data[9]  = 0;  // forward
            data[10] = 0; data[11] = 0; data[12] = 0; // normU
            data[13] = 0; data[14] = 0; data[15] = 0; // normV
        }

        public void resetVerts() {
            data[16] = 0; data[17] = 0; data[18] = 0;
            data[19] = 0; data[20] = 0; data[21] = 0;
            data[22] = 0; data[23] = 0; data[24] = 0;
            data[25] = 0; data[26] = 0; data[27] = 0;
            data[28] = 0; data[29] = 0; data[30] = 0;
            data[31] = 0; data[32] = 0; data[33] = 0;
            data[34] = 0; data[35] = 0; data[36] = 0;
            data[37] = 0; data[38] = 0; data[39] = 0;
        }

        public MutableExtruder computeMatrix(Vector3f forward) {
            data[4] = CatenaryAttributes.UP.x; 
            data[5] = CatenaryAttributes.UP.y; 
            data[6] = CatenaryAttributes.UP.z;
            data[7] = forward.x; 
            data[8] = forward.y; 
            data[9] = forward.z;

            data[1] = Math.fma(data[5], forward.z, -data[6] * forward.y);
            data[2] = Math.fma(data[6], forward.x, -data[4] * forward.z);
            data[3] = Math.fma(data[4], forward.y, -data[5] * forward.x);

            data[4] = Math.fma(data[8], data[3], -data[9] * data[2]);
            data[5] = Math.fma(data[9], data[1], -data[7] * data[3]);
            data[6] = Math.fma(data[7], data[2], -data[8] * data[1]);
            return this;
        }

        public MutableExtruder addVert(int offset, float x, float y, float z) {
            if(offset >= 8)
                throw new ArrayIndexOutOfBoundsException("Can't store vertex at offset " + offset + " - Only up to 8 vertices are allowed!");
            offset = offset * 3 + 16;
            data[offset] = x; data[offset + 1] = y; data[offset + 2] = z;
            return this;
        }

        public float v(int index) {
            return data[index];
        }

        public int getLight(Vector3f pos) {
            if(world == null)
                return LightTexture.FULL_BRIGHT;
            lightLookup.setX((int)Math.floor(pos.x + basis.x)); 
            lightLookup.setY((int)Math.floor(pos.y + basis.y)); 
            lightLookup.setZ((int)Math.floor(pos.z + basis.z));
            return LightTexture.pack(world.getBrightness(LightLayer.BLOCK, lightLookup), world.getBrightness(LightLayer.SKY, lightLookup));
        }

        public Vec3 getBasis() {
            return basis;
        }

        public BlockAndTintGetter getWorld() {
            return world;
        }
    }


    /**
     * Represents a singular point in 3D space
     * with a controllable position and velocity.
     * <h2>Important Note:</h2>
     * All coordinates for both Points and Sticks fall within the parent
     * wire's Local frame of reference. This point's position vector
     * does NOT represent a point in the world. For more information, 
     * read the javadoc attached to {@link WireModel}
     */
    public static class Point {

        public Vector3f pos;
        public Vector3f lastPos;
        public boolean pinned;

        public Point(Vector3f pos) {
            setPos(pos);
            this.pinned = false;
        }

        public void clearPos() {
            this.pos = new Vector3f();
            this.lastPos = new Vector3f();
        }

        public void setPos(Vector3f pos) {
            this.pos = new Vector3f(pos);
            this.lastPos = new Vector3f(pos);
        }

        public void pin() {
            this.pinned = true;
            lastPos = new Vector3f(pos);
        }

        public void unpin() {
            this.pinned = false;
        }

        public String toString() {
            return"(" + String.format("%4.3f" , pos.x) + ", " +  String.format("%4.3f" , pos.y) + ", " +  String.format("%4.3f" , pos.z) + ")";
        }

        public void drawDebug(Vec3 basis, int hashIndex) {
            VectorHelper.drawDebugBox(basis.add(pos.x, pos.y, pos.z), 0.05f, Color.BLACK, "point_" + hashIndex);
        }

        public int getLight(Vec3 basis, BlockAndTintGetter world) {
            BlockPos pos = new BlockPos((int)Math.floor(this.pos.x + basis.x), (int)Math.floor(this.pos.y + basis.y), (int)Math.floor(this.pos.z + basis.z));
            return LightTexture.pack(world.getBrightness(LightLayer.BLOCK, pos), world.getBrightness(LightLayer.SKY, pos));
        }
    }

    /**
     * A physical link connecting two {@link Point points}
     * Designed as a way for PBD/particle simulations to
     * apprixmimate the behaviour of chains by representing
     * an arbitrary volume as a length.
     * <h2>Important Note:</h2>
     * All coordinates for both Points and Sticks fall within the parent
     * wire's Local frame of reference. For more information, 
     * read the javadoc attached to {@link WireModel}
     */
    public static class Stick {

        public final Point start, end;
        public Vector3f facing;
        public Vector3f center;

        public Stick(Point start, Point end) {
            this.start = start;
            this.end = end;
            this.facing = new Vector3f();
            this.center = new Vector3f();
        }

        public Vector3f computeCenter() {
            center.x = (start.pos.x + end.pos.x) / 2f;
            center.y = (start.pos.y + end.pos.y) / 2f;
            center.z = (start.pos.z + end.pos.z) / 2f;
            return center;
        }

        public Vector3f computeForward() {
            facing.x = (float)(start.pos.x - end.pos.x);
            facing.y = (float)(start.pos.y - end.pos.y);
            facing.z = (float)(start.pos.z - end.pos.z);
            return facing.normalize();
        }

        public Vector3f getForward() {
            return facing;
        }

        public String toString() {
            return "(" + String.format("%.2f", start.pos.x) + ", " + String.format("%.2f", start.pos.y) + ", " + String.format("%.2f", start.pos.z) + "  ->  " + String.format("%.2f", end.pos.x) + ", " + String.format("%.2f", end.pos.y) + ", " + String.format("%.2f", end.pos.z) + ")";
        }
    }

}
