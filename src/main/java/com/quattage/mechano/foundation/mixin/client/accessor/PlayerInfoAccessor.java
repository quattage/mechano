package com.quattage.mechano.foundation.mixin.client.accessor;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;

@Mixin(AbstractClientPlayer.class)
public interface PlayerInfoAccessor {
    @Accessor("playerInfo")
    @Nullable PlayerInfo mechano$getPlayerInfo();
}
