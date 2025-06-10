package com.quattage.mechano.foundation.catenary.meshing;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class UVWalker {
    
    private float[] appliedUvs = new float[4];
    private @Nullable float[] baseUvs;
    private @Nullable TextureAtlasSprite sprite;

    public UVWalker(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    public UVWalker() {}

    public void applyAtlas(TextureAtlasSprite sprite) {
        this.sprite = sprite;
        baseUvs = new float[4];
        applyAtlas();
    }

    public UVWalker setWidth(int pixels) {
        
    }

    public UVWalker walk(float vPos, float height) {
        if(baseUvs == null) {

        }
        return this;
    }

    public boolean applyAtlas() {
        appliedUvs[0] = sprite.getU(baseUvs[0]);
        appliedUvs[1] = sprite.getU(baseUvs[1]);
        appliedUvs[2] = sprite.getV(baseUvs[2]);
        appliedUvs[3] = sprite.getV(baseUvs[3]);
        return true;
    }

    public void wipeSprite() {
        if(sprite == null) return;
        this.appliedUvs[0] = baseUvs[0];
        this.appliedUvs[1] = baseUvs[1];
        this.appliedUvs[2] = baseUvs[2];
        this.appliedUvs[3] = baseUvs[3];
        baseUvs = null;
    }

    public float u0() {
        return appliedUvs[0];
    }

    public float u1() {
        return appliedUvs[1];
    }

    public float v0() {
        return appliedUvs[2];
    }

    public float v1() {
        return appliedUvs[3];
    }
}
