package com.quattage.mechano.core.electricity.node.connection;

import java.util.Locale;

import net.minecraft.network.chat.Component;

public enum NodeConnectResult {
    // status 
    LINK_STARTED(true),      // this link was made (first node right clicked)
    LINK_ADDED(true),        // this link was added. applies to source and destination connections
    LINK_CANCELLED(false),   // this link was canceled (player left clicks with spool)

    // error
    LINK_INVALID(false),      // generic catch-all error for links (temporary)
    LINK_CONFLICT(false),     // this link's target and destination positions belong to the same block
    LINK_EXISTS(false),       // this link already exsists
    LINK_INCOMPATABLE(false), // this link's WireType doesn't match with the nodes allowed WireTypes

    NODE_EMPTY(false),        // this node is already empty, but the player tried to remove a link
    NODE_FULL(false),         // this node is already full, but the player tried to add a link
    
    WIRE_LONG(false),         // this wire is too long
    WIRE_OBSTRUCTED(false),   // this wire is obstructed by blocks (unused for now)

    // success
    WIRE_SUCCESS(true),    // this wire was successfully created by the player
    WIRE_REMOVED(true);      // this wire was successfully removed by the player

    private final String key;
    private final Component message;
    private final boolean success;

    private NodeConnectResult(boolean success) {
        this.success = success;
        this.key = makeKey();
        this.message = Component.translatable(key);
    }

    private String makeKey() {
        return "mechano.statusbar.connection." + this;
    }
    
    public boolean isSuccessful() {
        return success;
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
