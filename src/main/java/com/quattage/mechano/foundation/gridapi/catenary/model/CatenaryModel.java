package com.quattage.mechano.foundation.gridapi.catenary.model;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryMesher.Point;
import com.quattage.mechano.foundation.gridapi.catenary.Tensionable;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.gridapi.switchboard.TrackedStreamable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

/**
 * Provides multiple ways to build, update, simulate, and bake
 * catenary wire meshes. These meshes can be bound to 
 * multple shaders (for both chunk and non-chunk meshes) 
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
 * by applying a basis vector and/or translating the PoseStack, depending on what context you're rendering from.
 * 
 */
public abstract class CatenaryModel<T extends CatenaryModel<?>> implements Tensionable {

    // TODO flywheel and caching

    protected static final float fudge = 0.76f;

    @Nullable
    public Vector3f offset;

    // average accumulated velocity as of the last time the wire's shape was updated
    protected float avgVelocity = 0f;
    public float length = 0f;
    public float maxLength = 32f;

    /**
     * Dumps the given catenary, nullifying its internal references
     * and destroying it.
     * @param cat
     */
    public static void dispose(CatenaryModel<?> cat) {
        if(cat == null) return;
        cat.offset = null;
        cat.avgVelocity = 0;
        cat.length = 0;
        cat.maxLength = 0;
        cat.destroy();
        // also nullify flywheel stuff and cache info if i ever do that in the future
    }

    public Vec3 getEnd(Vec3 basis) {
        return new Vec3(basis.x + offset.x, basis.y + offset.y, basis.z + offset.z);
    }

    public Vector3f getOffset() {
        return offset;
    }

    /**
     * Changes the offset of this Catenary, which will
     * change its end point and may make it longer/shorter. 
     * If this Catenary is baked, this method
     * will unbake it, move it, and rebake it. Calls to this method
     * may extend the length of the wire as needed.
     * @param offset New offset
     * @return This Catenary for chaining
     */
    public abstract T setOffset(Vector3f offset);

    /**
     * Automatically calculates the {@link #setOffset offset} vector
     * for this Catenary given a known start and end point
     * @return This Catenary for chaining
     */
    public abstract T setOffset(Vec3 start, Vec3 end);

    /**
     * Automatically calculates the {@link #setOffset offset} vector
     * for this catenary given a pair of {@link GridUUID addresses}
     * and enforces their order using the deterministic 
     * {@link TrackedStreamable#orderedByRenderPriority render priority}
     * to ensure that the sign of the offset vector's length is correct.
     * @param world World to operate within
     * @param start GridUUID starting position
     * @param end GridUUID ending position (start/end order here is arbitrary)
     * @param pTicks Partial ticks to use for lerping where necessary. When in doubt,
     * just pass 1.
     */
    public void setOrderedOffset(LevelReader world, GridUUID start, GridUUID end, float pTicks) {
        TrackedStreamable[] ordered = TrackedStreamable.orderedByAssertionPriority(world, start, end);
        setOffset(((AnchorPoint)ordered[0]).getPos(world, pTicks), ((AnchorPoint)ordered[1]).getPos(world, pTicks));
    }

    /**
     * A helper call that sets the first and last
     * {@link Point} in this CatenaryModel to 
     * <code>0, 0, 0</code> and <code>offset</code>
     * respectively. Additionally, this method
     * will pin these points in-place and wipe their
     * interpolation data so that they are physically
     * anchored and cannot move.
     */
    public abstract CatenaryModel<T> fixEndpoints();

