package com.quattage.mechano.foundation.api.catenary;

import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes.Point;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes.Stick;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.helper.Duo;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class SimulatedCatenary extends CatenaryModel<SimulatedCatenary> {

    private @Nullable Vector2f wind = null;
    protected @Nullable ObjectArrayList<Point> points;
    protected @Nullable ObjectArrayList<Stick> sticks;
    private final RestitutionTracker tracker = new RestitutionTracker(1e-8f);
    private float uniformLength = 0f;

    public SimulatedCatenary() {}

    @Override
    public SimulatedCatenary setOffset(Vec3 start, Vec3 end) {
        if(this.halfOffset == null) {
            this.halfOffset = new Vector3f(
                (float)start.x - ((float)(start.x + end.x)) / 2f, 
                (float)start.y - ((float)(start.y + end.y)) / 2f,
                (float)start.z - ((float)(start.z + end.z)) / 2f
            );
            this.length = halfOffset.length() * 2f;
            calculateSegmentation(); 
            return this;
        }
        Vector3f oldOffset = new Vector3f(halfOffset);
        this.halfOffset.set(
            start.x - ((float)(start.x + end.x)) / 2f, 
            start.y - ((float)(start.y + end.y)) / 2f, 
            start.z - ((float)(start.z + end.z)) / 2f
        );
        this.length = halfOffset.length() * 2f;
        calculateSegmentation(); 
        if(Math.abs(oldOffset.dot(halfOffset)) > 35f)
            flipSpan();
        return this;
    }

    @Override
    public SimulatedCatenary setOrderedOffset(LevelReader world, GridUUID start, GridUUID end, float pTicks) {
        Duo<GridUUID> ordered = TrackedStreamable.orderedByAssertionPriority(world, start, end);
        setOffset(ordered.first().getPos(world, pTicks), ordered.second().getPos(world, pTicks));
        return this;
    }

    @Override
    public SimulatedCatenary setOrderedOffset(LevelReader world, AnchorPoint start, AnchorPoint end, float pTicks) {
        Duo<AnchorPoint> ordered = TrackedStreamable.orderedByAssertionPriority(world, start, end);
        setOffset(ordered.first().getPos(world, pTicks), ordered.second().getPos(world, pTicks));
        return this;
    }

    /**
     * Initializes a Verlet Integration simulation for this Catenary. Calls to this 
     * method populate this Catenary's internal points array to the size necessary 
     * to span across this wire's {@link #offset magnitude vector.} As a result of 
     * this call, each {@link Point} and {@link Stick} is aligned in a straight 
     * line. No offsets or simulated constraints are applied until at least one call 
     * to {@link #update} is made.
     * <p>
     * Additionally, if any pre-existing vertex data exists for this Catenary, 
     * that data will be cleared. This Catenary will return to its initial state.
     * @return This Catenary for chaining calls
     */
    @Override
    public SimulatedCatenary initializeSpan() {
        assertHasOffset();
        tracker.reset();
        int segments = getSegmentCount();
        if(segments == 0) return this;
        this.points = new ObjectArrayList<Point>(segments + 1);
        this.sticks = new ObjectArrayList<Stick>(segments);
        for(int x = 0; x < segments + 1; x++) {
            float spanProgress = (float)x / (float)segments;
            Point newPoint = new Point(new Vector3f(-halfOffset.x, -halfOffset.y, -halfOffset.z).mul(2f * spanProgress - 1f));
            points.add(x, newPoint);
            if(x > 0) sticks.add(new Stick(points.get(x - 1), newPoint));
        }
        return this;
    }

    public void flipSpan() {
        assertHasOffset();
        assertInitialized();
        for(Point p : points) {
            p.pos.mul(-1);
            p.lastPos.mul(-1);
        }
    }

    @Override
    public SimulatedCatenary calculateSegmentation() {
        int segmentCount = getSegmentCount();
        if(!isInitialized()) {
            return this;
        }
        if(segmentCount < this.points.size()) {
            boolean wasPinned = this.points.getLast().pinned;
            this.points.removeElements(segmentCount, this.points.size());
            this.sticks.removeElements(segmentCount - 1, this.sticks.size());
            if(wasPinned) {
                Point p = this.points.getLast();
                p.pinned = true;
                p.pos.set(-halfOffset.x, -halfOffset.y, -halfOffset.z);
                p.lastPos.set(p.pos);
            }
            this.points.trim();
            this.sticks.trim();
        } else if(segmentCount > this.points.size() && length < maxLength) {
            this.points.ensureCapacity(segmentCount);
            this.sticks.ensureCapacity(segmentCount - 1);
            addAdditionalSegments(segmentCount - this.points.size());
        }
        this.uniformLength = (length / (float)sticks.size()) * 0.99f;
        return this;
    }

    /**
     * Extends this wire by the given amount of segments
     * while retaining velocity data for all pre-existing 
     * {@link Point points} and {@link Stick sticks}.
     * This method is called internally
     * by {@link #calculateSegmentation}, which will automatically
     * grow/shrunk this SimulatedCatenary to span the length required.
     * @param additionalSegments
     */
    public void addAdditionalSegments(int additionalSegments) {
        Point previous = points.getLast();
        boolean wasPinned = false;
        if(previous.pinned) {
            previous.pinned = false;
            wasPinned = true;
        }
        int totalSegments = points.size() + additionalSegments;
        for(int x = points.size(); x < totalSegments; x++) {
            // the new point gets placed between the current and last point
            Point newPoint = new Point(-halfOffset.x, -halfOffset.y, -halfOffset.z);
            points.add(x, newPoint);
            sticks.add(x - 1, new Stick(previous, newPoint));
            previous = newPoint;
        }
        if(wasPinned)
            points.getLast().pinned = true;
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
     * @throws IllegalStateException if this Catenary doesn't have {@link #setOffset an offset} or hasn't been {@link #initialize initialized} at least once.
    */
    @Override
    public void update() {
        assertHasOffset();
        assertSimulatable();
        resetIfUnstable();
        tracker.softReset();
        updateEndpoints();

        final Vector3f gravity = getGravity(points.size());
        if(WindManager.INSTANCE.isEnabled() && wind != null) 
            integrateVelocity(gravity, wind);
        else integrateVelocity(gravity);

        float error = 0;
        for(int iter = 0; iter < CatenaryAttributes.SOLVER_STEPS; iter++) {
            for(Stick stick : sticks) {
                Vector3f center = stick.getCenter();
                Vector3f dir = stick.getDir();
                float initialLength = dir.length();
                dir.normalize();
                if(!stick.start.pinned) {
                    stick.start.pos.set(
                        center.x + dir.x * uniformLength / 2f,
                        center.y + dir.y * uniformLength / 2f,
                        center.z + dir.z * uniformLength / 2f
                    );
                } if(!stick.end.pinned) {
                    stick.end.pos.set(
                        center.x - dir.x * uniformLength / 2f,
                        center.y - dir.y * uniformLength / 2f,
                        center.z - dir.z * uniformLength / 2f
                    );
                }
                error += Math.abs(stick.getLength() - initialLength);
            }
        }
        tracker.walk(error, points.size());
    }

    /**
     * A single step of the verlet integration algorithm
     * https://en.wikipedia.org/wiki/Verlet_integration
     * Optionally includes an arbitrary wind vector for adding
     * additional dynamism.
     * @param vec Working vector passed here to avoid continuous re-declaration
     * @param gravity Gravitational force to apply (see {@link #getGravity})
     * @param wind (Optional) An additional, arbitrary force resembling wind
     */
    public void integrateVelocity(Vector3f gravity, Vector2f wind) {
        float mid = points.size() / 2f;
        for(int x = 0; x < points.size(); x++) {
            Point point = points.get(x);
            if(point.pinned) continue;
            Vector3f vel = point.pos.sub(point.lastPos, new Vector3f());
            vel.sub(gravity);
            float windStrength = (1 - (((float)x - mid ) / mid));
            vel.add(wind.x * windStrength, 0, wind.y * windStrength);
            point.lastPos.set(point.pos);
            point.pos.add(vel);
            tracker.apply(vel);
        }
    }

    /**
     * A single step of the verlet integration algorithm
     * https://en.wikipedia.org/wiki/Verlet_integration
     * Optionally includes an arbitrary wind vector for adding
     * additional dynamism.
     * @param vec Working vector passed here to avoid continuous re-declaration
     * @param gravity Gravitational force to apply (see {@link #getGravity})
     * @param wind (Optional) An additional, arbitrary force resembling wind
     */
    public void integrateVelocity(Vector3f gravity) {
        for(Point point : points) {
            if(point.pinned) continue;
            Vector3f vel = point.pos.sub(point.lastPos, new Vector3f());
            vel.sub(gravity);
            point.lastPos.set(point.pos);
            point.pos.add(vel);
            tracker.apply(vel);
        }
    }   

    @Override
    public SimulatedCatenary render(VertexConsumer buffer, Pose pose, CatenaryMesher geo, float pTicks) {
        if(sticks.size() < 2) {
            Mechano.LOGGER.error("Attempted to render SimulatedCatenary with invalid (< 2) size!");
            return this;
        }
        Stick previous = sticks.getFirst();
        geo.setLight0(geo.getLight(previous.start.pos));
        geo.setLight1(geo.getLight(previous.end.pos));
        float arclength = 0;
        geo.model.extruder.make(buffer, pose, geo, null, previous, sticks.get(1), arclength, true, pTicks);
        for(int x = 1; x < sticks.size() - 1; x++) {
            Stick current = sticks.get(x);
            arclength += current.getLength();
            geo.setLight1(geo.getLight(current.start.pos));
            geo.model.extruder.make(buffer, pose, geo, previous, current, sticks.get(x + 1), arclength, true, pTicks);
            previous = current;
            geo.walkLight();
        }
        Stick last = sticks.getLast();
        geo.setLight1(geo.getLight(last.end.pos));
        geo.model.extruder.make(buffer, pose, geo, previous, last, null, arclength, true, pTicks);
        return this;
    }

    @Override
    public SimulatedCatenary pinEndpoints() {
        if(this.points == null) return this;
        Point p = this.points.getFirst();
        if(p != null) {
            p.lastPos.set(p.pos);
            p.pos.set(halfOffset.x, halfOffset.y, halfOffset.z);
            p.pinned = true;
        }
        p = this.points.getLast();
        if(p != null) {
            p.lastPos.set(p.pos);
            p.pos.set(-halfOffset.x, -halfOffset.y, -halfOffset.z);
            p.pinned = true;
        }
        return this;
    }

    @Override
    public SimulatedCatenary unpinEndpoints() {
        if(this.points == null) return this;
        Point p = this.points.getFirst();
        if(p != null) {
            p.pos.set(halfOffset.x, halfOffset.y, halfOffset.z);
            p.pinned = false;
        }
        p = this.points.getLast();
        if(p != null) {
            p.pos.set(-halfOffset.x, -halfOffset.y, -halfOffset.z);
            p.pinned = false;
        }
        return this;
    }

    public SimulatedCatenary setOffsetContinuous(LevelReader world, Vec3 startPos, Vec3 worldMid) {
        if(this.halfOffset == null) this.halfOffset = new Vector3f();
        this.halfOffset.set(startPos.x - worldMid.x, startPos.y - worldMid.y, startPos.z - worldMid.z);
        this.length = halfOffset.length() * 2f;
        updateEndpoints();
        return this;
    }

    @Override
    public SimulatedCatenary updateEndpoints() {
        if(this.points == null) return this;
        Point p = points.getFirst();
        if(p.pinned) {
            p.lastPos.set(p.pos);
            p.pos.set(halfOffset.x, halfOffset.y, halfOffset.z);
        }
        p = points.getLast();
        if(p.pinned) {
            p.lastPos.set(p.pos);
            p.pos.set(-halfOffset.x, -halfOffset.y, -halfOffset.z);
        }
        return this;
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

    private void resetIfUnstable() {
        if(points == null || !tracker.isCascading()) return;
        Mechano.LOGGER.warn("Cascading instability detected in " + this);
        ArrayList<@Nullable Vector3f> pins = new ArrayList<>(points.size());
        for(Point p : points) {
            if(p == null || !p.pinned) 
                pins.add(null);
            else pins.add(p.pos);
        }
        initializeSpan();
        calculateSegmentation();
        for(int x = 0; x < points.size(); x++) {
            if(x >= pins.size()) return;
            Vector3f pinnedPos = pins.get(x);
            if(pins.get(x) == null) return;
            Point p = points.get(x);
            p.pos.set(pinnedPos);
            p.lastPos.set(p.pos);
            p.pinned = true;
        }
    }

    /**
     * Apply an upward force to the middle of the wire
     * to add some extra visual interest when the wire is 
     * created.
     */
    public void kick(float strength) {
        assertInitialized();
        float mid = points.size() / 2f;
        for(int x = 0; x < points.size(); x++) {
            Point point = points.get(x);
            if(point.pinned) continue;
            float magnitude = (1 - (((float)x - mid ) / mid));
            point.lastPos.set(point.pos);
            point.pos.add(0, strength * magnitude, 0);
        }
    }

    @Override
    public boolean isResting() {
        if(wind != null && wind.length() > 0.01f) return false;
        return tracker.isResting();
    }

    @Override
    public void updateAhead(int steps) {
        disableWind();
        for(int x = 0; x < steps; x++) {
            update();
            if(isResting()) return;
        }
        float arclength = 0;
        for(Stick s : sticks) arclength += s.getLength();
        Mechano.LOGGER.warn("Catenary simulation (" + length + " meters, " + arclength + " arcmeters) couldn't reach a state of restitution in " + steps + " iterations.");
    }

    @Override
    public String toString() {
        if(sticks == null) return "SimulatedCatenary[UNINITIALIZED]";
        String out = "\nSimulatedCatenary[\n";
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

    private void assertSimulatable() {
        if(points == null || sticks == null)
            throw new IllegalStateException("Cannot integrate " + this + " - This Catenary has not been initialized!");
    }

    public void applyWind(@Nullable Vector2f wind) {
        this.wind = wind;
    }

    public void disableWind() {
        this.wind = null;
    }

    @Override
    public boolean isInitialized() {
        return this.points != null && this.sticks != null;
    }

    @Override
    public void adjustSpan(LevelReader world, float length) {
        this.maxLength = length;
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
    public void destroy() {
        this.points = null;
        this.sticks = null;
    }
}