package com.quattage.mechano.foundation.catenary;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import com.quattage.mechano.foundation.catenary.meshing.ProfileExtrusion;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryGeometry;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryGeometry.MutableExtruder;

public class CatenaryAttributes {

    public static final boolean TEX_USE_MIPS = false;
    public static final int[] TEX_DIMS = new int[] { 16, 32 };

    public static final Vector3f UP = new Vector3f(0, 1, 0);
    public static final int SOLVER_STEPS = 10;
    public static final float POINT_MASS = 3f;
    public static final float TENSION_EPSILON = 1e-3f;
    public static final float DRAW_RES = 1f;
    public static final int DRAW_MIN = 4;
    public static final int DRAW_MAX = 32;

    public static final  CatenaryAttributeHolder INVISIBLE
        = CatenaryAttributes
            .as(ModelType.NO_DRAW)
            .withThickness(Thickness.ZERO);

    public static final CatenaryAttributeHolder DEFAULT 
        = CatenaryAttributes
            .as(ModelType.SQUARE)
            .withThickness(Thickness.TRIPLE)
            .withTension(Tension.AVERAGE);


    public static final Function<TransmitterType<?>, RenderType> SOLID_MATERIAL = Util.memoize(trns -> {
        RenderType.CompositeState composite = RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_SOLID_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(trns.getTextureLocation(), false, TEX_USE_MIPS))
            .setOverlayState(RenderType.OVERLAY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
            .setOutputState(RenderType.MAIN_TARGET)
            .setCullState(RenderType.CULL)
            .createCompositeState(false); 
        // TODO maybe switch to triangle strips for better performance 
        return RenderType.create("catenary_solid", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, DRAW_MAX * 8, true, false, composite);
    });

