package com.quattage.mechano.foundation.api.catenary;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;


import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.phys.Vec3;

public class RealtimeWireModel extends WireModel<RealtimeWireModel> {

    public float spannedDistance = 0;
    public float avgVelocity = 0;

    private @Nullable ObjectArrayList<Point> points;
    private @Nullable ObjectArrayList<Stick> sticks;

    public RealtimeWireModel() {}

    /**
     * Initializes a Verlet Integration simulation for this WireModel. Calls to this method
     * populate this WireModel's internal points array to the size necessary to span from
     * {@link #start start} to {@link #offset end}. If this WireModel were to be baked
     * after calling this method, the resulting wire would form a perfectly straight line.
     * <p>
     * Additionally, if any pre-existing baked vertex data exists for this WireModel, 
     * that data will be cleared. This WireModel will return to its unbaked, simulatable state.
     * @return This WireModel for chaining calls
     */
    public RealtimeWireModel initialize() {
        assertHasOffset();
        int pointCount = getPointCount();
        this.points = new ObjectArrayList<Point>(pointCount);
        this.sticks = new ObjectArrayList<Stick>(pointCount - 1);
        addSegments(pointCount, true);
        return this;
    }


    // creates perfectly straight line from the start to the end
    private void addSegments(int segments, boolean pinEnds) {
        Point previous = null;
        Vector3f trgt = new Vector3f();
        for(int x = 0; x <= segments; x++) {
            float spanProgress = ((float)x / (float)segments);
            Point newPoint = new Point(quicklerp(trgt, spanProgress));
            points.add(x, newPoint);
            if(previous != null)
                sticks.add(x - 1, new Stick(previous, newPoint));
            previous = newPoint;
        }
        if(pinEnds) {
            points.getFirst().pin();
            points.getLast().pin();
        }
    }

    private void addAdditionalSegments(int additionalSegments) {
        Point previous = points.getLast();
        boolean wasPinned = false;
        if(previous.pinned) {
            previous.unpin();
            wasPinned = true;
        }
        int totalSegments = points.size() + additionalSegments;
        for(int x = points.size(); x <= totalSegments; x++) {
            // the new point gets placed between the current and last point
            Point newPoint = new Point(previous.pos.lerp(offset, 0.5f, new Vector3f()));
            points.add(x, newPoint);
            sticks.add(x - 1, new Stick(previous, newPoint));
            previous = newPoint;
        }
        if(wasPinned)
            points.getLast().pin();
    }

    /**
     * A method useful for reminding this WireModel to update its length.
     * Called automatically whenever this WireModel is {@link #moveTo moved.}
     */
    public void calculateSegmentation() {
        this.spannedDistance = offset.length();

        if(!isInitialized()) return;

        this.points.getFirst().clearPos();
        int pointCount = getPointCount();

        // grow or shrink the wire as needed
        if(pointCount < this.points.size()) {
            boolean wasPinned = this.points.getLast().pinned;
            this.points.removeElements(pointCount, this.points.size());
            this.sticks.removeElements(pointCount - 1, this.sticks.size());
            if(wasPinned) this.points.getLast().pin();
            this.points.trim();
            this.sticks.trim();
        } else if(pointCount > this.points.size()) {
            this.points.ensureCapacity(pointCount);
            this.sticks.ensureCapacity(pointCount - 1);
            addAdditionalSegments(pointCount - this.points.size());
        }
        this.points.getLast().setPos(offset);
    }

    @Override
    public boolean isInitialized() {
        return this.points != null && this.sticks != null;
    }


    @Override
    public RealtimeWireModel setOffset(Vector3f offset) {
        if(this.offset == null)
            this.offset = new Vector3f(offset.x, offset.y, offset.z);
        else this.offset.set(offset);
        calculateSegmentation();
        return this;
    }

    @Override
    public RealtimeWireModel setOffset(Vec3 start, Vec3 end) {
        if(this.offset == null)
            this.offset = new Vector3f();
        this.offset.set((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z));
        calculateSegmentation();
        return this;
    }

    /**
     * Runs {@link #simulate} multiple times to deform this WireModel into an approximate catenary shape.
     * @param steps (Optional) The amount of simulation steps to perform. Or provide no value to use the {@link #CATENARY_SOLVER_ITERATIONS default}
     */
    public void simulateAhead() {
        simulateAhead(CATENARY_SOLVER_ITERATIONS);
    }


    /**
     * Runs {@link #simulate} multiple times to deform this WireModel into an approximate catenary shape.
     * @param steps (Optional) The amount of simulation steps to perform. Or provide no value to use the {@link #CATENARY_SOLVER_ITERATIONS default}
     */
    public void simulateAhead(int steps) {
        for(int x = 0; x < steps; x++)
            simulate();
    }