    /**
     * Updates this Catenary. May bake, simulate, or otherwise
     * construct mesh-related based on the implementing subclass's
     * requirements in any structure deemed suitable by this
     * Catenary's underlying implementation. 
     * <p>
     * Note that updating a model will not result in any visual
     * indication that anything has occured in-game. For that to
     * happen, the model must be {@link #render pushed to a VertexConsumer}.
     * The re-usable pipeline wrapper, {@link CatenaryMesher}, contains
     * more robust helper methods for doing this.
     * <p> 
     * <h3>A quick note about update cycles</h3>
     * It is reccomended that most Catenary implementations run
     * on a fixed update cycle for performance and stability
     * reasons. Updating Catenarys in a frame-dependent context
     * (such as a renderer) comes with an immediate performance
     * hit, as well as a potential to produce bad results at 
     * especially high or low framerates. If you must call this
     * method in a non-fixed context, use the {@link #update(float) 
     * overload that takes a delta}.
     */
    public void update() {
        update(1);
    }

    /**
     * Updates this Catenary. May bake, simulate, or otherwise
     * construct mesh-related based on the implementing subclass's
     * requirements in any structure deemed suitable by this
     * Catenary's underlying implementation. 
     * <p>
     * Note that updating a model will not result in any visual
     * indication that anything has occured in-game. For that to
     * happen, the model must be {@link #render pushed to a VertexConsumer}.
     * The re-usable pipeline wrapper, {@link CatenaryMesher}, contains
     * more robust helper methods for doing this.
     * <p> 
     * <h3>A quick note about update cycles</h3>
     * It is reccomended that most Catenary implementations run
     * on a fixed update cycle for performance and stability
     * reasons. Updating Catenarys in a frame-dependent context
     * (such as a renderer) comes with an immediate performance
     * hit, as well as a potential to produce bad results at 
     * especially high or low framerates. If you must call this
     * method in a non-fixed context, use the {@link #update(float) 
     * overload that takes a delta}.
     */
    public abstract void update(float delta);

    /**
     * Runs {@link #update} <code>steps</code> number
     * of times. This is useful for simulation-based
     * Catenary implementations that use iterative
     * solvers, where you need to run {@link #update}
     * multiple times in order to achieve the desired 
     * result.
     * @param steps
     */
    public void updateAhead(int steps) {
        for(int x = 0; x < steps; x++)
            update();
    }

    /**
     * Tells this Catenary to update its length.
     * Doing so will automatically manage internal arrays if applicable,
     * and calculate local matrices and/or other relevent vector information.
     * <p>
     * This method mirrors the behaviour of {@link #update}, but this method
     * only needs to be called whenever the wire is {@link #setOffset moved,}
     * rather than continuously. To that point, this method is called automatically
     * for Catenary implementations that require it, but this method can still
     * be invoked manually in circumstances where doing so is useful.
     */
    public abstract CatenaryModel<T> calculateSegmentation();

    /**
     * Renders this Catenary to the provided stack. For more 
     * comprehensive access and ease of use, this method is
     * primarily intended to be accessed via the
     * {@link CatenaryMesher#render geometry dispatcher}
     */
    public abstract CatenaryModel<T> render(VertexConsumer buffer, Pose pose, CatenaryMesher geo, float pTicks);

    /**
     * Initializes this Catenary, telling it to
     * prepare itself based on its currently configured
     * start and end points. Any data that can't be populated
     * in a constructor should be prepared here.
     * @return This Catenary for chaining
     */
    public abstract T initialize();

    /**
     * At least one call to {@link #setOffset} and {@link #initialize}
     * must be made before this Catenary meets the minimum requirements
     * in order to be meshed, shaded, and rendered. 
     * @return <code>true</code> if this Catenary is initialized.
     */
    public abstract boolean isInitialized();


    /**
     * A helper method provided by all implementing Catenarys
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
            Mth.lerp(t, 0, offset.x),
            Mth.lerp(t, 0, offset.y),
            Mth.lerp(t, 0, offset.z)
        );
        return trgt;
    }

    @Override
    public float getSpan() {
        return length;
    }

    @Override
    public float getMaximumSpan() {
        return maxLength;
    }

    @Override
    public void adjustSpan(LevelReader world, float length) {
        this.maxLength = length;
    }

    /**
     * The amount of segments in this wire is determined by the distance spanned, which, in turn
     * determines the length of each uniform segment
     */
    public int getSegmentCount() {
        return Math.max(CatenaryAttributes.DRAW_MIN, Math.min(CatenaryAttributes.DRAW_MAX, (int)(length * CatenaryAttributes.DRAW_RES)));
    }   

