
package com.quattage.mechano.foundation.catenary;

import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher.Stick;
import com.quattage.mechano.foundation.catenary.meshing.MeshExtruder;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class CatenaryAttributes {

    public static final boolean TEX_USE_MIPS = false;
    public static final int[] TEX_DIMS = new int[] { 16, 32 };

    public static final Vector3f UP = new Vector3f(0, 1, 0);
    public static final int SOLVER_STEPS = 20;
    public static final float POINT_MASS = 3f;
    public static final float TENSION_EPSILON = 1e-3f;
    public static final float DRAW_RES = 1f;

    public static final float KINEMATIC_SOFT = 0.9f;
    public static final float KINEMATIC_DAMP = 0.6f;
    public static final float DETACH_THRESHOLD = 0.6f;

    public static final int DRAW_MIN = 5;
    public static final int DRAW_MAX = 32;

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

    public static final  CatenaryAttributeHolder INVISIBLE
        = CatenaryAttributes
            .as(ModelType.NO_DRAW)
            .withThickness(Thickness.ZERO);

    public static final CatenaryAttributeHolder DEFAULT 
        = CatenaryAttributes
            .as(ModelType.SQUARE)
            .withThickness(Thickness.TRIPLE)
            .withTension(Tension.AVERAGE);

    public static enum ModelType {

        SQUARE(SOLID_MATERIAL, (VertexConsumer buffer, Pose pose, CatenaryMesher geo, @Nullable Stick previous, Stick current, @Nullable Stick next, int iteration, boolean faceNormals,
            float pTicks) -> {
                if(previous == null) geo.computeMatrix(current.getForward());
                else geo.computeMatrix(previous.getForward(), current.getForward());
                if(faceNormals) {
                    geo.setNormalA(geo.rightX() + geo.upX(), geo.rightY() + geo.upY(), geo.rightZ() + geo.upZ())
                        .setNormalB(geo.rightX() - geo.upX(), geo.rightY() - geo.upY(), geo.rightZ() - geo.upZ());
                }
                geo.place4Verts(current.start(pTicks), 0);
                if(next != null) geo.computeMatrix(current.getForward(), next.getForward());
                geo.place4Verts(current.end(pTicks), 4);
                geo.walkUVs(current, iteration);
                geo.emitQuad(buffer, pose, geo.normAX(), geo.normAY(), geo.normAZ(), 0, 4, 5, 1);
                geo.emitQuad(buffer, pose, -geo.normAX(), -geo.normAY(), -geo.normAZ(), 2, 6, 7, 3);
                geo.shiftUVs();
                geo.emitQuad(buffer, pose, geo.normBX(), geo.normBY(), geo.normBZ(), 3, 7, 4, 0);
                geo.emitQuad(buffer, pose, -geo.normBX(), -geo.normBY(), -geo.normBZ(), 1, 5, 6, 2);
        }), SQUARE_CUTOUT(CUTOUT_MATERIAL, SQUARE.profile),


        CROSS(null, null), CROSS_CUTOUT(null, null),
        BILLBOARD(null, null), BILLBOARD_CUTOUT(null, null),
        NO_DRAW(null, null);

        public final @Nullable MeshExtruder profile;
        private final @Nullable Function<TransmitterType<?>, RenderType> mat;

        private ModelType(Function<TransmitterType<?>, RenderType> materialGetter, MeshExtruder extruder) {
            this.profile = extruder;
            this.mat = Util.memoize(materialGetter);
        }

        public @Nullable RenderType getShader(TransmitterType<?> type) {
            if(profile == null) return null;
            if(type == null) return SOLID_MATERIAL.apply(MechanoTransmissionTypes.PERFECT_INSULATOR);
            ResourceLocation loc = TransmitterRegistry.INSTANCE.getKey(type);
            if(mat == null) {
                Mechano.LOGGER.warn("No valid RenderType could be found for transmitter '" + loc + "' (Model type '" + this + "')");
                return SOLID_MATERIAL.apply(MechanoTransmissionTypes.PERFECT_INSULATOR);
            }
            RenderType shader = mat.apply(type);
            if(shader == null) {
                Mechano.LOGGER.warn("No valid RenderType could be found for transmitter '" + loc + "'");
                return SOLID_MATERIAL.apply(MechanoTransmissionTypes.PERFECT_INSULATOR);
            }
            return shader;
        }

        public MeshExtruder getProfile() {
            if(profile == null)
                throw new UnsupportedOperationException("Extruder for " + this.name() + " has not yet been implemented!");
            return profile;
        }
    }

    public static CatenaryAttributeHolder as(ModelType type) {
        return new CatenaryAttributeHolder(type);
    }

    public static class CatenaryAttributeHolder {

        public @Nullable ModelType model;
        protected @Nullable Thickness thick = Thickness.TRIPLE;
        protected @Nullable Tension tension = null;

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

        public Thickness getThickness() {
            return thick == null ? Thickness.TRIPLE : thick;
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
        TIGHT(0.99f),
        AVERAGE(0.76f),
        LOOSE(0.60f),
        VERY_LOOSE(0.50f),
        STUPID_LOOSE(0.40f);
        
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