    /**
     * A call to this method represents a sigular update
     * of a discrete Verlet Integration simulation. This 
     * method can be called to accumulate gravity on a wire 
     * over time. 
     * In order for this method to function as expected, 
     * You need to first call {@link #moveTo} to
     * set this WireModel's start and end positions,
     * as well as a call to {@link #initializeSimulation()},
     * to initially construct the wire itself.
     * @throws IllegalStateException if this WireModel hasn't been moved or initialized at least once.
    */
    public void simulate() {
        assertHasOffset();
        assertIntegrateCompatable();

        final Vector3f gravity = new Vector3f(0, CATENARY_POINT_MASS / (float)points.size(), 0);
        final Vector3d lastPos = new Vector3d();

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

        // if(Float.isNaN(avgVelocity)) {
        //     Mechano.LOGGER.warn("Instability detected in " + this.toFullString() + " - The simulation has been reset");
        //     initializeSimulation();
        //     return;
        // }

        float segmentLength = getSegmentLength();
        for(int x = 0; x < CATENARY_SOLVER_ITERATIONS; x++) {
            for(Stick stick : sticks) {
                Vector3f center = stick.getCenter();
                Vector3f facing = stick.getFacingVector();
                Vector3f nudge = new Vector3f(facing.x * segmentLength / 2f, facing.y * segmentLength / 2f, facing.z * segmentLength / 2f);
                if(!stick.start.pinned)
                    stick.start.pos.set(center).add(nudge);
                if(!stick.end.pinned)
                    stick.end.pos.set(center).sub(nudge);
            }
        }

        // Mechano.LOGGER.info("P: " + points.size() + " S: " + sticks.size() + " VEL: " + avgVelocity);
    }

    @Override
    public void render() {
        throw new UnsupportedOperationException("Unimplemented method 'render'");
    }

    /*
     * The amount of points in this wire is determined by the distance spanned, which, in turn
     * determines the length of each uniform segment
     */
    private int getPointCount() {
        return Math.max(CATENARY_POINT_MINIMUM, Math.min(CATENARY_POINT_MAXIMUM, (int)(spannedDistance * CATENARY_POINT_RESOLUTION)));
    }

    /*
     * The segment length is uniform for every Stick across the wire and 
     * is based directly on how long the wire itself is.
     * We also assume that there's a tiny amount of extra length to prevent
     * the wire from getting too tight and freaking out in certain situations
     */
    private float getSegmentLength() {
        return Math.max(0.015f, (spannedDistance / sticks.size()) + (1 - tension) + CATENARY_TENSION_EPSILON);
    }


    private void assertHasOffset() {
        if(offset == null)
            throw new IllegalStateException("Cannot perform operation on " + this + " - This WireModel is missing a start or end position! (It was either never populated or this WireModel instance was destroyed.)");
    }


    // sanity checks for baked vs realtime state
    private void assertIntegrateCompatable() {
        if(points == null || sticks == null)
            throw new IllegalStateException("Cannot integrate " + this + " - This WireModel has not been initialized!");
    }


    public String toFullString() {
        String out = this.toString() + ", ";
        out += "\nPoints: " + this.points.toString();
        out += "\nSticks:" + this.sticks.toString();
        return out;
    }



    public void forEachPoint(Consumer<Point> cons) {
        if(points == null) return;
        for(int x = 0; x < points.size(); x++) {
            Point point = points.get(x);
            if(point == null) continue;
            cons.accept(point);
        }
    }

    public void forEachStick(Consumer<Stick> cons) {
        if(sticks == null) return;
        for(int x = 0; x < sticks.size(); x++) {
            Stick stick = sticks.get(x);
            if(stick == null) continue;
            cons.accept(stick);
        }
    }

    @Override
    public void drawDebug(Vec3 basis) {
        if(sticks == null) return;
        for(int x = 0; x < sticks.size(); x++) {
            Stick s = sticks.get(x);
            if(s == null) continue;
            Outliner.getInstance().showLine("stick_" + x, basis.add(s.start.pos.x, s.start.pos.y, s.start.pos.z), basis.add(s.end.pos.x, s.end.pos.y, s.end.pos.z)).lineWidth(0.02f).disableCull().colored(Color.GREEN);
        }
    }

    @Override
    public String toString() {
        return "WireModel[" + offset + ", " + spannedDistance + "m]";
    }



























    private static enum SkewVariant {
        A(0, 3), B(3, 6);
        public final Vector2i coords;
        private SkewVariant(int u0, int u1) {
            this.coords = new Vector2i(u0, u1);
        }
    }



























    



























}
