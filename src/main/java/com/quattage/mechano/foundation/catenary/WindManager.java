package com.quattage.mechano.foundation.catenary;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joml.Vector2f;

import com.quattage.mechano.foundation.api.landmark.GridCatenary;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WindManager {

    public static final WindManager INSTANCE = new WindManager();

    private final float[] windSpeeds = new float[] { 0.013f, 0.021f, 0.042f };
    private boolean enabled = true;
    private WeakReference<PerlinSimplexNoise> noise = new WeakReference<>(null);
    private Set<GridCatenary> trackedCatenaries = new HashSet<>();

    private PerlinSimplexNoise getOrCreateNoise(RandomSource random) {
        if(!noise.refersTo(null)) return noise.get();
        noise = new WeakReference<PerlinSimplexNoise>(new PerlinSimplexNoise(random.fork(), List.of(0)));
        return noise.get();
    }

    public Vector2f sample(ClientLevel world, BlockPos pos) {
        float strengthScalar = getWindSpeedAt(world, pos);
        float time = world.getGameTime() * strengthScalar;
        Vector2f output = new Vector2f(
            (float)getOrCreateNoise(world.random).getValue(pos.getX() + time * 0.13f, pos.getZ() * 0.31f + time, true),
            (float)getOrCreateNoise(world.random).getValue(pos.getX() * 0.76f + time, pos.getZ() + time * 0.49, true)
        );
        output.normalize().mul(strengthScalar * output.x);
        return output;
    }
    private float getWindSpeedAt(ClientLevel world, BlockPos pos) {
        boolean rain = world.isRainingAt(pos);
        if(rain && world.isThundering()) return 0.021f;
        if(rain) return windSpeeds[1];
        return windSpeeds[0];
    }

    public void track(GridCatenary cat) {
        synchronized(trackedCatenaries) { trackedCatenaries.add(cat); }
    }

    public void forget(GridCatenary cat) {
        synchronized(trackedCatenaries) { trackedCatenaries.remove(cat); }
    }

    public void setEnabled(boolean enable) {
        if(!enabled)
            enabled = enable;
        else if(enabled && !enable) {
            reset();
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reset() {
        enabled = true;
        noise = new WeakReference<>(null);
        trackedCatenaries.clear();
    }
}
