package com.quattage.mechano.foundation.item;

import com.quattage.mechano.MechanoClientEvents;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * funny lil thing
 * TODO what happens if the player rebinds left click lol
 */
public interface LeftClickCapturable {
    /**
     * Fired by the {@link MechanoClientEvents#onLeftClick client event}
     * whenever the player left clicks holding this item
     * @param player
     * @param stack
     * @return <code>true</code> if vanilla left click behaviour with this
     * item should be cancelled in favor of the implementation in this method.
     */
    abstract boolean onLeftClick(LocalPlayer player, ItemStack stack, InteractionHand hand);
}