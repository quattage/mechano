package com.quattage.mechano.foundation.catenary.model;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.quattage.mechano.foundation.catenary.Catenary;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher.Point;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher.Stick;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;

public class ParametricCatenary extends Catenary<ParametricCatenary> {
    
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
    public void calculateSegmentation() {

        this.length = offset.length();
        if(axisU == null) axisU = new Vector3f();
        if(axisV == null) axisV = new Vector3f();
        this.axisU.set(offset).normalize();
        this.axisV.set(CatenaryAttributes.UP).sub(axisU.mul(CatenaryAttributes.UP.dot(axisU), new Vector3f())).normalize();

        if(!isInitialized()) return;
        int pointCount = getSegmentCount();
        if(this.points == null) 
            this.points = new ObjectArrayList<>(pointCount);
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
        accelerator[1] = getApproximateTension();
        accelerator[2] = accelerator[1] / (2f * (a * (float)StrictMath.cosh(length / (2f * a)) - a));
        accelerator[3] = accelerator[2] * (a * (float)StrictMath.cosh(-(length / 2) /  a) - a);
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
    public void render(VertexConsumer buffer, Pose pose, CatenaryMesher geo, float pTicks) {
        
    }

    @Override
    public boolean isInitialized() {
        return this.points != null;
    }

    private void assertInitialized() {
        if(this.points == null)
            throw new IllegalStateException("Cannot update " + this + " - This WireModel has not been initialized!");
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
    public String toString() {
        if(points == null) return "ParametricWireModel[UNINITIALIZED]";
        String out = "\nParametricWireModel[\n";
        for(int x = 0; x < points.size(); x++) {
            Point p = points.get(x);
            if(p == null) out += "\t( NULL )\n";
            out += "\t(" + String.format("%.2f", p.pos.x) + ", " + String.format("%.2f", p.pos.y) + ", " + String.format("%.2f", p.pos.z) + ")\n";
        }
        return out + "]";
    }

    public SimulatedCatenary toSimulated(boolean pinEnds) {
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
        if(pinEnds) {
            simulated.points.getFirst().pin();
            simulated.points.getLast().pin();
        }
        simulated.offset = this.offset;
        simulated.tension = this.tension;
        simulated.length = this.length;
        this.points = null;
        this.axisU = null;
        this.axisV = null;
        return simulated;
    }
}
