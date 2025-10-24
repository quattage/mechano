package com.quattage.mechano.api.griddable;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.ServerMatrix;
import com.quattage.mechano.api.SidedGridDispatcher;
import com.quattage.mechano.api.anchor.AnchorCollection;
import com.quattage.mechano.api.anchor.AnchorPoint;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.api.landmark.GridLink;
import com.quattage.mechano.api.landmark.GridNode;
import com.quattage.mechano.api.landmark.NodeMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A side-agnostic, stand-in replacement for the {@link AnchorCollection} 
 * for use in situations where the {@link AnchorPoint}
 * needs to communicate with the {@link GridNode} and vice-versa 
 * particularly in situations where the contents of either need to 
 * be altered by the opposing side. An {@link #owner accelerated reference}
 * to the most relevent {@link ServerMatrix} is stored here, so that it 
 * doesn't have to be looked up in the {@link GlobalGrid} whenever
 * modifications need to be made.
 * 
 *<p>
 *
 * This class should be instantiated by an implementing {@link Griddable} subclass
 * as a way to for it to keep track of its {@link ServerMatrix} representation on both
 * logical sides.
 */
public final class SurrogateNode {

    private @Nullable ServerMatrix owner;         // accelerated reference - always null on the client
    private boolean belongsToNetwork = false;     // indicates the server-sided nullness of the accelerated reference to the client
    private @NotNull Griddable<?> host;
    private @Nullable GridUUID addr = null;       // lazily populated - never refer to this directly, instead use getOrCreateAddress()

    public SurrogateNode(Griddable<?> points) {
        Objects.requireNonNull(points);
        this.host = points;
    }

    /**
     * @return <code>true</code> if this SurrogateNode refers
     * to a collection of server-sided {@link GridNode} instances
     */
    public boolean isSynced() {
        if(host.getWorld().isClientSide()) return belongsToNetwork;
        return owner != null;
    }

    /**
     * Updates this SurrogateNode, binding it to the given 
     * ServerMatrix.
     * @param world
     * @param newOwner
     */
    public void sync(ServerMatrix newOwner) {
        if(host.getWorld().isClientSide) {
            belongsToNetwork = true;
            return;
        }
        Objects.requireNonNull(newOwner);
        this.owner = newOwner;
    }

    /**
     * Runs {@link #forget} only if this SurrogateNode has no 
     * links. This is useful for severing the persistent data
     * associated with a {@link GridNode} whenever all connections
     * leading to/from a Griddable are destroyed.
     * @param world
     * @return
     */
    public boolean forgetIfNeeded() {
        if(hasLinks()) return false;
        forget();
        return true;
    }

    /**
     * Tells this SurrogateNode to forget its references to the 
     * {@link ServerMatrix}. This is useful for when 
     * {@link GridLink} instances need to be removed, but this 
     * method does not alter the ServerMatrix itself. This can 
     * result in stale references in the ServerMatrix if not 
     * used carefully.
     * <p> 
     * When in doubt, use {@link SurrogateNode#destroy()} instead, 
     * which calls {@link #forgetIfNeeded} internally.
     * @param world
     */
    public void forget() {
        belongsToNetwork = false;
        this.owner = null;
        return;
    }

    /**
     * Nullifies this SurrogateNode's internal references
     * and removes its representation from the {@link ServerMatrix}.
     * This is useful for when a block is broken or in some 
     * way disabled. {@link GridLink GridLinks} made to 
     * {@link GridNode GridNodes} that belong to the cooresponding
     * holder/GridNode will be removed.
     * Calls to this method will keep this SurrogateNode 
     * instance valid so that it can be reused later.
     */
    public void destroy() {
        if(host.getWorld().isClientSide) {
            Mechano.LOGGER.warn("Skipped attempt to destory surrogate " + getOrCreateAddress() + " on the client");
            return;
        }
        if(!isSynced()) {
            forget();
            return;
        }
        this.owner.destroy(getOrCreateAddress());
    }

    /**
     * The first call to this method creates & stores an internal reference to the 
     * {@link Griddable#createSupplementaryAddress() supplementary address} returned
     * by the {@link Griddable host} of this SurrogateNode. Subsequent calls will
     * refer to this stored reference. 
     * @return {@link GridUUID unindexed address} of this SurrogateNode
     */
    public GridUUID getOrCreateAddress() {
        if(this.addr != null) return this.addr;
        this.addr = host.createSupplementaryAddress();
        return this.addr;
    }

    /**
     * Calls to this method indicate that this SurrogateNode has been moved
     * from one {@link Griddable host} to another. This method will
     * nullify the {@link getOrCreateAddress cached supplimentary address}
     * and replace the internal host reference.
     * @param host {@link Griddable} to handoff instantiating control to.
     * if a <code>null</code> is passed here, this method will throw.
     */
    public void forceHostHandoff(Griddable<?> host) {
        Objects.requireNonNull(host);
        this.host = host;
        this.addr = null;
        this.owner = host.getSurrogate().getOwnerMatrix();
    }

    /**
     * Gets the {@link ServerMatrix} that this Surrogate 
     * is participating in. If this Surorgate node is not
     * {@link #isSynced(LevelReader) discoverable,} this
     * method will return <code>null.</code>
     * @return The ServerMatrix containing {@link GridUUID addresses}
     * belonging to this SurrogateNode. Will always return <code>null</code>
     * when called from the client.
     */
    public @Nullable ServerMatrix getOwnerMatrix() {
        return owner;
    }

    /**
     * Gets and returns the {@link NodeMap} that contains this SurrogateNode.
     * This will always return null if called on the client.
     * @return 
     * @see {@link SurrogateNode#getOwnerMatrix()}
     */
    public @Nullable NodeMap constituents() {
        return owner == null ? null : owner.nodes == null ? null : owner.nodes.isEmpty() ? null : owner.nodes;
    }

    /**
     * Gets and returns the first discoverable {@link GridNode}
     * in the constituents map. This GridNode's address will 
     * match the host's {@link #getOrCreateAddress() supplimentary address}
     * @return The primary GridNode (whose address is at index 0) 
     * belonging to this SurrogateNode's host
     */
    public @Nullable GridNode getSelf() {
        NodeMap friends = constituents();
        if(friends == null || friends.isEmpty()) return null;
        return friends.get(getOrCreateAddress());
    }

    /**
     * 
     * @param world
     * @return
     */
    public boolean hasLinks() {

        if(host.getWorld().isClientSide()) return hasLinksClient();

        if(!isSynced()) return false;
        NodeMap friends = constituents();
        if(friends == null || friends.isEmpty()) return false;
        GridUUID walkingAddress = getOrCreateAddress();
        for(int x = 0; x < GridUUID.MAX_SHARED_OCCUPANCY; x++) {
            walkingAddress = walkingAddress.indexedCopy(x);
            GridNode node = friends.get(walkingAddress);
            if(node == null) continue;
            if(node.hasLinks()) return true;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    private boolean hasLinksClient() {
        AnchorCollection anchors = host.getAnchors();
        return anchors != null && anchors.hasAnyConnections();
    }

    /**
     * Iterates over all {@link GridNode} instances sharing the same unindexed ID
     * as this SurrogateNode that are currently reachable in the {@link ServerGrid}. 
     * This method does nothing when called on the client.
     * @param action Action to execute on GridNodes. 
     * The GridNode instance supplied to this consumer will never be null or invalid.
     */
    public void forEachAssociated(Consumer<GridNode> action) {
        NodeMap friends = constituents();
        if(friends == null || friends.isEmpty()) return;
        GridUUID walkingAddress = getOrCreateAddress();
        for(int x = 0; x < GridUUID.MAX_SHARED_OCCUPANCY; x++) {
            walkingAddress = walkingAddress.indexedCopy(x);
            GridNode node = friends.get(walkingAddress);
            if(node == null) continue;
            action.accept(node);
            return;
        }
    }

    @Override
    public String toString() {
        return "SurrogateNode(" + host + ", " + (host.getWorld().isClientSide ? "CLIENT" : "SERVER") + ", synced? : " + isSynced() + ")";
    }

    public CompoundTag writeMatrixOwner(LevelReader world, CompoundTag in) {
        if(world == null || world.isClientSide()) return in;
        in.putInt("matind", owner == null ? -1 : owner.getIndex());
        return in;
    }

    public void readMatrixOwner(LevelReader world, CompoundTag nbt) {
        if(world.isClientSide()) return;
        ServerGrid grid = SidedGridDispatcher.server(world);
        int index = -1;
        if(nbt.contains("matind")) index = nbt.getInt("matind");
        if(index >= 0) this.owner = grid.matrices.get(index);
    }
}
