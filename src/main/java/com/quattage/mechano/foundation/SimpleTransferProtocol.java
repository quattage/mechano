package com.quattage.mechano.foundation;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.grid.GlobalServerGrid;
import com.quattage.mechano.foundation.api.grid.ProtocolTransferable;
import com.quattage.mechano.foundation.api.grid.landmarks.GridLink;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public abstract class SimpleTransferProtocol {

    // public static EmptyProtocol EMPTY = new EmptyProtocol();

    private int index = -1;
    protected boolean enabled = true;
    private final @Nullable ProtocolTransferable backer;

    /**
     * Create a new TransferProtocol from the data contained within a
     * CompoundTag. Looks up the numerical ID of the protocol in the 
     * {@link TransferProtocolRegistry} 
     * @param in Input Compoundtag to read from
     * @return a TransferProtocol instance at the given ID, or {@link SimpleTransferProtocol#EMPTY} if the ID couldn't be found in the registry. 
     */
    public static SimpleTransferProtocol loadFrom(CompoundTag in) {
        if(!in.contains("id")) throw new IllegalArgumentException("Can't get a TransferProtocol from '" + in + "' - this CompoundTag does not contain relevent data!");
        int id = in.getInt("id");
        SimpleTransferProtocol resultant = GlobalServerGrid.PROTOCOLS.get(id);
        resultant.readFrom(in);
        return resultant;
    }

    public SimpleTransferProtocol(ProtocolTransferable backer) {
        this.backer = backer;
    }

    public SimpleTransferProtocol() {
        this.backer = null;
    }

    /**
     * By default, this method defers its implementation to its internal
     * {@link ProtocolTransferable} instance.
     * @see ProtocolTransferable#onConnectionCreated
     * @see ProtocolTransferable#onConnectionCreatedAnonymous
     */
    public boolean onConnectionCreated(Level world, @Nullable Player creator, GridLink connection) {
        if(backer == null) return true;
        if(creator == null)
            return backer.onConnectionCreatedAnonymous(world, connection);
        return backer.onConnectionCreated(world, creator, connection);
    }

    /**
     * By default, this method defers its implementation to its internal
     * {@link ProtocolTransferable} instance.
     * @see ProtocolTransferable#onConnectionDestroyed
     * @see ProtocolTransferable#onConnectionDestroyedAnonymous
     */
    public boolean onConnectionDestroyed(Level world, @Nullable Player destroyer, GridLink connection) {
        if(backer == null) return true;
        if(destroyer == null)
            return backer.onConnectionDestroyedAnonymous(world, connection);
        return backer.onConnectionDestroyed(world, destroyer, connection);
    }

    /**
     * A protocol's cost can be thought of in literal terms as the resistance of a wire.
     * A high cost value means that energy is less inclined to use this protocol when being 
     * transmitted. Energy will search for {@link GridLink GridLinks} with a lower cost over 
     * this one. (think 'path of least resistance') A cost of zero represents a perfect conductor.
     * @return A positive number representing this TransferProtocol's priority over others.
     */
    public abstract int getCost();

    /**
     * Compare this TransferProtocol to another based on arbitrary characteristics
     * defined by both. TransferProtocols are compared against one another to determine
     * which one represents the lowest "quality" connection in a {@link GridPath}.
     * 
     * Implementations should override this method if they intend to define some
     * kind of transfer rate or equivalent functionality - Lower transfer rates
     * should take priority over higher ones in order to create realistic bottlenecks.
     * 
     * By default, this implementation only compares the cost defined by
     * {@link SimpleTransferProtocol#getCost getCost} - highest cost wins.
     * @param other
     * @return
     */
    public SimpleTransferProtocol compareTo(SimpleTransferProtocol other) {
        if(this.canTransfer() && !other.canTransfer()) return this;
        if(!this.canTransfer() && other.canTransfer()) return other;
        if(other.getCost() > this.getCost()) return other;
        return this;
    }

    /**
     * To be called only by registry systems
     */
    public void setID(int id) {
        this.index = id;
    }

    /**
     * An arbitrary switch to turn this protocol on/off.
     * Used by path indexing to determine 
     * @return <code>true</code> if this TransferProtocol is enabled
     */
    public boolean canTransfer() {
        return enabled;
    }

    public int getID() {
        return index;
    }

    public String toString() {
        return "TransferProtocol('" + getName() + ",' index " + getID() + ")";
    }

    /**
     * Write additional data associated with this TransferProtocol to nbt.
     * Note: The tag <code>in</code> already contains
     * data pertaining to the owner's destination address,
     * so "x", "y," "z," "i," and "id" are reserved keywords for this
     * address. Any data you serialize should not use these
     * keys.
     * @param in CompoundTag to write to
     * @return the input CompoundTag, modified
     */
    public abstract CompoundTag writeTo(CompoundTag in);

    /**
     * Read additional data associated with this TransferProtocol from nbt.
     * The PowerGrid will manage this protocol's address and indexing for you,
     * so this is only for additional non-transient data that you'd like to serialize 
     * along side the internal stuff.
     * @param in CompoundTag to read from
     */
    public abstract void readFrom(CompoundTag in);

    /**
     * Gets the string name of this TransferProtocol.
     * The name is useful for having some way to find
     * this TransferProtocol by an ID when you don't have
     * access to its index in the {@link TransferProtocolRegistry}
     * @return String name of this TransferProtocol.
     */
    public abstract String getName();
}
