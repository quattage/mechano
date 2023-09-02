package com.quattage.mechano.foundation.electricity.core.connection;

import com.quattage.mechano.MechanoSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public enum NodeConnectSound {
    INITIATE(SoundEvents.BONE_BLOCK_BREAK),
    CONFIRM(MechanoSounds.CABLE_CREATE.get()),
    DENY_SOFT(SoundEvents.BONE_BLOCK_BREAK),
    DENY(SoundEvents.BONE_BLOCK_BREAK);

    private static SoundSource src = SoundSource.BLOCKS;
    private final SoundEvent sound;
    private final float volume;
    private final float pitch;

    private NodeConnectSound(SoundEvent sound) {
        this.sound = sound;
        this.volume = 1f;
        this.pitch = 1f;
    }

    private NodeConnectSound(SoundEvent sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    public void play(Level world, BlockPos pos) {
        world.playSound(null, pos, sound, src, volume, pitch);
    }
}
