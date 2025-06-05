package com.quattage.mechano.foundation.catenary;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.phys.Vec3;

public class SimulatedWireModel extends WireModel<SimulatedWireModel> {

    // average accumulated velocity as of the last time the wire was simulated
    public float avgVelocity = 0;

    private @Nullable ObjectArrayList<Point> points;
    private @Nullable ObjectArrayList<Stick> sticks;

    public SimulatedWireModel() {}

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
    public SimulatedWireModel initialize() {
        assertHasOffset();
        int segmentCount = getSegmentCount();
        this.points = new ObjectArrayList<Point>(segmentCount + 1);
        this.sticks = new ObjectArrayList<Stick>(segmentCount);
        addSegments(segmentCount, true);
        return this;
    }


    // creates perfectly straight line from the start to the end
    private void addSegments(int segments, boolean pinEnds) {
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
     * In order for this method to function as expected, 
     * You need to first call {@link #moveTo} to
     * set this WireModel's start and end positions,
     * as well as a call to {@link #initializeSimulation()},
     * to initially construct the wire itself.
     * @throws IllegalStateException if this WireModel hasn't been moved or initialized at least once.
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
        Vector3f facing = new Vector3f();
        Vector3f center = new Vector3f();

        for(int x = 0; x < CATENARY_SOLVER_ITERATIONS; x++) {
            for(int s = sticks.size() - 1; s >= 0; s--) {
                Stick stick = sticks.get(s);
                facing.set(stick.getAsRay()).normalize();
                center.set(stick.getCenter());
                if(!stick.start.pinned) stick.start.pos.set(center).add(facing.x * lengthAdj, facing.y * lengthAdj, facing.z * lengthAdj);
                if(!stick.end.pinned) stick.end.pos.set(center).sub(facing.x * lengthAdj, facing.y * lengthAdj, facing.z * lengthAdj);
            }
        }
    }

    @Override
    public void render() {
        throw new UnsupportedOperationException("Unimplemented method 'render'");
    }


    // sanity checks for baked vs realtime state
    private void assertSimulatable() {
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
            Stick stick = sticks.get(x);
            if(stick == null) continue;
            VectorHelper.drawDebugBox(basis.add(stick.end.pos.x, stick.end.pos.y, stick.end.pos.z), 0.007f, Color.BLACK, "sim_point_" + x);
            Outliner.getInstance().showLine("sim_stick_" + x, basis.add(stick.start.pos.x, stick.start.pos.y, stick.start.pos.z), basis.add(stick.end.pos.x, stick.end.pos.y, stick.end.pos.z)).lineWidth(0.02f).disableCull().colored(Color.GREEN);
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



























    private static enum SkewVariant {
        A(0, 3), B(3, 6);
        public final Vector2i coords;
        private SkewVariant(int u0, int u1) {
            this.coords = new Vector2i(u0, u1);
        }
    }



























    



























}