    public static final Function<TransmitterType<?>, RenderType> CUTOUT_MATERIAL = Util.memoize(trns -> {
        RenderType.CompositeState composite = RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(trns.getTextureLocation(), false, TEX_USE_MIPS))
            .setOverlayState(RenderType.OVERLAY)
            .setLightmapState(RenderType.LIGHTMAP)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setOutputState(RenderType.TRANSLUCENT_TARGET)
            .setCullState(RenderType.CULL)
            .createCompositeState(false);
        return RenderType.create("catenary_cutout", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, DRAW_MAX * 8, true, false, composite);
    });

    public static CatenaryAttributeHolder as(ModelType type) {
        return new CatenaryAttributeHolder(type);
    }


    public static CatenaryAttributeHolder getFor(@Nullable TransmitterType<?> type) {
        return getFor(type, null);
    }

    public static CatenaryAttributeHolder getFor(@Nullable TransmitterType<?> type, Tension t) {
        if(type == null)
            return CatenaryAttributeHolder.copy(DEFAULT).withTension(t);
        return CatenaryAttributeHolder.copy(type.getAttributes()).withTension(t);
    }

    public static MutableExtruder getMutableFor(@Nullable TransmitterType<?> type, Vec3 basis) {
        return getMutableFor(type, null, basis);
    }

    public static MutableExtruder getMutableFor(@Nullable TransmitterType<?> type, Tension tension, Vec3 basis) {
        if(type == null)
            return (MutableExtruder)CatenaryAttributeHolder.mutableCopy(DEFAULT, basis).withTension(tension);
        CatenaryAttributeHolder original = type.getAttributes();
        MutableExtruder out = new MutableExtruder(original.model, basis);
        out.thick = original.thick;
        out.tension = tension == null ? original.tension : tension;
        return out;
    }


    public static class CatenaryAttributeHolder {

        public final ModelType model;
        protected @Nullable Thickness thick = Thickness.TRIPLE;
        protected @Nullable Tension tension = null;

        public static CatenaryAttributeHolder copy(CatenaryAttributeHolder in) {
            CatenaryAttributeHolder copy = new CatenaryAttributeHolder(in.getModelType());
            copy.thick = in.thick;
            copy.tension = in.tension;
            return copy;
        }

        public static MutableExtruder mutableCopy(CatenaryAttributeHolder in, Vec3 basis) {
            MutableExtruder copy = CatenaryGeometry
                .begin(in.getModelType()).at(basis)
                .withThickness(in.thick)
                .withTension(in.tension);
            return copy;
        }

        protected CatenaryAttributeHolder(ModelType model) {
            this.model = model;
        }

        public CatenaryAttributeHolder withThickness(Thickness thick) {
            if(thick == null) return this;
            this.thick = thick;
            return this;
        }

        public CatenaryAttributeHolder withTension(Tension tension) {
            if(tension == null) return this;
            this.tension = tension;
            return this;
        }

        protected ModelType getModelType() {
            return model;
        }

        public float getThickness() {
            return thick == null ? Thickness.TRIPLE.get() : thick.get();
        }

        public float getHalfThickness() {
            return thick == null ? Thickness.TRIPLE.get() / 2f : thick.get() / 2f;
        }

        public Tension getTension() {
            return tension == null ? Tension.AVERAGE : tension;
        }

        protected float getNumericalTension() {
            return tension == null ? Tension.AVERAGE.get() : tension.get();
        }

        public @Nullable RenderType getShaderFor(TransmitterType<?> type) {
            return model.getShader(type);
        }
    }

    public static enum ModelType {

        SQUARE(false, true, new ProfileExtrusion() {
            @Override
            public void extrude(VertexConsumer buffer, Pose pose, float length, int uWidth, @Nullable float[] verts, @Nullable int[] light, Vector3f[] matrix) {
                emitQuad(buffer, pose, light, verts, matrix[3], 0, 4, 5, 1, 0, 0, 0, 0);
                emitQuad(buffer, pose, light, verts, matrix[4], 3, 7, 4, 0, 0, 0, 0, 0);
                emitQuad(buffer, pose, light, verts, matrix[4], 2, 6, 7, 3, 0, 0, 0, 0);
                emitQuad(buffer, pose, light, verts, matrix[3], 1, 5, 6, 2, 0, 0, 0, 0);
            }
        }), SQUARE_CUTOUT(true, true, SQUARE.profile),


        CROSS(false, true, null), CROSS_CUTOUT(true, true, null),

        BILLBOARD(false, true, null), BILLBOARD_CUTOUT(true, true, null),

        NO_DRAW(false, false, null);

        public final ProfileExtrusion profile;
        private final boolean cutout;
        private final boolean draws;

        private ModelType(boolean cutout, boolean draws, ProfileExtrusion extruder) {
            this.cutout = cutout;
            this.draws = draws;
            this.profile = extruder;
        }

        private @Nullable RenderType getShader(TransmitterType<?> type) {
            if(!draws) return null;
            if(type == null)
                return cutout ? RenderType.CUTOUT : RenderType.SOLID;
            ResourceLocation loc = TransmitterRegistry.INSTANCE.getKey(type);
            RenderType shader = cutout 
                ? CUTOUT_MATERIAL.apply(type)
                : SOLID_MATERIAL.apply(type);
            if(shader == null) {
                Mechano.LOGGER.warn("No valid RenderType could be found for regstered TransmitterType '" + loc + "' - A fallback default RenderType was returned!");
                return cutout ? RenderType.CUTOUT_MIPPED : RenderType.SOLID;
            }
            return shader;
        }

        public ProfileExtrusion getProfile() {
            if(profile == null)
                throw new UnsupportedOperationException("Extruder for " + this.name() + " has not yet been implemented!");
            return profile;
        }

        public boolean canRender() {
            return draws;
        }
    }

    public static enum Thickness {
        ZERO(0),
        SINGLE(1),
        DOUBLE(2),
        TRIPLE(3),
        QUADRUPLE(4);

        private final int pix;
        private final float thick;
        private Thickness(int pixels) {
            this.pix = pixels;
            this.thick = (pixels) / 16f;
        }

        public int getPixels() {
            return pix;
        }

        public float get() {
            return thick;
        }

        public float half() {
            return thick / 2f;
        }
    }

    public static enum Tension {

        TAUT(1f),
        TIGHT(0.4f),
        AVERAGE(0.2f),
        LOOSE(0.17f),
        VERY_LOOSE(0.15f),
        STUPID_LOOSE(0.09f);
        
        private final float t;

        private Tension(float t) {
            this.t = t;
        }

        public float get(float mul) {
            return t * mul;
        }

        public float get() {
            return t;
        }

        public float getSquared() {
            return t * t;
        }
    }
}
