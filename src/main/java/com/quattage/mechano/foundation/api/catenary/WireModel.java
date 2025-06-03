package com.quattage.mechano.foundation.api.catenary;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.phys.Vec3;

/**
 * Provides multiple ways to build, update, simulate, and bake
 * catenary wire meshes. within the defined physical 
 * frame of reference. 
 * 
 * <h3>Important note about frames of reference:</h3>
 * All wire meshing math is done with single-precision
 * floats to keep things reasonably performant.
 * To accomplish this, the class only stores its {@link #offset displacement on all 3 axes}.
 * This means that the wire must be moved to its in-world position at some point
 * after it's constructed but before it's drawn. Normally, this is done
 * by translating the PoseStack.
 * 
 */
public abstract class WireModel<T extends WireModel<?>> {

    public static int CATENARY_SOLVER_ITERATIONS = 5;

    public static final float CATENARY_POINT_RESOLUTION = 1f;
    public static final float CATENARY_POINT_MASS = 3f;
    public static final float CATENARY_TENSION_EPSILON = 0.001f;

    public static int CATENARY_POINT_MINIMUM = 4;
    public static int CATENARY_POINT_MAXIMUM = 32;
    
    protected @Nullable Vector3f offset;

    public float tension = 5f;

    public Vec3 getEnd(Vec3 basis) {
        return new Vec3(basis.x + offset.x, basis.y + offset.y, basis.z + offset.z);
    }

    public Vector3f getOffset() {
        return offset;
    }


    /**
     * Changes the offset of this WireModel, which will
     * change its end point and may make it longer/shorter. 
     * If this WireModel is baked, this method
     * will unbake it, move it, and rebake it. Calls to this method
     * may extend the length of the wire as needed.
     * @param offset New offset
     * @return This WireModel for chaining
     */
    public abstract T setOffset(Vector3f offset);

    /**
     * Automatically calculates the {@link #setOffset offset} vector
     * for this WireModel given a known start and end point
     * @return This WireModel for chaining
     */
    public abstract T setOffset(Vec3 start, Vec3 end);

    /**
     * Renders this WireModel to the provided stack.
     */
    public abstract void render();

    /**
     * Initializes this WireModel, telling it to
     * prepare itself based on its currently configured
     * start and end points. Any data that can't be populated
     * in a constructor should be prepared here.
     * @return This WireModel for chaining
     */
    public abstract T initialize();

    /**
     * At least one call to {@link #setOffset} and {@link #initialize}
     * must be made before this WireModel meets the minimum requirements
     * in order to be meshed, shaded, and rendered. 
     * @return <code>true</code> if this WireModel is initialized.
     */
    public abstract boolean isInitialized();


    /**
     * A helper method provided by all implementing WireModels
     * to visualize their shape directly while skipping any 
     * meshing or shading proceeses.
     * @param basis The basis vector (usually the starting point of the wire)
     * to use when remapping this Wire's coordinates to world-space.
     */
    public abstract void drawDebug(Vec3 basis);

    /**
     * Helper method for lerping from implied
     * start (0, 0, 0) to offset. 
     */
    protected Vector3f quicklerp(Vector3f trgt, float t) {
        trgt.set(
            Math.fma(offset.x, t, offset.x),
            Math.fma(offset.y, t, offset.y),
            Math.fma(offset.z, t, offset.z)
        );
        return trgt;
    }



    /**
     * Represents a singular point in 3D space
     * with a controllable position and velocity.
     */
    public static class Point {
        
        protected Vector3f pos;
        protected Vector3f lastPos;
        protected boolean pinned;

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

        public void drawDebug(Vec3 basis) {
            VectorHelper.drawDebugBox(basis.add(pos.x, pos.y, pos.z), 0.05f, Color.BLACK, "point_" + pos);
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

        protected final Point start, end;
        protected Vector3f facing;
        protected Vector3f center;

        public Stick(Point start, Point end) {
            this.start = start;
            this.end = end;
            this.facing = new Vector3f();
            this.center = new Vector3f();
        }

        public Vector3f getCenter() {
            center.x = (start.pos.x + end.pos.x) / 2f;
            center.y = (start.pos.y + end.pos.y) / 2f;
            center.z = (start.pos.z + end.pos.z) / 2f;
            return center;
        }

        public Vector3f getFacingVector() {
            facing.x = (float)(start.pos.x - end.pos.x);
            facing.y = (float)(start.pos.y - end.pos.y);
            facing.z = (float)(start.pos.z - end.pos.z);
            return facing.normalize();
        }

        public void drawDebug(Vec3 basis) {
            Outliner.getInstance().showLine("stick_" + start + end, basis.add(start.pos.x, start.pos.y, start.pos.z), basis.add(end.pos.x, end.pos.y, end.pos.z)).lineWidth(0.02f).disableCull().colored(Color.GREEN);
        }

        public String toString() {
            return "[" + start + " -> " + end + "]";
        }
    }

}
