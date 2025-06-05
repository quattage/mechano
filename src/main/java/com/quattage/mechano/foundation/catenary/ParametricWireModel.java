package com.quattage.mechano.foundation.catenary;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.phys.Vec3;

public class ParametricWireModel extends WireModel<ParametricWireModel> {
    
    private @Nullable ObjectArrayList<Vector3f> points;

    private @Nullable Vector3f axisU;
    private @Nullable Vector3f axisV;

    private final float[] accelerator = new float[4];

    private static final Vector3f up = new Vector3f(0, 1, 0);

    @Override
    public ParametricWireModel setOffset(Vector3f offset) {
        if(this.offset == null)
            this.offset = new Vector3f(offset.x, offset.y, offset.z);
        else this.offset.set(offset);
        calculateSegmentation();
        return this;
    }

    @Override
    public ParametricWireModel setOffset(Vec3 start, Vec3 end) {
        if(this.offset == null) this.offset = new Vector3f();
        this.offset.set((float)(end.x - start.x), (float)(end.y - start.y), (float)(end.z - start.z));
        calculateSegmentation();
        return this;
    }


    @Override
    public void calculateSegmentation() {

        this.length = offset.length();
        if(axisU == null) axisU = new Vector3f();
        if(axisV == null) axisV = new Vector3f();
        this.axisU.set(offset).normalize();
        this.axisV.set(up).sub(axisU.mul(up.dot(axisU), new Vector3f())).normalize();

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
                points.add(null);
        }

        // length, height, corrective, offset
        accelerator[0] = offset.dot(axisU); 
        accelerator[1] = getApproximateTension();
        accelerator[2] = accelerator[1] / (2f * (CATENARY_HYPERBOLIC * (float)StrictMath.cosh(length / (2f * CATENARY_HYPERBOLIC)) - CATENARY_HYPERBOLIC));
        accelerator[3] = accelerator[2] * (CATENARY_HYPERBOLIC * (float)StrictMath.cosh(-(length / 2) /  CATENARY_HYPERBOLIC) - CATENARY_HYPERBOLIC);
    }

    @Override
    public ParametricWireModel initialize() {
        assertHasOffset();
        this.points = new ObjectArrayList<>();
        return this;
    }

    @Override
    public void update() {
        assertInitialized();

        Vector3f oU = new Vector3f();
        Vector3f oV = new Vector3f();

        for(int x = 0; x < points.size(); x++) {
            float spanProgress = ((float)x / ((float)points.size() - 1));
            float xO = spanProgress * length;
            float yO = ((CATENARY_HYPERBOLIC * (float)StrictMath.cosh((xO - length / 2f) / CATENARY_HYPERBOLIC) - CATENARY_HYPERBOLIC) * accelerator[2]) - accelerator[3];

            oU.set(axisU.x * xO, axisU.y, axisU.z);
            oV.set(axisV.x, axisV.y * yO, axisV.z);

            points.set(x, new Vector3f(axisU).mul(xO).add(new Vector3f(axisV).mul(yO)));
        }
    }

    private void assertInitialized() {
        if(this.points == null)
            throw new IllegalStateException("Cannot update " + this + " - This WireModel has not been initialized!");
    }

    @Override
    public void render() {
        
    }

    @Override
    public boolean isInitialized() {
        return this.points != null;
    }

    @Override
    public void drawDebug(Vec3 basis) {
        if(points == null) return;
        Vector3f last = null;
        for(int x = 0; x < points.size(); x++) {
            Vector3f current = points.get(x);
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
            Vector3f p = points.get(x);
            if(p == null) out += "\t( NULL )\n";
            out += "\t(" + String.format("%.2f", p.x) + ", " + String.format("%.2f", p.y) + ", " + String.format("%.2f", p.z) + ")\n";
        }
        return out + "]";
    }

}
