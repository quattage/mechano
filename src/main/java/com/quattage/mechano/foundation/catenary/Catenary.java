package com.quattage.mechano.foundation.catenary;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.foundation.api.anchor.AnchorSelector;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher.Point;
import com.quattage.mechano.foundation.catenary.model.SimulatedCatenary;

import net.minecraft.client.DeltaTracker;
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
public abstract class Catenary<T extends Catenary<?>> implements Tensionable {

    // TODO FLYWHEEL

    @Nullable
    public Vector3f offset;
    public Tension tension = Tension.AVERAGE;
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

    @SuppressWarnings("unchecked")
    public T setOffset(AnchorSelector selector) {
        if(selector == null) return (T)this;
        if(!selector.hasSelection()) return (T)this;
        if(AnchorSelector.INSTANCE.lookingRay == null) return (T)this;
        return setOffset(AnchorSelector.INSTANCE.selected.anchor.getPos(selector.playerHands.player().level()), AnchorSelector.INSTANCE.lookingRay.end);
    }


    /**
     * Updates this WireModel. May bake, simulate, or otherwise
     * construct mesh-related based on the implementing subclass's
     * requirements in any structure deemed suitable by this
     * WireModel's underlying implementation. 
     * <p>
     * Note that updating a model will not result in any visual
     * indication that anything has occured in-game. For that to
     * happen, the model must be {@link #render pushed to a VertexConsumer}
     * The re-usable pipeline wrapper, {@link CatenaryMesher}, contains
     * more robust helper methods for doing this.
     * <p> 
     * <h3>A quick note about update cycles</h3>
     * It is reccomended that most WireModel implementations run
     * on a fixed update cycle for performance and stability
     * reasons. Updating WireModels in a frame-dependent context
     * (such as a renderer) comes with an immediate performance
     * hit, as well as a potential to produce bad results at 
     * especially high or low framerates If you must call this
     * method in a non-fixed context, use the {@link #update(float) 
     * overload that takes a delta}.
     */
    public void update() {
        update(1);
    }

    /**
     * Updates this WireModel. May bake, simulate, or otherwise
     * construct mesh-related based on the implementing subclass's
     * requirements in any structure deemed suitable by this
     * WireModel's underlying implementation. 
     * <p>
     * Note that updating a model will not result in any visual
     * indication that anything has occured in-game. For that to
     * happen, the model must be {@link #render pushed to a VertexConsumer}
     * The re-usable pipeline wrapper, {@link CatenaryMesher}, contains
     * more robust helper methods for doing this.
     * <p> 
     * <h3>A quick note about update cycles</h3>
     * It is reccomended that most WireModel implementations run
     * on a fixed update cycle for performance and stability
     * reasons. Updating WireModels in a frame-dependent context
     * (such as a renderer) comes with an immediate performance
     * hit, as well as a potential to produce bad results at 
     * especially high or low framerates If you must call this
     * method in a non-fixed context, use the {@link #update(float) 
     * overload that takes a delta}.
     */
    public abstract void update(float delta);

    /**
     * Runs {@link #update} <code>steps</code> number
     * of times. This is useful for simulation-based
     * WireModel implementations that use iterative
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
     * Renders this WireModel to the provided stack. For more 
     * comprehensive access and ease of use, this method is
     * primarily intended to be accessed via the
     * {@link CatenaryMesher#render geometry dispatcher}
     */
    public abstract void render(VertexConsumer buffer, Pose pose, CatenaryMesher geo, float pTicks);

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
        return Math.max(CatenaryAttributes.DRAW_MIN, Math.min(CatenaryAttributes.DRAW_MAX, (int)(length * CatenaryAttributes.DRAW_RES)));
    }   

    public Vector3f getGravity(int points) {
        return CatenaryAttributes.UP.mul((CatenaryAttributes.POINT_MASS / (float)points) * (1 - tension.get()), new Vector3f());
    }

    public Vector3f getGravity(int points, DeltaTracker delta) {
        return CatenaryAttributes.UP.mul(((CatenaryAttributes.POINT_MASS / (float)points) * (1 - tension.get())) * delta.getGameTimeDeltaTicks(), new Vector3f());
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
        return Math.max(0.0015f, (length / segmentCount) + CatenaryAttributes.TENSION_EPSILON) / (Math.max(1f, tension.get(16f)));
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
        return (1 - tension.get()) * ((Math.abs(offset.x) + Math.abs(offset.z)) / (128 * (tension.getSquared())));
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

    @Override
    public Tension getTension() {
        return tension;
    }

    @Override
    public boolean setTension(Tension tension) {
        if(tension == null) return setTension();
        if(this.tension.equals(tension)) return false;
        this.tension = tension;
        return true;
    }

    /**
     * Converts this WireModel to its {@link SimulatedCatenary simulatable version}
     * if possible. Calls to this method will <strong>uninitialize</strong> this
     * WireModel instance during the process of creating a SimulatedWireModel,
     * transfering its {@link Point point data} over without copying.
     * @param pinEnds <code>true</code> if the resulting SimulatedWireModel
     * should have its ends pinned during its initialization phase
     * @return A (new or preexisting) SimulatedWireModel instance
     */
    public abstract SimulatedCatenary toSimulated(boolean pinEnds);
}
