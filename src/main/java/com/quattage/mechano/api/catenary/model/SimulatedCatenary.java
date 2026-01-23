package com.quattage.mechano.api.catenary.model;

import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.catenary.Catenaries;
import com.quattage.mechano.api.catenary.Catenaries.Point;
import com.quattage.mechano.api.catenary.Catenaries.Stick;
import com.quattage.mechano.api.catenary.CatenaryMeshBuffer;
import com.quattage.mechano.api.catenary.EntropyTracker;
import com.quattage.mechano.api.catenary.MeshExtruder;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.numeric.VectorOperations;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class SimulatedCatenary extends CatenaryModel<SimulatedCatenary> {

    protected @Nullable ObjectArrayList<Point> points;
    protected @Nullable ObjectArrayList<Stick> sticks;
    private final EntropyTracker tracker = new EntropyTracker();
    private float[] forces;
    private boolean locked = false;

    public SimulatedCatenary() {
        forces = new float[12];
    }

    @Override
    public SimulatedCatenary setOffset(TransmitterType trns, Vector3d start, Vector3d end) {
        if(start == null || end == null) return this;
        applyDisplacement(start, end);
        if(this.halfOffset == null)
            this.halfOffset = new Vector3f();
        this.halfOffset.set(    
            (float)(start.x - forces[1]),
            (float)(start.y - forces[2]),
            (float)(start.z - forces[3])
        );
        if(!locked) {
            this.span = halfOffset.length() * 2f;
            calculateSegmentation(trns); 
        }
        return this;
    }

    private void applyDisplacement(Vector3d start, Vector3d end) {
        this.forces[4] = (float)start.x - (forces[1] + halfOffset.x);
        this.forces[5] = (float)start.y - (forces[2] + halfOffset.y);
        this.forces[6] = (float)start.z - (forces[3] + halfOffset.z);
        this.forces[7] = (float)end.x - (forces[1] - halfOffset.x);
        this.forces[8] = (float)end.y - (forces[2] - halfOffset.y);
        this.forces[9] = (float)end.z - (forces[3] - halfOffset.z);
        this.forces[1] = (float)((start.x + end.x) / 2f);
        this.forces[2] = (float)((start.y + end.y) / 2f);
        this.forces[3] = (float)((start.z + end.z) / 2f);
    }

    public SimulatedCatenary setOffsetContinuous(LevelReader world, Vec3 startPos, Vec3 endPos, Vec3 worldMid) {
        if(startPos == null || endPos == null) return this;
        if(this.halfOffset == null) this.halfOffset = new Vector3f();
        this.halfOffset.set(startPos.x - worldMid.x, startPos.y - worldMid.y, startPos.z - worldMid.z);
        updateEndpoints();
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
        int segments = Catenaries.renderPipeline().getSegmentCount(span);  
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
    public SimulatedCatenary calculateSegmentation(TransmitterType trns) {
        if(!isInitialized()) return this;
        int segmentCount = Catenaries.renderPipeline().getSegmentCount(span);        
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
        } else if(segmentCount > this.points.size() && span < trns.getMaximumSpan()) {
            this.points.ensureCapacity(segmentCount);
            this.sticks.ensureCapacity(segmentCount - 1);
            addAdditionalSegments(segmentCount - this.points.size());
        }
        if(span < trns.getMaximumSpan())
            this.forces[0] = (span / (float)sticks.size()) * 0.99f;
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
    public void update(TransmitterType trns) {
        assertHasOffset();
        assertSimulatable();
        resetIfUnstable(trns);
        tracker.softReset();
        updateEndpoints();
        integrateVelocity(Catenaries.renderPipeline().getGravity(points.size()), this.forces[10], this.forces[11]);

        // TODO compute shader?
        float error = 0;
        for(int iter = 0; iter < Catenaries.renderPipeline().getSolverSteps(); iter++) {
            for(Stick stick : sticks) {
                Vector3f center = stick.getCenter();
                Vector3f dir = stick.getDir();
                float initialLength = dir.length();
                dir.normalize();
                float half = this.forces[0] / 2f;
                if(!stick.start.pinned) {
                    stick.start.pos.set(
                        center.x + dir.x * half,
                        center.y + dir.y * half,
                        center.z + dir.z * half
                    );
                } if(!stick.end.pinned) {
                    stick.end.pos.set(
                        center.x - dir.x * half,
                        center.y - dir.y * half,
                        center.z - dir.z * half
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
     * @param gravity Gravitational force to apply (see {@link #getGravity})
     * @param windX  average X value for wind forces applied across all points
     * @param windY average Y value for wind forces applied across all points
     */
    public void integrateVelocity(Vector3f gravity, float windX, float windY) {
        int s = points.size();
        float mid = (float)s / 2f;
        for(int x = 0; x < points.size(); x++) {
            Point point = points.get(x);
            if(point.pinned) continue;
            Vector3f vel = point.pos.sub(point.lastPos, new Vector3f());
            vel.sub(gravity);
            float tS = (float)x / (float)s;
            float tC = 1 - (((float)x - mid ) / mid);
            vel.add(windX * tC, 0, windY * tC);
            vel.sub(
                (Mth.lerp(tS, forces[4], forces[7]) * 3) / s,
                (Mth.lerp(tS, forces[5], forces[8]) * 3) / s,
                (Mth.lerp(tS, forces[6], forces[9]) * 3) / s
            );
            point.lastPos.set(point.pos);
            point.pos.add(vel);

            tracker.apply(vel);
        }
    }

    @Override
    public SimulatedCatenary render(VertexConsumer buffer, Pose pose, CatenaryMeshBuffer geo, float pTicks) {
        if(!geo.getCurrentlyBoundType().getRenderProperties().isVisible()) {
            Mechano.LOGGER.warn("Attempted to render a CatenaryModel for non-renderable type '" + geo.getCurrentlyBoundType() + "'");
            return this;
        }
        if(sticks.size() < 2) {
            Mechano.LOGGER.error("Attempted to render a CatenaryModel with invalid (< 2) size!");
            return this;
        }
        Stick previous = sticks.getFirst();
        geo.setLight0(geo.getLight(previous.start.pos));
        geo.setLight1(geo.getLight(previous.end.pos));
        float arclength = 0;
        MeshExtruder extr = geo.getCurrentlyBoundType().getRenderProperties().getExtruder();
        extr.extrude(buffer, pose, geo, null, previous, sticks.get(1), arclength, true, pTicks);
        for(int x = 1; x < sticks.size() - 1; x++) {
            Stick current = sticks.get(x);
            arclength += current.getLength();
            geo.setLight1(geo.getLight(current.start.pos));
            extr.extrude(buffer, pose, geo, previous, current, sticks.get(x + 1), arclength, true, pTicks);
            previous = current;
            geo.walkLight();
        }
        Stick last = sticks.getLast();
        geo.setLight1(geo.getLight(last.end.pos));
        extr.extrude(buffer, pose, geo, previous, last, null, arclength, true, pTicks);
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
            VectorOperations.drawDebugBox(basis.add(stick.end.pos.x, stick.end.pos.y, stick.end.pos.z), 0.007f, Color.BLACK, "sim_point_" + x);
            Outliner.getInstance().showLine("sim_stick_" + x, 
                basis.add(stick.start.pos.x, stick.start.pos.y, stick.start.pos.z), 
                basis.add(stick.end.pos.x, stick.end.pos.y, stick.end.pos.z))
                    .lineWidth(0.02f).disableCull().colored(Color.GREEN);
        }
        debugVelocities();
    }

    private void debugVelocities() {
        Vec3 center = new Vec3(forces[1], forces[2], forces[3]);
        Vec3 start = new Vec3(forces[1] + halfOffset.x, forces[2] + halfOffset.y, forces[3] + halfOffset.z);
        Vec3 end = new Vec3(forces[1] - halfOffset.x, forces[2] - halfOffset.y, forces[3] - halfOffset.z);
        VectorOperations.drawDebugBox(start, Color.RED, "dbls");
        VectorOperations.drawDebugBox(end, Color.RED, "dble");
        VectorOperations.drawDebugBox(center, Color.RED, "dblc");
        VectorOperations.drawDebugRay(start, new Vector3f(forces[4] * 40f, forces[5] * 40f, forces[6] * 40f), new Color(0, 0, 255), "vs");
        VectorOperations.drawDebugRay(end, new Vector3f(forces[7] * 40f, forces[8] * 40f, forces[9] * 40f), new Color(0, 0, 255), "ve");
    }

    private void resetIfUnstable(TransmitterType trns) {
        if(points == null || !tracker.isCascading()) return;
        Mechano.LOGGER.warn("Cascading instability detected in " + this);
        ArrayList<@Nullable Vector3f> pins = new ArrayList<>(points.size());
        for(Point p : points) {
            if(p == null || !p.pinned) 
                pins.add(null);
            else pins.add(p.pos);
        }
        boolean wasLocked = this.locked;
        unlockSpan();
        Vector3d start = new Vector3d(forces[1] + halfOffset.x, forces[2] + halfOffset.y, forces[3] + halfOffset.z);
        Vector3d end = new Vector3d(forces[1] - halfOffset.x, forces[2] - halfOffset.y, forces[3] - halfOffset.z);
        setOffset(trns, start, end);
        calculateSegmentation(trns);
        this.locked = wasLocked;
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
        if(hasWind()) return false;
        return tracker.isResting();
    }

    @Override
    public void updateAhead(TransmitterType trns, int steps) {
        this.forces[10] = 0;
        this.forces[11] = 0;
        for(int x = 0; x < steps; x++) {
            update(trns);
            if(isResting()) return;
        }
        float arclength = 0;
        for(Stick s : sticks) arclength += s.getLength();
        Mechano.LOGGER.warn("Catenary simulation (" + span + " meters, " + arclength + " arcmeters) couldn't reach a state of restitution in " + steps + " iterations.");
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

    public float getSegmentLength() {
        return forces[0];
    }

    public void applyWind(float x, float y) {
        this.forces[10] = x;
        this.forces[11] = y;
    }
    
    public void applyWind(@Nullable Vector2f wind) {
        if(wind == null) {
            this.forces[10] = 0;
            this.forces[11] = 0;
            return;
        }
        applyWind(wind.x, wind.y);
    }

    public boolean hasWind() {
        return Math.abs(forces[10] - Catenaries.renderPipeline().getRestitutionSpeed()) > 0.1f 
            || Math.abs(forces[11] - Catenaries.renderPipeline().getRestitutionSpeed()) > 0.1f;
    }


    public Vec3 getWorldlyMidpoint() {
        return new Vec3(forces[1], forces[2], forces[3]);
    }

    public Vec3 getWorldlyStartPoint() {
        return getWorldlyMidpoint().add(halfOffset.x, halfOffset.y, halfOffset.z);
    }

    public Vec3 getWorldlyEndPoint() {
        return getWorldlyMidpoint().subtract(halfOffset.x, halfOffset.y, halfOffset.z);
    }

    @Override
    public boolean isInitialized() {
        return this.points != null && this.sticks != null;
    }

    @Override
    public void lockSpan() {
        this.locked = true;
    }

    @Override
    public void unlockSpan() {
        this.locked = false;
    }

    @Override
    public void dispose() {
        this.points = null;
        this.sticks = null;
    }

    @Override
    public boolean hasBeenDisposed() {
        return this.points == null;
    }
}