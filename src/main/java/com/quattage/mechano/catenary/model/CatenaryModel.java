package com.quattage.mechano.catenary.model;


import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.catenary.CatenaryMeshBuffer;
import com.quattage.mechano.foundation.Disposable;

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
public abstract class CatenaryModel<T extends CatenaryModel<?>> implements Disposable {

    // TODO flywheel and cache

    @Nullable protected Vector3f halfOffset;

    public float span = 0f;

    /**
     * Calculates the {@link #setOffset offset} vector
     * for this Catenary given a known start and end point
     * @return This Catenary for chaining
     */
    public abstract T setOffset(TransmitterType trns, Vector3d start, Vector3d end);

    /**
     * A helper call that sets the first and last
     * {@link Point} in this CatenaryModel to 
     * <code>-halfOffset</code> and <code>halfOffset</code>
     * respectively. Additionally, this method
     * will pin these points in-place and wipe their
     * interpolation data so that they are physically
     * anchored and cannot move.
     */
    public abstract CatenaryModel<T> pinEndpoints();

    /**
     * The opposite of {@link #pinEndpoints}, this method
     * frees the endpoints and restores their interpolation
     * data so that they can receive velocity later.
     */
    public abstract CatenaryModel<T> unpinEndpoints();

    /**
     * If the endpoints are pinned, this method call updates
     * their positions to constrain them to their proper
     * position as <code>halfOffset</code> is updated.
     * For API users, manually invoking this method is
     * usually not necessary, since most implementations 
     * will call this on their own in {@link #update}.
     */
    public abstract CatenaryModel<T> updateEndpoints();

    /**
     * Updates this Catenary. May bake, simulate, or otherwise
     * construct mesh-related data based on the requirements
     * and data structure of this particular implementing catenary.
     * <p>
     * Note that updating a model will not result in any visual
     * indication that anything has occured in-game. For that to
     * happen, the model must be {@link #render pushed to a VertexConsumer}.
     * The re-usable pipeline wrapper, {@link CatenaryMeshBuffer}, contains
     * more robust helper methods for doing this.
     * <p> 
     * <h3>A quick note about update cycles</h3>
     * It is highly recommended that this method is called
     * in a fixed timestep, such as a BlockEntity tick, as
     * most catenary implementations are tuned for a 20 tick
     * fixed update cycle for performance and stability
     * reasons. Updating Catenaries in a frame-dependent context
     * (such as a BlockEntityRenderer) comes with an immediate performance
     * hit, as well as a strong likelihood to produce poor results,
     * especially at particularly high or low framerates. 
     */
    public abstract void update(TransmitterType trns);

    /**
     * Runs {@link #update} <code>steps</code> number
     * of times. This is useful for simulation-based
     * Catenary implementations that use iterative
     * solvers, where you need to run {@link #update}
     * multiple times in order to achieve the desired 
     * result.
     * @param steps
     */
    public void updateAhead(TransmitterType trns, int steps) {
        for(int x = 0; x < steps; x++)
            update(trns);
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
    public abstract CatenaryModel<T> calculateSegmentation(TransmitterType trns);

    /**
     * Renders this Catenary to the provided stack. For more 
     * comprehensive access and ease of use, this method is
     * primarily intended to be accessed via the
     * {@link CatenaryMeshBuffer#render geometry dispatcher}
     */
    public abstract CatenaryModel<T> render(VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo, float pTicks);

    /**
     * Renders this Catenary to the provided stack. For more 
     * comprehensive access and ease of use, this method is
     * primarily intended to be accessed via the
     * {@link CatenaryMeshBuffer#render geometry dispatcher}
     */
    public CatenaryModel<T> render(VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo) {
        return render(buffer, pose, geo, 1);
    }

    /**
     * Initializes this Catenary, telling it to
     * prepare itself based on its currently configured
     * start and end points. Any data that can't be populated
     * in a constructor should be prepared here.
     * @return This Catenary for chaining
     */
    public abstract T initializeSpan();

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

    public void adjustSpan(LevelReader world, float span) {
        this.span = span;
    }

    /**
     * Determines whether or not this Catenary has reached a state of
     * restitution. If <code>false</code>, this Catenary is moving
     * or could move at any time, and should not be baked.
     * @return <code>true</code> if this Catenary is completely still.
     */
    public abstract boolean isResting();

    /**
     * a throw for when the offset vector has not been initialized
     * or has been nullified after rendering by some disposal process
     * @throws IllegalStateException if this Catenary has no offset
     */
    protected void assertHasOffset() {
        if(halfOffset == null) throw new IllegalStateException("Cannot perform operation on " + this 
            + " - This Catenary is missing a start or end position! (It was either never populated or this Catenary instance was destroyed.)");
    }

    /**
     * throws when this Catenary is not in its initialized state
     * @throws IllegalStateException 
     */
    protected void assertInitialized() {
        if(!isInitialized())
            throw new IllegalStateException("Cannot update " + this + " - This Catenary has not been initialized!");
    }

    @Override
    public void dispose() {
        halfOffset = null;
        span = 0;
    }

    public abstract void lockSpan();
    public abstract void unlockSpan();
}
