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
import net.minecraft.world.phys.Vec3;

public class BakedCatenary extends CatenaryModel<BakedCatenary> {

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

    @Override public BakedCatenary initialize() { return this; }
    @Override public boolean isInitialized() { return this.sticks != null; }
    @Override public float getLength() { return length; }
    @Override public float getMaxLength() { return length; }
    @Override public BakedCatenary setOffset(Vector3f offset) { return this; }
    @Override public BakedCatenary setOffset(Vec3 start, Vec3 end) { return this; }
    @Override public void update(float delta) { return; }
    @Override public void calculateSegmentation() { return; }
    @Override public void destroy() { this.sticks = null; }
    @Override public boolean isMovable() { return false; }


    @Override
    public void render(VertexConsumer buffer, Pose pose, CatenaryMesher geo, float pTicks) {
        if(sticks.size() < 2) {
            Mechano.LOGGER.error("Attempted to render BakedCatenary with invalid (< 2) size!");
            return;
        }
        Stick previous = sticks.getFirst();
        geo.setLight0(geo.getLight(previous.start.pos));
        geo.setLight1(geo.getLight(previous.end.pos));
        geo.model.profile.make(buffer, pose, geo, null, previous, sticks.get(1), 0, true, pTicks);
        for(int x = 1; x < sticks.size() - 1; x++) {
            Stick current = sticks.get(x);
            geo.setLight1(geo.getLight(current.start.pos));
            geo.model.profile.make(buffer, pose, geo, previous, current, sticks.get(x + 1), x, true, pTicks);
            previous = current;
            geo.walkLight();
        }
        Stick last = sticks.getLast();
        geo.setLight1(geo.getLight(last.end.pos));
        geo.model.profile.make(buffer, pose, geo, previous, last, null, sticks.size(), true, pTicks);
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
    public SimulatedCatenary toSimulated(boolean pinEnds) {
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
        if(pinEnds) {
            simulated.points.getFirst().pin();
            simulated.points.getLast().pin();
        }
        simulated.offset = this.offset;
        simulated.tension = this.tension;
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
        parametric.tension = this.tension;
        parametric.length = this.length;
        parametric.maxLength = this.maxLength;
        this.sticks = null;
        return parametric;
    }

    @Override
    public BakedCatenary bake() {
        Mechano.LOGGER.warn("Attempted to convert a BakedCatenary to itself!");
        return this;
    }


}
