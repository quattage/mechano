package com.quattage.mechano.foundation.api.anchor;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.ServerMatrix;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.NodeMap;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;

import net.minecraft.world.level.LevelReader;

/**
 * A side-agnostic version of the {@link AnchorArray} whose index is always 0.
 * This class does a few things: <ul>
 * 
 * <li>Simultaneously represents both logical sides (client and server)
 * and sends packets to sync each instance.
 * <li> Serves as an indicator for side-specific {@link SidedGridDispatcher} access,
 * where the client is able to tell whether or not the server-sided dispatch is
 * synced to a {@link ServerMatrix}. (this is necessary because the ServerMatrix does not exist on the client)
 * <li>Provides {@link #destroy() helper methods} for traversing the {@link ServerGrid}
 * starting at this DispatchedNode's internally stored BlockEntity instance.
 * <li>Stores an {@link #owner accelerated reference} to the most relevent 
 * {@link ServerMatrix} containing an address that points to this
 * DispatchedNode's {@link #points internal host},
 * skipping the need to call {@link ServerGrid#lookup} and avoiding
 * brute-force iteration.
 *
 * </ul><p>
 * This class should be instantiated by implementing {@link AnchorPointHostable hosts} 
 * as a way to for them to keep track of their ServerMatrix representation on both logical 
 * sides while avoiding race conditions, stale data, and large packets.
 */
public final class DispatchedAnchorNode {

    /**
     * The owner is always null on the client, and sometimes null
     * on the server. A DispatchedNode with a null owner indicates
     * that this instance does not belong to a ServerMatrix. 
     */
    private @Nullable ServerMatrix owner;

    /**
     * A client-sided hint so that we can tell if this DispatchedNode
     * has an owner without having to send a packet to do so.
     */
    private boolean belongsToNetwork = false;

    /**
     * Never null, immutable - The host of this DispatchedNode
     * in the world. Used for getting BlockPos and level.
     */
    private final AnchorPointable<?> points;

    // lazily loaded from the holder
    public @Nullable GridUUID addr = null;

    /**
     * Indicates (on both the server and client) the
     * amount of AnchorPoints represented by the PGBE
     */
    public int nodeCount = -1;

    public DispatchedAnchorNode(AnchorPointable<?> points) {
        Objects.requireNonNull(points);
        this.points = points;
    }

    /**
     * @return <code>true</code> if this DispatchedNode refers
     * to a collection of server-sided {@link GridNode} instances
     */
    public boolean isSynced() {
        return (owner != null) || belongsToNetwork;
    }

    /**
     * Updates this DispatchedNode, binding it to the given 
     * ServerMatrix.
     * @param world
     * @param newOwner
     */
    public void sync(LevelReader world, @Nullable ServerMatrix newOwner) {
        belongsToNetwork = true;
        this.owner = world.isClientSide() ? null : newOwner;
        return;
    }

    /**
     * Updates this DispatchedNode, binding it to the given 
     * ServerMatrix.
     * @param world
     * @param newOwner
     */
    public void sync(DispatchedAnchorNode other) {
        if(!other.isSynced()) {
            Mechano.LOGGER.warn("Couldn't sync dispatched node against non-synced constituent!");
            return;
        }
        belongsToNetwork = true;
        this.owner = other.owner.getWorld().isClientSide() ? null : other.owner;
        return;
    }

    /**
     * Tells this DispatchedNode to forget its references to the {@link ServerMatrix}.
     * This is useful for when {@link GridLink} instances need to be removed, but
     * this method does not alter the ServerMatrix itself. This can result
     * in stale references in the ServerMatrix if not used carefully.
     * <p> 
     * When in doubt, use {@link DispatchedAnchorNode#destroy()} instead.
     * @param world
     */
    public void forget(LevelReader world) {
        belongsToNetwork = false;
        this.owner = null;
        return;
    }

    /**
     * Nullifies this DispatchedNode's internal references
     * and removes its representation from the {@link ServerMatrix}.
     * This is useful for when a block is broken or in some 
     * way disabled. {@link GridLink GridLinks} made to 
     * {@link GridNode GridNodes} that belong to the cooresponding
     * holder/GridNode will be removed.
     * Calls to this method will keep this DispatchedNode 
     * instance valid so that it can be reused later.
     */
    public void destroy() {
        if(points.getWorld().isClientSide) return;
        if(!isSynced()) return;
        forEachAddress(ServerMatrix::removeNode);
        forget(points.getWorld());
    }

    private GridUUID getOrMakeAddress() {
        if(this.addr != null) return this.addr;
        this.addr = points.createAddress();
        return this.addr;
    }

    public ServerMatrix getOwnerMatrix() {
        return owner;
    }

    /**
     * Executes the given consumer for each address that this DispatchedNode represents.
     * Provides access to the ServerMatrix that this DispatchedNode belongs to as well as
     * a mutable key.
     * @param cons
     */
    public void forEachAddress(BiConsumer<ServerMatrix, GridUUID> cons) {
        if(points.getWorld().isClientSide()) return;
        ServerMatrix grid = this.owner;
        ServerGrid global = SidedGridDispatcher.server(points.getWorld());
        if(grid == null) {
            grid = global.lookup(getOrMakeAddress()).getFirst();
            Mechano.LOGGER.warn("Dispatch at " + getOrMakeAddress() + " had to re-acquire its parent grid.");
            if(grid == null) return;
        }
        for(int x = 0; x < nodeCount; x++)
            cons.accept(grid, getOrMakeAddress().indexedCopy(x));
    }

    public NodeMap constituents() {
        return owner == null ? new NodeMap() : owner.nodes == null ? new NodeMap() : owner.nodes;
    }
    
    @Override
    public String toString() {
        return "DispatchedNode(" + points + ", " + (points.getWorld().isClientSide ? "CLIENT" : "SERVER") + ", synced? : " + isSynced() + ")";
    }
}
