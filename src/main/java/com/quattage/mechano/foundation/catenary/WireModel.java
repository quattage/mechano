package com.quattage.mechano.foundation.catenary;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.theme.Color;
import net.minecraft.world.phys.Vec3;

/**
 * Provides multiple ways to build, update, simulate, and bake
 * catenary wire meshes. These meshes can be bound to 
 * multple shaders (for both BE and chunk meshes) 
 * and pushed to PoseStacks at most phases of rendering.
 *
 * All math is done within a defined local frame.
 * 
 * <h3>Important note about frames of reference:</h3>
 * All wire meshing math is done with single-precision
 * floats to keep things reasonably performant.
 * To accomplish this, the class only stores its {@link #offset displacement in all 3 axes}.
 * Practically, this means that the wire can be constructed, extruded, simulated, and/or meshed in its 
 * own local frame without accumulating precision losses depending on how far it is from the world origin.
 * <p>
 * Implementations need to keep this in mind when rendering, since it means that the wire must be moved to its 
 * in-world position at some point after it's constructed but before it's drawn. Normally, this is done
 * by translating the PoseStack.
 * 
 */
public abstract class WireModel<T extends WireModel<?>> {

    public static int CATENARY_SOLVER_ITERATIONS = 10;

    public static final float CATENARY_POINT_RESOLUTION = 1f;
    public static final float CATENARY_POINT_MASS = 3f;
    public static final float CATENARY_TENSION_EPSILON = 0.001f;
    public static final float CATENARY_HYPERBOLIC = 7.8f;

    public static int CATENARY_POINT_MINIMUM = 4;
    public static int CATENARY_POINT_MAXIMUM = 32;
    
    
    protected @Nullable Vector3f offset;

    public float tension = 0.5f;
    public float length = 0f;

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
     * Updates this WireModel. May bake, simulate, or otherwise
     * construct a mesh based on the implementing subclass's
     * requirements.
     * May throw exceptions depending on initialization state
     * and implementing class.
     */
    abstract void update();

    /**
     * Runs {@link #update} <code>steps</code> number
     * of times. Useful for simulation-based WireModels
     * that use iterative solvers.
     * @param steps
     */
    public void updateAhead(int steps) {
        for(int x = 0; x < steps; x++)
            update();
    }

    /**
     * Tells this WireModel to update its length.
     * Doing so will automatically manage internal arrays if applicable,
     * and calculate local matrices and/or other relevent vector information.
     * <p>
     * This method mirrors the behaviour of {@link #update}, but this method
     * only needs to be called whenever the wire is {@link #setOffset moved,}
     * rather than continuously. To that point, this method is called automatically
     * for WireModel implementations that require it, but this method can still
     * be invoked manually in circumstances where doing so is useful.
     */
    public abstract void calculateSegmentation();


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
            Math.fma(-offset.x, t, offset.x),
            Math.fma(-offset.y, t, offset.y),
            Math.fma(-offset.z, t, offset.z)
        );
        return trgt;
    }

    /**
     * The amount of segments in this wire is determined by the distance spanned, which, in turn
     * determines the length of each uniform segment
     */
    public int getSegmentCount() {
        return Math.max(CATENARY_POINT_MINIMUM, Math.min(CATENARY_POINT_MAXIMUM, (int)(length * CATENARY_POINT_RESOLUTION)));
    }   

    public Vector3f getGravity(int points) {
        return new Vector3f(0, (CATENARY_POINT_MASS / (float)points) * (1 - tension), 0);
    }

    /**
     * For all WireModel implementations, the segment length is assumed to
     * be uniform across the length of the wire. In some situations, this
     * may not be the case, since simulated wire segments can stretch. This 
     * number may not be completely accurate for non-parametric wires.
     * Additionally, a very small number is added to the  resulting length 
     * to prevent simulated wires from becoming too tight. 
     * @param segmentCount the amount of segments in the wire
     */
    public float getLengthAdjustment(int segmentCount) {
        return Math.max(0.0015f, (length / segmentCount) + CATENARY_TENSION_EPSILON) / (Math.max(1f, tension * 16f));
    }

    /**
     * For parametric WireModel implementations, this method
     * will closely approximate the required tension for use
     * as a hyperbolic cosine scaling factor. This method
     * mimics the use case of {@link #getSegmentLength}
     * so that implementing classes can make use of fudged
     * numbers and achieve similar visual results.
     */
    public float getApproximateTension() {
        return tension;
    }

    /**
     * a quick throw for when the offset vector has not been initialized
     * or has been nullified after rendering by some disposal process
     * @throws IllegalStateException if this WireModel has no offset
     */
    protected void assertHasOffset() {
        if(offset == null)
            throw new IllegalStateException("Cannot perform operation on " + this + " - This WireModel is missing a start or end position! (It was either never populated or this WireModel instance was destroyed.)");
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

        public void drawDebug(Vec3 basis, int hashIndex) {
            VectorHelper.drawDebugBox(basis.add(pos.x, pos.y, pos.z), 0.05f, Color.BLACK, "point_" + hashIndex);
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

        public Vector3f getAsRay() {
            facing.x = (float)(start.pos.x - end.pos.x);
            facing.y = (float)(start.pos.y - end.pos.y);
            facing.z = (float)(start.pos.z - end.pos.z);
            return facing;
        }

        public String toString() {
            return "(" + String.format("%.2f", start.pos.x) + ", " + String.format("%.2f", start.pos.y) + ", " + String.format("%.2f", start.pos.z) + "  ->  " + String.format("%.2f", end.pos.x) + ", " + String.format("%.2f", end.pos.y) + ", " + String.format("%.2f", end.pos.z) + ")";
        }
    }
}
