package com.quattage.mechano.foundation.gridapi.catenary.model;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryMesher.Point;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryMesher.Stick;
import com.quattage.mechano.foundation.gridapi.catenary.WindManager;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.gridapi.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class SimulatedCatenary extends CatenaryModel<SimulatedCatenary> {

    private float avgVelocity = 0f;
    private float[] restitutionError = new float[3];
    private @Nullable Vector2f wind = null;

    protected @Nullable ObjectArrayList<Point> points;
    protected @Nullable ObjectArrayList<Stick> sticks;

    public SimulatedCatenary() {}

    @Override
    public SimulatedCatenary setOffset(Vec3 start, Vec3 end) {
        if(this.halfOffset == null) this.halfOffset = new Vector3f();
        this.halfOffset.set((((float)(start.x + end.x)) / 2f) - (float)start.x, (((float)(start.y + end.y)) / 2f) - (float)start.y, (((float)(start.z + end.z)) / 2f) - start.z);
        this.length = halfOffset.length() * 2f;
        if(length > maxLength || !isInitialized()) return this;
        calculateSegmentation(); 
        return this;
    }

    @Override
    public SimulatedCatenary setOrderedOffset(LevelReader world, GridUUID start, GridUUID end, float pTicks) {
        TrackedStreamable[] ordered = TrackedStreamable.orderedByAssertionPriority(world, start, end);
        setOffset(((GridUUID)ordered[0]).getPos(world, pTicks), ((GridUUID)ordered[1]).getPos(world, pTicks));
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
        int segments = getSegmentCount();
        if(segments == 0) return this;
        this.points = new ObjectArrayList<Point>(segments + 1);
        this.sticks = new ObjectArrayList<Stick>(segments);
        Point previous = null;
        Vector3f trgt = new Vector3f();
        for(int x = 0; x < segments + 1; x++) {
            float spanProgress = (float)x / (float)segments;
            Point newPoint = new Point(quicklerp(trgt, spanProgress));
            points.add(x, newPoint);
            if(previous != null)
                sticks.add(x - 1, new Stick(previous, newPoint));
            previous = newPoint;
        }
        points.getFirst().pinned = true;
        points.getLast().pinned = true;
        return this;
    }

    

    @Override
    public SimulatedCatenary calculateSegmentation() {
        this.points.getFirst().clearPos();
        int segmentCount = getSegmentCount();
        if(segmentCount < this.points.size()) {
            boolean wasPinned = this.points.getLast().pinned;
            this.points.removeElements(segmentCount, this.points.size());
            this.sticks.removeElements(segmentCount - 1, this.sticks.size());
            if(wasPinned) {
                Point p = this.points.getLast();
                p.pinned = true;
                p.lastPos.set(p.pos);
            }
            this.points.trim();
            this.sticks.trim();
        } else if(segmentCount > this.points.size() && length < maxLength) {
            this.points.ensureCapacity(segmentCount);
            this.sticks.ensureCapacity(segmentCount - 1);
            addAdditionalSegments(segmentCount - this.points.size());
        }
        return this;
    }

    /**
     * Extends this wire by the given amount of segments
     * while retaining velocity data for all pre-existing 
     * {@link Point points} and {@link Stick sticks}.
     * This method is called internally
     * by {@link #calculateSegmentation}, which will automatically
     * grow/shrunk this SimulatedCatenary to span the 
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
            Point newPoint = new Point(previous.pos.lerp(halfOffset, 0.5f, new Vector3f()));
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
     * @throws IllegalStateException if this Catenary doesn't have {@link #moveTo an offset} or hasn't been {@link #initialize initialized} at least once.
    */
    @Override
    public void update(float delta) {
        assertHasOffset();
        assertSimulatable();

        final Vector3f gravity = getGravity(points.size());
        final Vector3f workingVector = new Vector3f(points.getLast().pos);
        avgVelocity = 0;

        updateEndpoints();
        if(WindManager.INSTANCE.isEnabled() && wind != null) 
            integrateVelocity(workingVector, gravity, wind, delta);
        else integrateVelocity(workingVector, gravity, delta);

        float constraintError = 0f;
        final Vector3f deltaPos = new Vector3f();
        for(int x = 0; x < CatenaryAttributes.SOLVER_STEPS; x++)
            applyConstriants(constraintError, deltaPos, delta);

        restitutionError[0] = restitutionError[1];
        restitutionError[1] = constraintError / (float)this.sticks.size();
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
    public void integrateVelocity(Vector3f vec, Vector3f gravity, Vector2f wind, float delta) {
        float mid = points.size() / 2f;
        for(int x = 0; x < points.size(); x++) {
            Point point = points.get(x);
            if(point.pinned) continue;
            vec.set(point.pos);
            Vector3f vel = point.pos.sub(point.lastPos, new Vector3f());
            Vector3f accel = new Vector3f().sub(gravity);
            float windStrength = (1 - (((float)x - mid ) / mid)) ;
            accel.add(wind.x * windStrength, 0, wind.y * windStrength);
            point.pos.add(vel).add(accel.mul(delta * delta));
            this.avgVelocity += vel.length();
            point.lastPos.set(vec);
        }
        this.avgVelocity /= points.size();
        if(Float.isNaN(this.avgVelocity)) {
            Mechano.LOGGER.warn("Cascading instability detected in " + this);
            initializeSpan();
            calculateSegmentation();
            return;
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
    public void integrateVelocity(Vector3f vec, Vector3f gravity, float delta) {
        for(Point point : points) {
            if(point.pinned) continue;
            vec.set(point.pos);
            Vector3f vel = point.pos.sub(point.lastPos, new Vector3f());
            Vector3f accel = new Vector3f().sub(gravity);
            point.pos.add(vel).add(accel.mul(delta * delta));
            this.avgVelocity += vel.length();
            point.lastPos.set(vec);
        }
        this.avgVelocity /= points.size();
        if(Float.isNaN(this.avgVelocity)) {
            Mechano.LOGGER.warn("Cascading instability detected in " + this.toFullString());
            initializeSpan();
            calculateSegmentation();
            return;
        }
    }

    /**
     * Applies physical constraints to each {@link Point} in this
     * catenary while preserving the volume of each segment. 
     * Several calls to this method should be made to accumulate 
     * results over time.
     * @param error The total restitution error accumulated as a result of this call
     * @param vec Working vector passed here to avoid continuous re-declaration
     */
    public void applyConstriants(float error, Vector3f vec, float delta) {
        for(int s = 0; s < sticks.size(); s++) {

            Stick stick = sticks.get(s);
            stick.computeForward();
            stick.computeCenter();
            stick.end.pos.sub(stick.start.pos, vec);
            float currentLength = vec.length();
            float diff = (currentLength - stick.length) / currentLength;
            vec.mul(diff * delta);

            if(!stick.start.pinned && !stick.end.pinned) {
                stick.start.pos.add(vec);
                stick.end.pos.sub(vec);
            } else if(!stick.start.pinned)
                stick.start.pos.add(vec.mul(2));
            else if(!stick.end.pinned)
                stick.end.pos.sub(vec.mul(2));

            error += Math.abs(stick.start.pos.distance(stick.end.pos) - currentLength);
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
            arclength += current.length;
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
        Point p = null;
        p = this.points.getFirst();
        if(p != null) {
            p.pos.set(-halfOffset.x, -halfOffset.y, -halfOffset.z);
            p.lastPos.set(p.pos);
            p.pinned = true;
        }
        p = this.points.getLast();
        if(p != null) {
            p.pos.set(halfOffset.x, halfOffset.y, halfOffset.z);
            p.lastPos.set(p.pos);
            p.pinned = true;
        }
        return this;
    }

    @Override
    public SimulatedCatenary unpinEndpoints() {
        if(this.points == null) return this;
        Point p = null;
        p = this.points.getFirst();
        if(p != null) {
            p.pos.set(-halfOffset.x, -halfOffset.y, -halfOffset.z);
            p.pinned = false;
        }
        p = this.points.getLast();
        if(p != null) {
            p.pos.set(halfOffset.x, halfOffset.y, halfOffset.z);
            p.pinned = false;
        }
        return this;
    }

    @Override
    public SimulatedCatenary updateEndpoints() {
        if(this.points == null) return this;
        Point p = points.getFirst();
        if(p.pinned) {
            p.pos.set(-halfOffset.x, -halfOffset.y, -halfOffset.z);
            p.lastPos.set(p.pos);
        }
        p = points.getLast();
        if(p.pinned) {
            p.pos.set(halfOffset.x, halfOffset.y, halfOffset.z);
            p.lastPos.set(p.pos);
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

    /**
     * Apply an upward force to the middle of the wire
     * to add some extra visual interest when the wire is 
     * created.
     */
    public void kick(float strength) {
        assertInitialized();
        int index = Math.round(this.points.size() / 2);
        Point kickPoint = this.points.get(index);
        if(kickPoint == null) return;
        kickPoint.lastPos.set(kickPoint.pos);
        kickPoint.pos.y += strength;
    }

    @Override
    public boolean isResting() {
        if(wind != null) return wind.length() > 0.01f;
        if(avgVelocity < CatenaryAttributes.RESTITUTION_VELOCITY && (Math.abs(restitutionError[1] - restitutionError[0]) < 1e-4f)) {
            if(restitutionError[2] > 42) return true;
            restitutionError[2]++;
            return false;
        } 
        restitutionError[2] = 0;
        return false;
    }

    @Override
    public void updateAhead(int steps) {
        disableWind();
        for(int x = 0; x < steps; x++) {
            update();
            if(isResting()) return;
        }
        float arclength = 0;
        for(Stick s : sticks) arclength += s.length;
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