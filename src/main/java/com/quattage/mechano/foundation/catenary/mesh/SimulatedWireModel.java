package com.quattage.mechano.foundation.catenary.mesh;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryGeometry;
import com.quattage.mechano.foundation.catenary.CatenaryGeometry.Point;
import com.quattage.mechano.foundation.catenary.CatenaryGeometry.Stick;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.phys.Vec3;

public class SimulatedWireModel extends WireModel<SimulatedWireModel> {

    // average accumulated velocity as of the last time the wire was simulated
    public float avgVelocity = 0;

    protected @Nullable ObjectArrayList<Point> points;
    protected @Nullable ObjectArrayList<Stick> sticks;

    public SimulatedWireModel() {}

    /**
     * Initializes a Verlet Integration simulation for this WireModel. Calls to this 
     * method populate this WireModel's internal points array to the size necessary 
     * to span across this wire's {@link #offset magnitude vector.} As a result of 
     * this call, each {@link Point} and {@link Stick} is aligned in a straight 
     * line. No offsets or simulated constraints are applied until at least one call 
     * to {@link #update} is made.
     * <p>
     * Additionally, if any pre-existing vertex data exists for this WireModel, 
     * that data will be cleared. This WireModel will return to its initial state.
     * @return This WireModel for chaining calls
     */
    public SimulatedWireModel initialize() {
        assertHasOffset();
        int segments = getSegmentCount();
        this.points = new ObjectArrayList<Point>(segments + 1);
        this.sticks = new ObjectArrayList<Stick>(segments);
        Point previous = null;
        Vector3f trgt = new Vector3f();
        for(int x = 0; x < segments; x++) {
            float spanProgress = 1f - ((float)x / (float)segments);
            Point newPoint = new Point(quicklerp(trgt, spanProgress));
            points.add(x, newPoint);
            if(previous != null) {
                sticks.add(x - 1, new Stick(previous, newPoint));
                Mechano.LOGGER.info(x + ", " + spanProgress + ": " + sticks.get(x - 1) + ", diff: " + sticks.get(x - 1).start.pos.distance(sticks.get(x - 1).end.pos));
            }
            previous = newPoint;
        }
        points.getFirst().pin();
        points.getLast().pin();
        return this;
    }

    /**
     * Extends this wire by the given amount of segments
     * while retaining velocity data for all pre-existing 
     * {@link Point points} and {@link Stick sticks}.
     * @param additionalSegments
     */
    public void addAdditionalSegments(int additionalSegments) {
        Point previous = points.getLast();
        boolean wasPinned = false;
        if(previous.pinned) {
            previous.unpin();
            wasPinned = true;
        }
        int totalSegments = points.size() + additionalSegments;
        for(int x = points.size(); x < totalSegments; x++) {
            // the new point gets placed between the current and last point
            Point newPoint = new Point(previous.pos.lerp(offset, 0.5f, new Vector3f()));
            points.add(x, newPoint);
            sticks.add(x - 1, new Stick(previous, newPoint));
            previous = newPoint;
        }
        if(wasPinned)
            points.getLast().pin();
    }

    @Override
    public void calculateSegmentation() {

        this.length = offset.length();
        if(!isInitialized()) return;
        this.points.getFirst().clearPos();
        int segmentCount = getSegmentCount();

        // grow or shrink the wire as needed
        if(segmentCount < this.points.size()) {
            boolean wasPinned = this.points.getLast().pinned;
            this.points.removeElements(segmentCount, this.points.size());
            this.sticks.removeElements(segmentCount - 1, this.sticks.size());
            if(wasPinned) this.points.getLast().pin();
            this.points.trim();
            this.sticks.trim();
        } else if(segmentCount > this.points.size()) {
            this.points.ensureCapacity(segmentCount);
            this.sticks.ensureCapacity(segmentCount - 1);
            addAdditionalSegments(segmentCount - this.points.size());
        }
        this.points.getLast().setPos(offset);
    }

    @Override
    public boolean isInitialized() {
        return this.points != null && this.sticks != null;
    }


    @Override
    public SimulatedWireModel setOffset(Vector3f offset) {
        if(this.offset == null)
            this.offset = new Vector3f(offset.x, offset.y, offset.z);
        else this.offset.set(offset);
        calculateSegmentation();
        return this;
    }

    @Override
    public SimulatedWireModel setOffset(Vec3 start, Vec3 end) {
        if(this.offset == null)
            this.offset = new Vector3f();
        this.offset.set((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z));
        calculateSegmentation();
        return this;
    }

