package com.quattage.mechano.foundation.electricity.core.anchor.interaction;

import com.quattage.mechano.MechanoSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public enum AnchorInteractSound {

    INITIATE(SoundEvents.BONE_BLOCK_BREAK), // TOOD bone sound is placeholder
    CONFIRM(MechanoSounds.CABLE_CREATE.get()),
    DENY_SOFT(SoundEvents.BONE_BLOCK_BREAK),
    DENY(SoundEvents.BONE_BLOCK_BREAK);

    private static SoundSource src = SoundSource.BLOCKS;
    private final SoundEvent sound;
    private final float volume;
    private final float pitch;

    private AnchorInteractSound(SoundEvent sound) {
        this.sound = sound;
        this.volume = 1f;
        this.pitch = 1f;
    }

    private AnchorInteractSound(SoundEvent sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void playInWorld(Level world, BlockPos pos) {
        world.playSound(null, pos, sound, src, volume, pitch);
    }
}
