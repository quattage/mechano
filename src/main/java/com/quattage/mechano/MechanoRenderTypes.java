package com.quattage.mechano;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class MechanoRenderTypes extends RenderType {
    

    public MechanoRenderTypes(String pName, VertexFormat pFormat, Mode pMode, int pBufferSize,
            boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    public static final Function<ResourceLocation, RenderType> WIRE_TRANSLUCENT = Util.memoize((spoolType) -> {
        RenderType.CompositeState wireComposite = RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard((ResourceLocation)spoolType, false, false))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setOverlayState(OVERLAY)
            .setLightmapState(LIGHTMAP)
            .setOutputState(TRANSLUCENT_TARGET)
            .setCullState(new RenderStateShard.CullStateShard(false))
            .createCompositeState(false);

        return create("wire", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, wireComposite);
    });

    // the same for now
    public static final Function<ResourceLocation, RenderType> WIRE_STATIC = Util.memoize((spoolType) -> {
        RenderType.CompositeState wireComposite = RenderType.CompositeState.builder()
            .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard((ResourceLocation)spoolType, false, false))
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setOverlayState(OVERLAY)
            .setLightmapState(LIGHTMAP)
            .setOutputState(TRANSLUCENT_TARGET)
            .setCullState(new RenderStateShard.CullStateShard(false))
            .createCompositeState(false);

        return create("wire", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false, wireComposite);
    });

    public static RenderType getWireStatic(ResourceLocation spoolType) {
        return WIRE_STATIC.apply(spoolType);
    }


    public static RenderType getWireTranslucent(ResourceLocation spoolType) {
        return WIRE_TRANSLUCENT.apply(spoolType);
    }
}
