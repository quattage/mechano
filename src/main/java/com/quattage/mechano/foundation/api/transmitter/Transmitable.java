
package com.quattage.mechano.foundation.api.transmitter;

import static com.quattage.mechano.Mechano.lang;

import java.util.List;

import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorSelector;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.UpdateResponse;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;

/**
 * Provides the skeleton implementation required for clients to interface with
 * the {@link com.quattage.mechano.foundation.api.ServerMatrix ServerMatrix}. 
 * Implementations of this interface usually subclass {@link net.minecraft.world.level.ItemLike ItemLike}.
 */
public interface Transmitable<T extends Transmitter<?>> {

    /**
     * Called continuously on the client while the player is looking at an {@link AnchorPoint} while the player is
     * holding the item associated with this transmitter.
     * Can be overridden to define custom tooltip behaviour for individual transmitters. This method is responsible
     * for both evaluating the validity of the given AnchorPoint as an interactable target, and for appending
     * tooltip elements to the provided list if applicable.
     * @param world World to operate within
     * @param tooltip The list of text components that will be appended to the displayed tooltip.
     * @param holder Holder that the target AnchorPoint belongs to
     * @param target The AnchorPoint that the player is looking at
     * @param held Container for information about the player and their held item stack
     * @return <code>AnchorResponse.GOOD</code> if the player may interact with this anchor.
     */
    default UpdateResponse collectTooltipInfoAndResponse(ClientLevel world, List<Component> tooltip, Griddable<?> points, AnchorPoint target, HoldingSummary held) {
        lang().text("hi >:)").forGoggles(tooltip);
        GridUUID prevAddress = held.stack.get(UUIDDiscriminator.ATTACHMENT);
        AnchorPoint prevAnchor = prevAddress == null ? null : prevAddress.getAnchor(world);
        if(target.equals(prevAnchor)) return UpdateResponse.FAIL_DUPLICATE;
        if(!target.isCompatableWith(getTransmitterType())) return UpdateResponse.FAIL_HELD_INCOMPATIBLE;
        if(!target.hasRoom()) return UpdateResponse.FAIL_DESTINATION_FULL;
        return UpdateResponse.TASK_SELECT_SUCCESS;
    }

    /**
     * Ticked at the framerate of the client. By default, this method is called continuously while the player
     * is holding the item associated with this transmitter. If this transmitter has no item, this method may be called in some custom
     * context or event handler as you see fit. This is not intended for pushing vertices anything, you can do that in a custom
     * renderer if that's necessary - This method's intended use is for cancelling the {@link AnchorSelector.INSTANCE}
     * ticking process early, if such a thing is ever necessary.
     * <p><strong>Note - overriding this method is not required, and this method has no implementation by default</strong>
     * @param world World to operate within
     * @param held Container for information about the player, their held item stack, and the {@link Transmitable}
     * @return <code>true</code> if the {@link AnchorSelector.INSTANCE} should carry on ticking.
     */
    public default boolean onRenderTick(LevelReader world, HoldingSummary held, DeltaTracker delta) {
        return true;
    }

    public abstract TransmitterType<T> getTransmitterType();





































    
    /**
     * Wraps information about the LocalPlayer and the item in that player's main and off hand. The player, the stack, and the TransmitterRepresentable 
     * are all stored in this wrapped object if applicable. Use {@link Transmitable#getHolding} to construct
     * this object.
     */
    public static record HoldingSummary(Player player, InteractionHand hand, ItemStack stack, Transmitable<? extends ItemLike> implementingItem) {
        public boolean isHoldingReleventItem() { return player != null && hand != null && implementingItem != null && stack != null; }
        public int bitmask() { return implementingItem == null ? 0 : implementingItem.getTransmitterType().bitmask(); }
        @Override
        public final String toString() {
            return "'" + player.getName().getString() + "'' is holding '" + implementingItem + "' in their (" + hand + ")";
        }
    }

    
    /**
     * Collects info about the player's current in-hand items and returns a container object
     * summarizing that information
     * @param player
     * @return {@link HoldingSummary} 
     */
    @SuppressWarnings("unchecked")
    public static HoldingSummary getHolding(Player player) {
        if(player == null) throw new NullPointerException("Couldn't instantiate a HoldingSummary - Player is null!");
        ItemStack stack = player.getMainHandItem();
        if(stack.getItem() instanceof Transmitable transmitterItem)
            return new HoldingSummary(player, InteractionHand.MAIN_HAND, stack, transmitterItem);
        ItemStack offStack = player.getOffhandItem();
        if(offStack.getItem() instanceof Transmitable transmitterItem)
            return new HoldingSummary(player, InteractionHand.OFF_HAND, offStack, transmitterItem);
        return new HoldingSummary(player, InteractionHand.MAIN_HAND, stack, null);
    }
}