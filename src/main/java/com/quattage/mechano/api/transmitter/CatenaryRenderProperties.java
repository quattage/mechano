package com.quattage.mechano.api.transmitter;

import java.util.Objects;
import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.catenary.Catenaries.RenderPipeline.Thickness;
import com.quattage.mechano.api.catenary.MeshExtruder;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatenaryRenderProperties {

    protected TransmitterEntry entry;
    protected MeshExtruder extruder;
    protected Thickness thickness;
    protected BiFunction<TransmitterEntry, Boolean, RenderType> materialGetter;

    protected @Nullable TextureAtlasSprite sprite = null;
    private @Nullable ResourceLocation atlasLocation;
    private @Nullable ResourceLocation textureLocation;

    protected CatenaryRenderProperties(ResourceLocation textureLocation) {
        Objects.requireNonNull(textureLocation);
        this.textureLocation = textureLocation;
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

    public CatenaryRenderProperties material(BiFunction<TransmitterEntry, Boolean, RenderType> materialGetter) {
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

    public BiFunction<TransmitterEntry, Boolean, RenderType> getMaterialGetter() {
        assertConfigured();
        return materialGetter;
    }

    public RenderType getMaterialForDynamicMeshing() {
        assertConfigured();
        return materialGetter.apply(entry, false);
    }

    public RenderType getMaterialForSectionMeshing() {
        assertConfigured();
        return materialGetter.apply(entry, true);
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
        if(this.entry == null) throw new IllegalStateException("The render properties for a transmitter type have yet to be configured!");
    }

    @Override
    public String toString() {
        return "TransmitterRenderProperties[Entry: " + 
            (entry == null ? "unavailable" : entry.getKey().location()) 
            + ", Extruder? " + (extruder != null) + ", Thickness: " 
            + (thickness == null ? "n/a" : thickness) 
            + ", Material? " + (materialGetter != null) + "]";
    }
}
