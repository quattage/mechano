package com.quattage.mechano.foundation.electricity.core.anchor.interaction;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/***
 * A NodeConnectResult is a simple representation of the actions that should follow
 * when the player makes a NodeConnection. NodeConnectResults also generate their
 * own translation keys to display them to the user.
 */
public enum AnchorInteractType {
    // status 
    LINK_STARTED(true),      // this link was made (first node right clicked)
    LINK_ADDED(true),        // this link was added. applies to source and destination connections
    LINK_CANCELLED(false),   // this link was canceled (player left clicks with spool)

    // error
    LINK_INVALID(false),      // generic catch-all error for links (temporary)
    LINK_CONFLICT(false, false),     // this link's target and destination positions belong to the same block
    LINK_EXISTS(false, false),       // this link already exsists
    LINK_INCOMPATABLE(false), // this link's WireType doesn't match with the nodes allowed WireTypes

    NODE_EMPTY(false),        // this node is already empty, but the player tried to remove a link
    NODE_FULL(false, false),         // this node is already full, but the player tried to add a link
    
    WIRE_LONG(false),         // this wire is too long
    WIRE_OBSTRUCTED(false),   // this wire is obstructed by blocks (unused for now)

    // success
    WIRE_SUCCESS(true, AnchorInteractSound.CONFIRM),    // this wire was successfully created by the player
    WIRE_REMOVED(true);      // this wire was successfully removed by the player

    private final String key;
    private final Component message;
    private final boolean success;
    private final boolean fatal;
    private final AnchorInteractSound sound;

    private AnchorInteractType(boolean success) {
        this.success = success;
        this.fatal = true;
        this.key = makeKey();
        this.message = Component.translatable(key);
        this.sound = AnchorInteractSound.DENY_SOFT;
    }

    private AnchorInteractType(boolean success, boolean fatal) {
        this.success = success;
        this.fatal = fatal;
        this.key = makeKey();
        this.message = Component.translatable(key);
        this.sound = AnchorInteractSound.DENY_SOFT;
    }

    private AnchorInteractType(boolean success, AnchorInteractSound sound) {
        this.success = success;
        this.fatal = true;
        this.key = makeKey();
        this.message = Component.translatable(key);
        this.sound = sound;
    }

    private AnchorInteractType(boolean success, boolean fatal, AnchorInteractSound sound) {
        this.success = success;
        this.fatal = fatal;
        this.key = makeKey();
        this.message = Component.translatable(key);
        this.sound = sound;
    }

    /***
     * Play the sound associated with this NodeConnectResult.
     * @param world World to play the sound in.
     * @param pos BlockPos to play the sound at.
     */
    public void playConnectSound(Level world, BlockPos pos) {
        if(sound == null) return;
        sound.playInWorld(world, pos);
    }

    private String makeKey() {
        return "actionbar.mechano.connection." + this;
    }
    
    /***
     * Whether this condition represents a successful connection or not
     * @return True if this condition can proceed without potential errors.
     */
    public boolean isSuccessful() {
        return success;
    }

    /***
     * In the event of a failure case, isFatal can be used to check whether 
     * implementations proceed without completely cancelling the connection.
     * @return True if this is a fatal case, always false if this is a successful case.
     */
    public boolean isFatal() {
        if(success) return false;
        return fatal;
    }

    public Component getMessage() {
        return message;
    }

    public String getKey() {
        return key;
    }

    /***
     * Returns this NodeConnectResult's enum declaration as a String.
     */
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}