
package com.quattage.mechano.foundation.api.grid;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.api.grid.landmarks.GridLink;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Provides a set of callbacks for interfacing with the {@link com.quattage.mechano.foundation.api.grid.PowerGrid PowerGrid}
 * in-game. Implementations of this interface should subclass {@link net.minecraft.world.level.ItemLike ItemLike}.
 */
public interface TransferProtocolRepresentable {

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
     * @return <code>TRUE</code> if this connection should proceed.
     */
    abstract boolean onConnectionCreated(Level world, Player creator, GridLink connection); // TODO LivingEntity instead of player?

    /**
     * A callback for when connections are made. 
     * Identical to {@link TransferProtocolRepresentable#onConnectionCreated onConnectionCreated}
     * but this callback doesn't receive a player. <p>
     * 
     * Only called on the server.
     * 
     * @param world World to operate within
     * @param connection The connection that was made
     * @return <code>TRUE</code> if this connection should proceed
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
     * @return <code>TRUE</code> if removing the connection should proceed
     */
    abstract boolean onConnectionDestroyed(Level world, Player destroyer, GridLink connection);

    /**
     * A callback for when a connection is destroyed. 
     * Identical to {@link TransferProtocolRepresentable#onConnectionDestroyed onConnectionDestroyed}
     * but this callback doesn't receive a player. <p>
     * 
     * Only called on the server.
     * 
     * @param world World to operate within
     * @param connection The connection about to be destroyed. 
     */
    abstract boolean onConnectionDestroyedAnonymous(Level world, GridLink connection);

    /**
     * @return The ResourceLocation of the implementing item, or <code>null</code> if this TPR is not being implemented by an item.
     */
    public default @Nullable ResourceLocation getID() {
        Item item = get();
        if(item == null) return null;
        return BuiltInRegistries.ITEM.getKey(get());
    }

    /**
     * Gets the object associated with this TPR.
     * Associated objects are usually ItemLike instances. For example,
     * A "copper wire" transfer protocol would have its associated
     * object be a "copper wire spool" item
     * @return The item/object associated with this TransferProtocol.
     */
    public abstract @Nullable Item get();

    /**
     * Gets an arbitrary integer associated with this TPR. 
     * Similar in utility to {@link TransferProtocolRepresentable#getID getID}
     * but for situations where numerical comparisons (like the index of an array)
     * are useful. 
     */
    public default int getNumericalID() {
        if(get() != null)
            return get().hashCode();
        return -1;
    }
}
