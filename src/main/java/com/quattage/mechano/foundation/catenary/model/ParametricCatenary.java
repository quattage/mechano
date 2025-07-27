package com.quattage.mechano.foundation.catenary.model;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.CatenaryMesher.Point;
import com.quattage.mechano.foundation.catenary.CatenaryMesher.Stick;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class ParametricCatenary extends CatenaryModel<ParametricCatenary> {
    
    private @Nullable ObjectArrayList<Point> points;
    private @Nullable Vector3f axisU;
    private @Nullable Vector3f axisV;

    private final float[] accelerator = new float[4];

    private static final float a = 10f;

    @Override
    public ParametricCatenary setOffset(Vector3f offset) {
        if(this.offset == null)
            this.offset = new Vector3f(offset.x, offset.y, offset.z);
        else this.offset.set(offset);
        calculateSegmentation();
        return this;
    }

    @Override
    public ParametricCatenary setOffset(Vec3 start, Vec3 end) {
        if(this.offset == null) this.offset = new Vector3f();
        this.offset.set((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z));
        calculateSegmentation();
        return this;
    }

    @Override
    public ParametricCatenary initialize() {
        this.points = new ObjectArrayList<>();
        return this;
    }

    @Override
    public ParametricCatenary calculateSegmentation() {

        this.length = offset.length();
        if(axisU == null) axisU = new Vector3f();
        if(axisV == null) axisV = new Vector3f();
        this.axisU.set(offset).normalize();
        this.axisV.set(CatenaryAttributes.UP).sub(axisU.mul(CatenaryAttributes.UP.dot(axisU), new Vector3f())).normalize();

        int pointCount = getSegmentCount();
        if(this.points == null) this.points = new ObjectArrayList<>(pointCount);
        if(pointCount < points.size()) {
            this.points.removeElements(pointCount, this.points.size());
            this.points.trim();
        } else if(pointCount > this.points.size()) {
            this.points.ensureCapacity(pointCount + 1);
            for(int x = this.points.size(); x < pointCount; x++)
                points.add(new Point(new Vector3f()));
        }

        // length, height, corrective, offset
        accelerator[0] = offset.dot(axisU); 
        accelerator[1] = 0.76f;
        accelerator[2] = accelerator[1] / (2f * (a * (float)StrictMath.cosh(length / (2f * a)) - a));
        accelerator[3] = accelerator[2] * (a * (float)StrictMath.cosh(-(length / 2) /  a) - a);

        return this;
    }

    @Override
    public void adjustSpan(LevelReader world, float length) {
        this.maxLength = length;
    }

    @Override
    public void update(float delta) {
        assertInitialized();
        Vector3f oU = new Vector3f();
        Vector3f oV = new Vector3f();
        for(int x = 0; x < points.size(); x++) {
            float spanProgress = ((float)x / ((float)points.size() - 1));
            float xO = spanProgress * length;
            float yO = ((a * (float)StrictMath.cosh((xO - length / 2f) / a) - a) * accelerator[2]) - accelerator[3];
            oU.set(axisU.x * xO, axisU.y, axisU.z);
            oV.set(axisV.x, axisV.y * yO, axisV.z);
            Point p = points.get(x);
            p.lastPos.set(p.pos);
            p.pos.set(new Vector3f(axisU).mul(xO).add(new Vector3f(axisV).mul(yO)));
        }
    }

    @Override
    public SimulatedCatenary toSimulated() {
        assertInitialized();
        assertHasOffset();
        SimulatedCatenary simulated = new SimulatedCatenary();
        simulated.points = this.points;
        simulated.sticks = new ObjectArrayList<Stick>(this.points.size() - 1);
        Point previous = null;
        for(int x = 0; x < points.size(); x++) {
            Point p = points.get(x);
            p.lastPos.set(p.pos);
            if(previous != null)
                simulated.sticks.add(new Stick(previous, p));
            previous = p;
        }
        simulated.offset = this.offset;
        simulated.length = this.length;
        simulated.maxLength = this.maxLength;
        simulated.avgVelocity = this.avgVelocity;
        this.points = null;
        this.axisU = null;
        this.axisV = null;
        return simulated;
    }

    @Override
    public BakedCatenary bake() {
        assertInitialized();
        assertHasOffset();
        ObjectArrayList<Stick> sticks = new ObjectArrayList<>(this.points.size() - 1);
        Point previous = null;
        for(int x = 0; x < points.size(); x++) {
            Point p = points.get(x);
            p.lastPos.set(p.pos);
            if(previous != null)
                sticks.add(new Stick(previous, p));
            previous = p;
        }
        BakedCatenary baked = new BakedCatenary(offset, sticks);
        baked.length = this.length;
        baked.maxLength = this.maxLength;
        baked.avgVelocity = 0;
        this.points = null;
        this.axisU = null;
        this.axisV = null;
        return baked;
    }

    @Override
    public float getMaximumSpan() {
        return maxLength;
    }

    @Override
    public ParametricCatenary fixEndpoints() {
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
    public String toString() {
        if(points == null) return "ParametricCatenary[UNINITIALIZED]";
        String out = "\nParametricCatenary[\n";
        for(int x = 0; x < points.size(); x++) {
            Point p = points.get(x);
            if(p == null) out += "\t( NULL )\n";
            out += "\t(" + String.format("%.2f", p.pos.x) + ", " + String.format("%.2f", p.pos.y) + ", " + String.format("%.2f", p.pos.z) + ")\n";
        }
        return out + "]";
    }

    @Override
    public void drawDebug(Vec3 basis) {
        if(points == null) return;
        Vector3f last = null;
        for(int x = 0; x < points.size(); x++) {
            Vector3f current = points.get(x).pos;
            VectorHelper.drawDebugBox(basis.add(current.x, current.y, current.z), 0.007f, Color.BLACK, "para_point_" + x);
            if(last != null)
                Outliner.getInstance().showLine("para_stick_" + x, basis.add(last.x, last.y, last.z), basis.add(current.x, current.y, current.z)).lineWidth(0.02f).disableCull().colored(Color.PURPLE);
            last = current;
        }
    } 

    @Override
    public ParametricCatenary toParametric() {
        Mechano.LOGGER.warn("Attempted to convert a ParametricCatenary to itself!");
        return this;
    }

    @Override
    public ParametricCatenary render(VertexConsumer buffer, Pose pose, CatenaryMesher geo, float pTicks) {
        throw new UnsupportedOperationException("Parametric catenaries cannot be rendered! They must either be baked first or converted to a simulation!");
    }

    @Override
    public boolean isInitialized() {
        return this.points != null;
    }

    @Override
    public float getSpan() {
        return length;
    }


    @Override
    public void destroy() {
        this.points = null;
        this.axisU = null;
        this.axisV = null;
    }
}
