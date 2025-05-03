
package com.quattage.mechano.foundation.api.grid;

import java.util.List;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.grid.client.AnchorPoint;
import com.quattage.mechano.foundation.api.grid.client.AnchorPoints;
import com.quattage.mechano.foundation.api.grid.landmarks.GridLink;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * Provides a set of callbacks for interfacing with the {@link com.quattage.mechano.foundation.api.grid.PowerGrid PowerGrid}
 * in-game. Implementations of this interface should subclass {@link net.minecraft.world.level.ItemLike ItemLike}.
 */
public interface ProtocolTransferable {

    /**
     * A callback for when connections are made,
     * which can be useful for sequencing server-sided
     * events related to this item. <p>
     * 
     * Note: You may return <code>FALSE</code> to cancel 
     * the creation of this connection. Keep in mind that if you do this,
     * the provided GridLink instance becomes stale, and should not be stored. <p>
     * 
     * Only called on the server.
     * 
     * @param world World to operate within
     * @param creator The player that created the connection
     * @param conection The connection that was made
     * @return <code>true</code> if this connection should proceed.
     */
    abstract boolean onConnectionCreated(Level world, Player creator, GridLink connection); // TODO LivingEntity instead of player?

    /**
     * A callback for when connections are made. 
     * Identical to {@link ProtocolTransferable#onConnectionCreated onConnectionCreated}
     * but this callback doesn't receive a player. <p>
     * 
     * Only called on the server.
     * 
     * @param world World to operate within
     * @param connection The connection that was made
     * @return <code>true</code> if this connection should proceed
     */
    abstract boolean onConnectionCreatedAnonymous(Level world, GridLink connection);

    /**
     * A callback for when a connection is destroyed. 
     * Useful for defining item drops for a destroyed wire. <p>
     * Note: This method is called right *before* the connection is destroyed.
     * This means that you don't have access to the PowerGrid in its new state
     * after the link is destroyed. <p>
     * Additionally, you may return <code>FALSE</code> to cancel the 
     * destruction of the provided GridLink instance. <p>
     * 
     * Only called on the server.
     * 
     * @param world World to operate within
     * @param destroyer The player that destroyed the connection
     * @param connection The connection about to be destroyed. 
     * @return <code>true</code> if removing the connection should proceed
     */
    abstract boolean onConnectionDestroyed(Level world, Player destroyer, GridLink connection);

    /**
     * A callback for when a connection is destroyed. 
     * Identical to {@link ProtocolTransferable#onConnectionDestroyed onConnectionDestroyed}
     * but this callback doesn't receive a player. <p>
     * 
     * Only called on the server.
     * 
     * @param world World to operate within
     * @param connection The connection about to be destroyed. 
     */
    abstract boolean onConnectionDestroyedAnonymous(Level world, GridLink connection);

    /**
     * Called continuously on the client while the player is looking at an {@link AnchorPoint} while the player is
     * holding the item associated with this protocol.
     * Can be overridden to define custom tooltip behaviour for individual protocols. This method is responsible
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
        if(!target.isCompatableWith(this)) return AnchorResponse.INCOMPATABLE;
        if(!target.hasRoom()) return AnchorResponse.FULL;
        return AnchorResponse.GOOD;
    }

    /**
     * @return The ResourceLocation of the implementing item, or <code>null</code> if this protocol is not being implemented by an item.
     */
    public default @Nullable ResourceLocation getID() {
        Item item = get();
        if(item == null) return null;
        return BuiltInRegistries.ITEM.getKey(get());
    }

    /**
     * Gets the object associated with this protocol.
     * Associated objects are usually ItemLike instances. For example,
     * A "copper wire" transfer protocol would have its associated
     * object be a "copper wire spool" item
     * @return The item/object associated with this TransferProtocol.
     */
    public abstract @Nullable Item get();

    /**
     * Ticked at the framerate of the client. By default, this method is called continuously while the player
     * is holding the item associated with this protocol. If this protocol has no item, this method may be called in some custom
     * context or event handler as you see fit. This is not intended for pushing vertices anything, you can do that in a custom
     * renderer if that's necessary - This method's intended use is for cancelling the {@link AnchorPoints.Selector}
     * ticking process early, if such a thing is ever necessary.
     * <p><strong>Note - overriding this method is not required, and this method has no implementation by default</strong>
     * @param world World to operate within
     * @param held Container for information about the player, their held item stack, and the {@link ProtocolTransferable}
     * @return <code>true</code> if the {@link AnchorPoints.Selector} should carry on ticking.
     */
    public default boolean onRenderTick(LevelReader world, HoldingSummary held, DeltaTracker delta) {
        return true;
    }

    /**
     * Gets an arbitrary integer associated with this protocol. 
     * Similar in utility to {@link ProtocolTransferable#getID getID}
     * but for situations where numerical comparisons (like the index of an array)
     * are useful. 
     */
    public default int getNumericalID() {
        if(get() != null)
            return get().hashCode();
        return -1;
    }

    public default int bitmask() {
        return 1 << getNumericalID();
    }

    /**
     * Wraps information about the LocalPlayer and the item in that player's main and off hand. The player, the stack, and the TransferProtocolRepresentable 
     * are all stored in this wrapped object if applicable. Use {@link ProtocolTransferable#getHolding} to construct
     * this object.
     */
    public static record HoldingSummary(LocalPlayer player, InteractionHand hand, ItemStack stack, ProtocolTransferable protocol) {
        public boolean isHoldingReleventItem() { return player != null && hand != null && protocol != null; }
        public int bitmask() { return protocol == null ? 0 : protocol.bitmask(); }
        @Override
        public final String toString() {
            return "(" + player.getName() + " is holding " + protocol.getID() + " in their " + hand + ")";
        }
    }

    /**
     * Collects info about the player's current in-hand items and returns a container object
     * summarizing that information
     * @param player
     * @return {@link HoldingSummary} 
     */
    public static HoldingSummary getHolding(LocalPlayer player) {
        if(player == null) throw new NullPointerException("Couldn't instantiate a HoldingSummary - Player is null!");
        ItemStack stack = player.getMainHandItem();
        if(stack.getItem() instanceof ProtocolTransferable protocolItem)
            return new HoldingSummary(player, InteractionHand.MAIN_HAND, stack, protocolItem);
        ItemStack offStack = player.getOffhandItem();
        if(offStack.getItem() instanceof ProtocolTransferable protocolItem)
            return new HoldingSummary(player, InteractionHand.OFF_HAND, offStack, protocolItem);
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
         * If this method is called, this response will hide the
         * targeted anchor from the user. Sometimes this may be desirable,
         * but it can obscure information from the player (they may wonder why they can't 
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
        public static final LinkResponse FAIL_GENERIC = new LinkResponse((byte)7);

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