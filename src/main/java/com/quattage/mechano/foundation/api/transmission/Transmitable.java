
package com.quattage.mechano.foundation.api.transmission;

import java.util.List;

import static com.quattage.mechano.Mechano.lang;

import com.quattage.mechano.MechanoDataAttachments;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.client.AnchorPoint;
import com.quattage.mechano.foundation.api.client.AnchorSelector;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

import net.minecraft.client.DeltaTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;

/**
 * Provides the skeleton implementation required for clients to interface with
 * the {@link com.quattage.mechano.foundation.api.PowerGrid PowerGrid}. 
 * Implementations of this interface usually subclass {@link net.minecraft.world.level.ItemLike ItemLike}.
 */
public interface Transmitable<T extends Transmitter> {

    /**
     * Called continuously on the client while the player is looking at an {@link AnchorPoint} while the player is
     * holding the item associated with this transmitter.
     * Can be overridden to define custom tooltip behaviour for individual transmitters. This method is responsible
     * for both evaluating the validity of the given AnchorPoint as an interactable target, and for appending
     * tooltip elements to the provided list if applicable.
     * @param world World to operate within
     * @param tooltip The list of text components that will be appended to the displayed tooltip.
     * @param be PGBE that the target AnchorPoint belongs to
     * @param target The AnchorPoint that the player is looking at
     * @param held Container for information about the player and their held item stack
     * @return <code>AnchorResponse.GOOD</code> if the player may interact with this anchor.
     */
    default AnchorResponse collectTooltipInfoAndResponse(LevelReader world, List<Component> tooltip, PowerGridBlockEntity be, AnchorPoint target, HoldingSummary held) {
        lang().text("hi >:)").forGoggles(tooltip);
        AnchorPoint prev = AnchorPoint.retrieve(world, held.stack.get(MechanoDataAttachments.ADDRESS_COMPONENT));
        if(target.equals(prev)) return AnchorResponse.INCOMPATABLE.andHideAnchor();
        if(!target.isCompatableWith(getTransmitterType())) return AnchorResponse.INCOMPATABLE;
        if(!target.hasRoom()) return AnchorResponse.FULL;
        return AnchorResponse.GOOD;
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
        public boolean isHoldingReleventItem() { return player != null && hand != null && implementingItem != null; }
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



    /**
     * An enum-like class that describes multiple response states that can arise 
     * when the player looks at and/or right-clicks an {@link AnchorPoint} in the world.
     */
    public static class AnchorResponse {

        private final byte code;
        private boolean hide = false;

        public static final AnchorResponse GOOD = new AnchorResponse((byte)0);
        public static final AnchorResponse NONE = new AnchorResponse((byte)1);
        public static final AnchorResponse INCOMPATABLE = new AnchorResponse((byte)2);
        public static final AnchorResponse FULL = new AnchorResponse((byte)3);
        public static final AnchorResponse GENERIC = new AnchorResponse((byte)4);

        public static boolean indicatesSpecialDrawing(AnchorResponse response) {
            if(response == null) return false;
            return (response.code != NONE.code) && (!response.hide);
        }

        private AnchorResponse(byte responseCode) {
            this.code = responseCode;
        }

        public boolean hidesAnchor() {
            return hide;
        }

        /**
         * A call to this method before this AnchorResponse instance
         * is returned will cause the {@Link com.quattage.mechano.foundation.api.client.AnchorSelector AnchorSelector}
         * to skip rendering its currently selected anchor and associated tooltip.
         * <p> This may be desirable for situations where the player is denied a specific interaction,
         * but immediate feedback is not necessary.
         * Note that this may obscure information from the player (they may wonder why they can't 
         * connect a wire in a specific circumstance, and hiding the anchor will hide that information from them)
         * @return this AnchorResponse
         */
        public AnchorResponse andHideAnchor() {
            this.hide = true;
            return this;
        }

        public boolean isSuccessful() {
            return code <= 0;
        }

        public boolean is(AnchorResponse other) {
            return this.code == other.code;
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof AnchorResponse that)) return false;
            return this.is(that);
        }

        @Override
        public String toString() {
            return "(" + code + ")";
        }
    }



    /**
     * An enum-like class that describes multiple response states that can arise as a result of forming a link between
     * two {@link AnchorPoint AnchorPoints} - Used on the client for reacting to user input and providing feedback.
     */
    public static class LinkResponse {

        private final byte responseCode;
        private boolean failHard;

        public static final LinkResponse SUCCESS = new LinkResponse((byte)0);
        public static final LinkResponse FAIL_DESTINATION_UNSUPPORTED = new LinkResponse((byte)1);
        public static final LinkResponse FAIL_DESTINATION_FULL = new LinkResponse((byte)2);
        public static final LinkResponse FAIL_USER_CANCEL = new LinkResponse((byte)3);
        public static final LinkResponse FAIL_DUPLICATE = new LinkResponse((byte)4);
        public static final LinkResponse FAIL_TOO_CLOSE = new LinkResponse((byte)5);
        public static final LinkResponse FAIL_TOO_FAR = new LinkResponse((byte)6);
        public static final LinkResponse FAIL_DIMENSION_MISMATCH = new LinkResponse((byte)7);
        public static final LinkResponse FAIL_SYNC_OUTDATED = new LinkResponse((byte)8);
        public static final LinkResponse FAIL_GENERIC = new LinkResponse((byte)9);

        private LinkResponse(byte responseCode) {
            this.responseCode = responseCode;
        }


        /**
         * If this method is called, this response will cause the 
         * connection that the player is currently making to immediately cancel
         * itself and return to its passive state. 
         * @return this ConnectionResponse
         */
        public LinkResponse andBailout() {
            this.failHard = true;
            return this;
        }

        public boolean isSuccessful() {
            return responseCode <= 0;
        }

        public boolean is(LinkResponse other) {
            return this.responseCode == other.responseCode;
        }

        @Override
        public boolean equals(Object obj) {
            if(!(obj instanceof LinkResponse that)) return false;
            return this.is(that);
        }

        public boolean shouldBail() {
            return failHard;
        }

    }
}