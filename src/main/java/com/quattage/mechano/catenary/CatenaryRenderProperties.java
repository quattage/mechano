package com.quattage.mechano.catenary;

import java.util.Objects;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.catenary.Catenaries.RenderPipeline.Thickness;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatenaryRenderProperties {

    protected TransmitterType trns;
    protected MeshExtruder extruder;
    protected Thickness thickness;
    protected BiFunction<TransmitterType, Boolean, RenderType> materialGetter;

    protected @Nullable TextureAtlasSprite sprite = null;
    private @Nullable ResourceLocation atlasLocation;
    private @Nullable ResourceLocation textureLocation;

    public CatenaryRenderProperties(ResourceLocation textureLocation) {
        Objects.requireNonNull(textureLocation);
        this.textureLocation = textureLocation;
        this.materialGetter = Catenaries.renderPipeline().SOLID_MATERIAL;
    }

    public void configure(TransmitterType entry) {
        this.trns = entry;
    }

    public void applyTextures(ResourceLocation atlasLocation, ResourceLocation textureLocation) {
        this.atlasLocation = atlasLocation;
        this.textureLocation = textureLocation;
        this.sprite = null; // reacquire lazily
    }

    public CatenaryRenderProperties extruder(MeshExtruder extruder) {
        Objects.requireNonNull(extruder);
        this.extruder = extruder;
        return this;
    }

    public CatenaryRenderProperties thickness(Thickness thickness) {
        Objects.requireNonNull(thickness);
        this.thickness = thickness;
        return this;
    }

    public CatenaryRenderProperties thickness(int pixels) {
        this.thickness = Thickness.byValue(pixels);
        return this;
    }

    public CatenaryRenderProperties material(BiFunction<TransmitterType, Boolean, RenderType> materialGetter) {
        Objects.requireNonNull(materialGetter);
        this.materialGetter = materialGetter;
        return this;
    }

    public boolean isVisible() {
        return extruder != null & thickness != Thickness.ZERO;
    }

    public ResourceLocation getTextureLocation() {
        assertConfigured();
        return textureLocation;
    }

    public ResourceLocation getAtlasLocation() {
        assertConfigured();
        return atlasLocation;
    }

    public BiFunction<TransmitterType, Boolean, RenderType> getMaterialGetter() {
        assertConfigured();
        return materialGetter;
    }

    public RenderType getMaterialForDynamicMeshing() {
        assertConfigured();
        if(trns.getRegistryID() == null) 
            return RenderType.entityCutoutNoCull(textureLocation);
        return materialGetter.apply(trns, false);
    }

    public RenderType getMaterialForSectionMeshing() {
        assertConfigured();
        if(trns.getRegistryID() == null) 
            return RenderType.cutout();
        return materialGetter.apply(trns, true);
    }

    public MeshExtruder getExtruder() {
        return extruder;
    }

    public Thickness getThickness() {
        assertConfigured();
        return thickness;
    }

    public TextureAtlasSprite getAtlasSprite() {
        assertConfigured();
        return sprite;
    }

    private void assertConfigured() {
        if(this.trns == null) throw new IllegalStateException("The render properties for a transmitter type have yet to be configured!");
    }

    @Override
    public String toString() {
        return "TransmitterRenderProperties[Entry: " + 
            (trns == null ? "unavailable" : trns.getName()) 
            + ", Extruder? " + (extruder != null) + ", Thickness: " 
            + (thickness == null ? "n/a" : thickness) 
            + ", Material? " + (materialGetter != null) + "]";
    }
}
