package com.quattage.mechano.foundation.catenary.model;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.CatenaryMesher.Stick;
import com.quattage.mechano.foundation.helper.VectorHelper;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class BakedCatenary extends CatenaryModel<BakedCatenary> {

    // TODO convert to primative array
    private @Nullable ObjectArrayList<Stick> sticks;

    /**
     * Called exclusively by the {@link CatenaryModel#bake bake} method in implementing
     * subclasses of {@link CatenaryModel}
     * @param offset
     * @param sticks
     */
    protected BakedCatenary(Vector3f offset, ObjectArrayList<Stick> sticks) {
        Objects.requireNonNull(sticks);
        Objects.requireNonNull(offset);
        this.offset = offset;
        this.sticks = sticks;
    }

    // TODO implement
    @Override
    public BakedCatenary render(VertexConsumer buffer, Pose pose, CatenaryMesher geo, float pTicks) {
        return this;
    }

    @Override
    public void drawDebug(Vec3 basis) {
        if(sticks == null) return;
        for(int x = 0; x < sticks.size(); x++) {
            Stick stick = sticks.get(x);
            if(stick == null) continue;
            VectorHelper.drawDebugBox(basis.add(stick.end.pos.x, stick.end.pos.y, stick.end.pos.z), 0.007f, Color.BLACK, "bake_point_" + x);
            Outliner.getInstance().showLine("bake_stick_" + x, 
                basis.add(stick.start.pos.x, stick.start.pos.y, stick.start.pos.z), 
                basis.add(stick.end.pos.x, stick.end.pos.y, stick.end.pos.z))
                    .lineWidth(0.02f).disableCull().colored(Color.PURPLE);
        }
    }

    @Override
    public SimulatedCatenary toSimulated() {
        assertInitialized();
        assertHasOffset();
        SimulatedCatenary simulated = new SimulatedCatenary();
        simulated.sticks = this.sticks;
        simulated.points = new ObjectArrayList<>();
        simulated.points.add(sticks.get(0).start);
        for(int x = 1; x < sticks.size(); x++) {
            Stick s = sticks.get(x);
            simulated.points.add(s.start);
            simulated.points.add(s.end);
        }
        simulated.offset = this.offset;
        simulated.length = this.length;
        simulated.maxLength = this.maxLength;
        this.sticks = null;
        return simulated;
    }

    @Override
    public ParametricCatenary toParametric() {
        assertInitialized();
        assertHasOffset();
        ParametricCatenary parametric = new ParametricCatenary();
        parametric.offset = this.offset;
        parametric.length = this.length;
        parametric.maxLength = this.maxLength;
        this.sticks = null;
        return parametric;
    }

    @Override
    public boolean isResting() {
        return true;
    }

    @Override
    public BakedCatenary fixEndpoints() {
        Mechano.LOGGER.warn("Attempted to fix the endpoints of a BakedCatenary!");
        return this;
    }

    @Override
    public BakedCatenary bake() {
        Mechano.LOGGER.warn("Attempted to convert a BakedCatenary to itself!");
        return this;
    }

    @Override public BakedCatenary initialize() { return this; }
    @Override public boolean isInitialized() { return this.sticks != null; }
    @Override public float getSpan() { return length; }
    @Override public float getMaximumSpan() { return length; }
    @Override public void adjustSpan(LevelReader world, float length) { return; }
    @Override public BakedCatenary setOffset(Vector3f offset) { return this; }
    @Override public BakedCatenary setOffset(Vec3 start, Vec3 end) { return this; }
    @Override public void update(float delta) { return; }
    @Override public BakedCatenary calculateSegmentation() { return this; }
    @Override public void destroy() { this.sticks = null; }
}