    public Vector3f getGravity(int points) {
        return CatenaryAttributes.UP.mul((CatenaryAttributes.POINT_MASS / (float)points) * 0.3f, new Vector3f());
    }

    public Vector3f getGravity(int points, DeltaTracker delta) {
        return CatenaryAttributes.UP.mul((CatenaryAttributes.POINT_MASS / (float)points) * delta.getGameTimeDeltaTicks(), new Vector3f());
    }

    /**
     * For all Catenary implementations, the segment length is assumed to
     * be uniform across the length of the wire. In some situations, this
     * may not be the case, since simulated wire segments can stretch. This 
     * number may not be completely accurate for non-parametric wires.
     * Additionally, a very small number is added to the  resulting length 
     * to prevent simulated wires from becoming too tight. 
     * @param segmentCount the amount of segments in the wire
     */
    public float getSegmentLength(int segmentCount) {
        return Math.max(0.0015f, (length / segmentCount) + CatenaryAttributes.TENSION_EPSILON) / (Math.max(1f, (fudge * 16)));
    }

    /**
     * For parametric Catenary implementations, this method
     * will closely approximate the required tension for use
     * as a hyperbolic cosine scaling factor. This method
     * mimics the use case of {@link #getSegmentLength}
     * so that implementing classes can make use of fudged
     * numbers and achieve similar visual results.
     */
    public float getApproximateTension() {
        return (1 - fudge) * ((Math.abs(offset.x) + Math.abs(offset.z)) / (128 * (fudge * fudge)));
    }

    /**
     * a quick throw for when the offset vector has not been initialized
     * or has been nullified after rendering by some disposal process
     * @throws IllegalStateException if this Catenary has no offset
     */
    protected void assertHasOffset() {
        if(offset == null)
            throw new IllegalStateException("Cannot perform operation on " + this + " - This Catenary is missing a start or end position! (It was either never populated or this Catenary instance was destroyed.)");
    }


    /**
     * throws when this Catenary is not in its initialized state
     * @throws IllegalStateException 
     */
    protected void assertInitialized() {
        if(!isInitialized())
            throw new IllegalStateException("Cannot update " + this + " - This Catenary has not been initialized!");
    }

    /**
     * @return The average velocity of each point in this Catenary
     */
    public float getAverageVelocity() {
        return avgVelocity;
    }

    /**
     * Converts this Catenary to its {@link SimulatedCatenary simulatable version}
     * if possible. Calls to this method will <strong>uninitialize</strong> this
     * Catenary instance during the process of creating a SimulatedCatenary,
     * transfering its {@link Point point data} over without copying.
     * @return A (new or preexisting) SimulatedCatenary instance
     */
    public abstract SimulatedCatenary toSimulated();

    /**
     * Converts this Catenary to its {@link ParametricCatenary non-simulated version}
     * if possible. Successful calls to this method will <strong>uninitialize</strong> 
     * this catenary, making it unusable.
     * @return A (new or preexisting) ParametricCatenary instance
     */
    public abstract ParametricCatenary toParametric();

    /**
     * Converts this Catenary to its {@link BakedCatenary baked version}
     * if possible. Successful calls to this method will <strong>uninitialize</strong> 
     * this catenary, making it unusable.
     * @return A new BakedCatenary instance
     */
    public abstract BakedCatenary bake();

    /**
     * Determines whether or not this Catenary has reached a state of
     * restitution. If <code>false</code>, this Catenary is moving
     * or could move at any time, and should not be baked.
     * @return <code>true</code> if this Catenary is completely still.
     */
    public boolean isResting() {
        return this.avgVelocity <= CatenaryAttributes.RESTITUTION_VELOCITY;
    }

    protected abstract void destroy();
}
