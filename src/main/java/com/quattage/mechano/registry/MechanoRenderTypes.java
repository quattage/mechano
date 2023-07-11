package com.quattage.mechano.registry;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;

import net.minecraft.client.renderer.RenderType;

public class MechanoRenderTypes extends RenderType {

    public MechanoRenderTypes(String pName, VertexFormat pFormat, Mode pMode, int pBufferSize,
            boolean pAffectsCrumbling, boolean pSortOnUpload, Runnable pSetupState, Runnable pClearState) {

        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    public static final RenderType WIRE_ALL = create(
        "wire_all",                                                  // Name
        DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,                 // Vertex Format
        VertexFormat.Mode.QUADS,                                     // Vertex Mode
        256,                                                         // Buffer Size
        false,                                                       // Affects Crumbling
        true,                                                        // Sort
        RenderType.CompositeState.builder()                          // Composite State
            .setShaderState(RENDERTYPE_LEASH_SHADER)
            .setTextureState(NO_TEXTURE)
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .createCompositeState(false)
    );
}
