package com.quattage.mechano.foundation.api.anchor;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.ServerMatrix;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.NodeMap;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;

/**
 * A side-agnostic, stand-in replacement for the {@link AnchorArray} 
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
    private boolean belongsToNetwork = false;     // indicates the nullness of the accelerated reference to the client

    private Griddable<?> host;
    private @Nullable GridUUID addr = null;

    public SurrogateNode(Griddable<?> points) {
        Objects.requireNonNull(points);
        this.host = points;
    }

    /**
     * @return <code>true</code> if this SurrogateNode refers
     * to a collection of server-sided {@link GridNode} instances
     */
    public boolean isSynced(LevelReader world) {
        if(world.isClientSide()) return belongsToNetwork;
        return owner != null;
    }

    /**
     * Updates this SurrogateNode, binding it to the given 
     * ServerMatrix.
     * @param world
     * @param newOwner
     */
    public void sync(LevelReader world, @Nullable ServerMatrix newOwner) {
        if(world.isClientSide()) {
            belongsToNetwork = true;
            return;
        }
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
    public boolean forgetIfNeeded(LevelReader world) {
        if(hasLinks(world)) return false;
        forget(world);
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
    public void forget(LevelReader world) {
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
        if(!isSynced(host.getWorld())) {
            forget(host.getWorld());
            return;
        }
        this.owner.destroy(getOrCreateAddress());
    }

    public GridUUID getOrCreateAddress() {
        if(this.addr != null) return this.addr;
        this.addr = host.createSupplementaryAddress();
        return this.addr;
    }

    public void forceHostHandoff(Griddable<?> host) {
        Objects.requireNonNull(host);
        this.host = host;
        if(addr != null) getOrCreateAddress();
    }

    public void forceAddressChange(GridUUID newAddress) {
        
    }

    public ServerMatrix getOwnerMatrix() {
        return owner;
    }

    public boolean hasLinks(LevelReader world) {
        if(world.isClientSide()) {
            for(int x = 0; x < host.getAnchors().size(); x++) {
                AnchorPoint anchor = host.getAnchor(x);
                if(anchor.getCurrentConnections() >= 0) return true;
            }
            return false;
        }
        if(!isSynced(world)) return false;
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

    public void forEachAssociated(LevelReader world, Int2ObjectOpenHashMap<GridUUID> composite, Consumer<GridNode> action) {
        ServerGrid grid = SidedGridDispatcher.server(world);
        for(Int2ObjectMap.Entry<GridUUID> subsurrogate : composite.int2ObjectEntrySet()) {
            ServerMatrix matrix = grid.getMatrixByIndex(subsurrogate.getIntKey());
            if(matrix == null || matrix.nodes == null) continue;
            GridUUID walkingAddress = subsurrogate.getValue();
            for(int x = 0; x < GridUUID.MAX_SHARED_OCCUPANCY; x++) {
                walkingAddress = walkingAddress.indexedCopy(x);
                GridNode node = matrix.nodes.get(walkingAddress);
                if(node == null) continue;
                this.owner = matrix;
                action.accept(node);
                return;
            }
        }
    }

    public @Nullable NodeMap constituents() {
        return owner == null ? null : owner.nodes == null ? null : owner.nodes;
    }

    @Override
    public String toString() {
        return "SurrogateNode(" + host + ", " + (host.getWorld().isClientSide ? "CLIENT" : "SERVER") + ", synced? : " + isSynced(host.getWorld()) + ")";
    }

    public CompoundTag writeOwnerIndex(LevelReader world, CompoundTag in) {
        if(world == null || world.isClientSide()) return in;
        in.putInt("matind", owner == null ? -1 : owner.getIndex());
        return in;
    }

    public void readOwnerIndex(LevelReader world, CompoundTag nbt) {
        if(world.isClientSide()) return;
        ServerGrid grid = SidedGridDispatcher.server(world);
        int index = -1;
        if(nbt.contains("matind")) index = nbt.getInt("matind");
        if(index >= 0) this.owner = grid.matrices.get(index);
    }
}
