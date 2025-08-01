package com.quattage.mechano.foundation.catenary.model;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.CatenaryMesher.Point;
import com.quattage.mechano.foundation.catenary.CatenaryMesher.Stick;
import com.quattage.mechano.foundation.catenary.WindManager;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class SimulatedCatenary extends CatenaryModel<SimulatedCatenary> {

    private float segmentTF = 0f;
    private float[] restitutionError = new float[3];
    private @Nullable Vector2f wind = null;

    protected @Nullable ObjectArrayList<Point> points;
    protected @Nullable ObjectArrayList<Stick> sticks;

    public SimulatedCatenary() {}

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
    public SimulatedCatenary initialize() {
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
        Mechano.LOGGER.info("CAT INIT");
        points.getFirst().pinned = true;
        points.getLast().pinned = true;
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
            previous.pinned = false;
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
            points.getLast().pinned = true;
    }

    @Override
    public SimulatedCatenary calculateSegmentation() {
        this.length = offset.length();
        
        if(!isInitialized()) {
            this.segmentTF = 0.01f;
            return this;
        }

        this.points.getFirst().clearPos();
        int segmentCount = getSegmentCount();
        float ratio = (length / maxLength);
        this.segmentTF = length < 2 ? 0.03f : Math.max(0.05f, Math.min(1, ratio * ratio * ratio));

        // grow or shrink the wire as needed
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
    public void update(float delta) {

        assertHasOffset();
        assertSimulatable();
        final Vector3f gravity = getGravity(points.size());
        final Vector3f lastPos = new Vector3f();
        avgVelocity = 0;

        constrainEnds(lastPos);
        if(WindManager.INSTANCE.isEnabled() && wind != null) 
            integrateVelocity(lastPos, gravity, wind);
        else integrateVelocity(lastPos, gravity);

        float constraintError = 0f;
        final Vector3f deltaPos = new Vector3f();
        for(int x = 0; x < CatenaryAttributes.SOLVER_STEPS; x++)
            applyConstriants(constraintError, deltaPos);

        restitutionError[0] = restitutionError[1];
        restitutionError[1] = constraintError / (float)this.sticks.size();
    }

    /**
     * Constrains the start and end of this catenary to their respective positions 
     * as long as they're pinned in place.
     * @param lastPos Working vector passed here to avoid continuous re-declaration
     */
    public void constrainEnds(Vector3f lastPos) {
        Point p = points.getFirst();
        if(p.pinned)
            p.clearPos();
        p = points.getLast();
        if(p.pinned) {
            lastPos.set(p.pos);
            p.pos.set(offset);
            p.lastPos.set(lastPos);
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
    public void integrateVelocity(Vector3f vec, Vector3f gravity, Vector2f wind) {
        float mid = points.size() / 2f;
        for(int x = 0; x < points.size(); x++) {
            Point point = points.get(x);
            if(point.pinned) continue;
            vec.set(point.pos);
            Vector3f vel = point.pos.sub(point.lastPos, new Vector3f());
            vel.sub(gravity);
            float windStrength = 1 - (((float)x - mid ) / mid);
            vel.add(wind.x * windStrength, 0, wind.y * windStrength);
            this.avgVelocity += vel.length();
            point.pos.add(vel);
            point.lastPos.set(vec);
        }
        this.avgVelocity /= points.size();
        if(Float.isNaN(this.avgVelocity)) {
            Mechano.LOGGER.warn("Cascading instability detected in " + this);
            initialize();
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
    public void integrateVelocity(Vector3f vec, Vector3f gravity) {
        for(Point point : points) {
            if(point.pinned) continue;
            vec.set(point.pos);
            Vector3f vel = point.pos.sub(point.lastPos, new Vector3f());
            vel.sub(gravity);
            this.avgVelocity += vel.length();
            point.pos.add(vel);
            point.lastPos.set(vec);
        }
        this.avgVelocity /= points.size();
        if(Float.isNaN(this.avgVelocity)) {
            Mechano.LOGGER.warn("Cascading instability detected in " + this.toFullString());
            initialize();
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
    public void applyConstriants(float error, Vector3f vec) {
        for(int s = 0; s < sticks.size(); s++) {

            Stick stick = sticks.get(s);
            stick.computeForward();
            stick.computeCenter();
            stick.end.pos.sub(stick.start.pos, vec);
            float currentLength = vec.length();
            float diff = (currentLength - stick.length) / currentLength;
            vec.mul(segmentTF * diff);

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

    public void applyWind(@Nullable Vector2f wind) {
        this.wind = wind;
    }

    @Override
    public SimulatedCatenary setOffset(Vector3f offset) {
        if(this.offset == null)
            this.offset = new Vector3f(offset.x, offset.y, offset.z);
        else this.offset.set(offset);
        calculateSegmentation();
        return this;
    }

    @Override
    public SimulatedCatenary setOffset(Vec3 start, Vec3 end) {
        if(this.offset == null)
            this.offset = new Vector3f();
        this.offset.set((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z));
        calculateSegmentation();
        return this;
    }

    @Override
    public SimulatedCatenary fixEndpoints() {
        if(this.points == null) return this;
        Point p = null;
        p = this.points.getFirst();
        p.pos.set(0, 0, 0);
        p.lastPos.set(0, 0, 0);
        p.pinned = true;
        p = this.points.getLast();
        p.pos.set(offset.x, offset.y, offset.z);
        p.lastPos.set(offset.x, offset.y, offset.z);
        p.pinned = true;
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

    @Override
    public ParametricCatenary toParametric() {
        assertHasOffset();
        assertInitialized();
        ParametricCatenary parametric = new ParametricCatenary();
        parametric.offset = this.offset;
        parametric.length = this.length;
        parametric.maxLength = this.maxLength;
        parametric.avgVelocity = this.avgVelocity;
        this.points = null;
        this.sticks = null;
        return parametric;
    }

    @Override
    public BakedCatenary bake() {
        assertHasOffset();
        assertInitialized();
        BakedCatenary baked = new BakedCatenary(offset, sticks);
        baked.length = this.length;
        baked.maxLength = this.maxLength;
        baked.avgVelocity = 0;
        this.points = null;
        this.sticks = null;
        return baked;
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
        if(wind != null)
            return wind.length() > 0.01f;
        if(super.isResting() && (Math.abs(restitutionError[1] - restitutionError[0]) < 1e-4f)) {
            if(restitutionError[2] > 42)
                return true;
            restitutionError[2]++;
            return false;
        } 
        restitutionError[2] = 0;
        return false;
    }

    @Override
    public void updateAhead(int steps) {
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

    @Override
    public SimulatedCatenary toSimulated() {
        Mechano.LOGGER.warn("Attempted to convert a SimulatedCatenary to itself!");
        return this;
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
        this.segmentTF = 0;
    }
}