    /**
     * A call to this method represents a sigular update
     * of a discrete Verlet Integration simulation. 
     * This wire will seek a state of minimal potential energy
     * by applying gravity and inertia.
     * <p>
     * Note that this simulation 
     * accumulates a realistic result over time, and so needs 
     * to be called several times for results to display 
     * immediately. See  {@link #updateAhead} to automatically
     * call call this method multiple times.
     * @throws IllegalStateException if this WireModel doesn't have {@link #setOffset an offset} or hasn't been {@link #initialize initialized} at least once.
    */
    @Override
    public void update() {
        assertHasOffset();
        assertSimulatable();

        final Vector3f gravity = getGravity(points.size());
        final Vector3d lastPos = new Vector3d();

        // apply velocity and gravity
        avgVelocity = 0;
        for(Point point : points) {
            if(point.pinned) continue;
            lastPos.set(point.pos);
            Vector3f vel = point.pos.sub(point.lastPos, new Vector3f());
            avgVelocity += vel.length();
            point.pos.add(vel);
            point.pos.sub(gravity);
            point.lastPos.set(lastPos);
        }

        avgVelocity /= points.size();
        if(Float.isNaN(avgVelocity)) {
            Mechano.LOGGER.warn("Instability detected in " + this.toFullString() + " - The simulation has been reset");
            initialize();
            return;
        }

        float lengthAdj = getLengthAdjustment(sticks.size());
        for(int x = 0; x < CatenaryAttributes.SOLVER_STEPS; x++) {
            for(int s = sticks.size() - 1; s >= 0; s--) {
                Stick stick = sticks.get(s);
                stick.computeForward();
                stick.computeCenter();
                if(!stick.start.pinned) stick.start.pos.set(stick.center).add(stick.facing.x * lengthAdj, stick.facing.y * lengthAdj, stick.facing.z * lengthAdj);
                if(!stick.end.pinned) stick.end.pos.set(stick.center).sub(stick.facing.x * lengthAdj, stick.facing.y * lengthAdj, stick.facing.z * lengthAdj);
            }
        }
    }

    @Override
    public void render(VertexConsumer buffer, Pose pose, CatenaryGeometry geo, float pTicks) {
        if(sticks.size() < 2) {
            Mechano.LOGGER.error("Attempted to render SimulatedWireModel with invalid (< 2) size!");
            return;
        }

        Stick previous = sticks.getFirst();
        geo.light[0] = geo.getLight(previous.start.pos);
        geo.light[1] = geo.getLight(previous.end.pos);
        geo.model.profile.make(buffer, pose, geo, null, previous, sticks.get(1), 0, true);
        for(int x = 1; x < sticks.size() - 1; x++) {
            Stick current = sticks.get(x);
            geo.light[1] = geo.getLight(current.start.pos);
            geo.model.profile.make(buffer, pose, geo, previous, current, sticks.get(x + 1), x, true);
            previous = current;
            geo.light[0] = geo.light[1];
        }

        Stick last = sticks.getLast();
        geo.light[1] = geo.getLight(last.end.pos);
        geo.model.profile.make(buffer, pose, geo, previous, last, null, sticks.size(), true);
    }

    @Override
    public void drawDebug(Vec3 basis) {
        if(sticks == null) return;
        for(int x = 0; x < sticks.size(); x++) {
            Stick stick = sticks.get(x);
            if(stick == null) continue;
            VectorHelper.drawDebugBox(basis.add(stick.end.pos.x, stick.end.pos.y, stick.end.pos.z), 0.007f, Color.BLACK, "sim_point_" + x);
            Outliner.getInstance().showLine("sim_stick_" + x, 
                basis.add(stick.start.pos.x, stick.start.pos.y, stick.start.pos.z), 
                basis.add(stick.end.pos.x, stick.end.pos.y, stick.end.pos.z))
                    .lineWidth(0.02f).disableCull().colored(Color.GREEN);
        }
    }

    @Override
    public String toString() {
        if(sticks == null) return "SimulatedWireModel[UNINITIALIZED]";
        String out = "\nSimulatedWireModel[\n";
        for(int x = 0; x < sticks.size(); x++) {
            Stick stick = sticks.get(x);
            if(stick == null) {
                out += "\t" + x + ": (NULL)\n";
                continue;
            }
            out += "\t" + x + ": " + stick + "\n";
        }
        return out + "]";
    }

    public String toFullString() {
        String out = this.toString() + ", ";
        out += "\nPoints: " + this.points.toString();
        out += "\nSticks:" + this.sticks.toString();
        return out;
    }

    // sanity checks for baked vs realtime state
    private void assertSimulatable() {
        if(points == null || sticks == null)
            throw new IllegalStateException("Cannot integrate " + this + " - This WireModel has not been initialized!");
    }

    @Override
    public SimulatedWireModel toSimulated(boolean pinEnds) {
        Mechano.LOGGER.warn("Attempted to convert a SimulatedWireModel to itself!");
        return this;
    }
}