package com.quattage.mechano.foundation.api.landmarks;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.GlobalServerGrid;
import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmarks.GridNode.Tracker;
import com.quattage.mechano.foundation.api.switchboard.DispatchSyncPacket;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * A side-agnostic addressable NodeIdentifiable whose index is always 0.
 * This class does a few things: <ul>
 * 
 * <li>Simultaneously represents both logical sides (client and server)
 * and sends packets to sync each instance.
 * <li> Serves as an indicator for side-specific {@link SidedGridDispatcher} access,
 * where the client is able to tell whether or not the server-sided dispatch is
 * synced to a {@link PowerGrid}. (this is necessary because the PowerGrid does not exist on the client)
 * <li>Provides {@link #severAndForget() helper methods} for traversing the {@link GlobalServerGrid}
 * starting at this DispatchedNode's internally stored BlockEntity instance.
 * <li>Stores an {@link #owner accelerated reference} to the most relevent 
 * {@link PowerGrid} containing an address that points to this
 * DispatchedNode's {@link #pgbe internal BlockEntity},
 * skipping the need to call {@link GlobalServerGrid#lookup} and avoiding
 * brute-force iteration.
 *
 * </ul><p>
 * This class can be instantiated by implementing BlockEntities as a way
 * to for them to keep track of their PowerGrid representation on both
 * logical sides while avoiding race conditions, stale data, and large 
 * packets.
 */
public final class DispatchedNode implements NodeIdentifiable {

    /**
     * The owner is always null on the client, and sometimes null
     * on the server. A DispatchedNode with a null owner indicates
     * that this instance does not belong to a PowerGrid. 
     */
    public @Nullable PowerGrid owner;

    /**
     * A client-sided hint so that we can tell if this DispatchedNode
     * has an owner without having to send a packet to do so.
     */
    private boolean belongsToNetwork = false;

    /**
     * Never null, immutable - The host of this DispatchedNode
     * in the world. Used for getting BlockPos and level.
     */
    private final PowerGridBlockEntity pgbe;

    /**
     * Indicates (on both the server and client) the
     * amount of AnchorPoints represented by the PGBE
     */
    public int nodeCount = -1;

    public DispatchedNode(PowerGridBlockEntity pgbe) {
        Objects.requireNonNull(pgbe);
        this.pgbe = pgbe;
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
     * PowerGrid.
     * @param world
     * @param newOwner
     */
    public void sync(LevelReader world, @Nullable PowerGrid newOwner) {
        if(!world.isClientSide()) {
            belongsToNetwork = true;
            this.owner = newOwner;
            CatnipServices.NETWORK.sendToAllClients(new DispatchSyncPacket(getPos(), SidedTask.SYNC));
            return;
        }
        belongsToNetwork = true;
        this.owner = null;
        return;
    }

    /**
     * Tells this DispatchedNode to forget its references to the {@link PowerGrid}.
     * This is useful for when {@link GridLink} instances need to be removed, but
     * this method does not alter the PowerGrid itself. This can result
     * in stale references in the PowerGrid if not used carefully.
     * <p> 
     * When in doubt, use {@link DispatchedNode#severAndForget()} instead.
     * @param world
     */
    public void forget(LevelReader world) {
        if(!world.isClientSide()) {
            belongsToNetwork = false;
            this.owner = null;
            CatnipServices.NETWORK.sendToAllClients(new DispatchSyncPacket(getPos(), SidedTask.UNSYNC));
            return;
        }
        belongsToNetwork = false;
        this.owner = null;
        return;
    }

    /**
     * Nullifies this DispatchedNode's internal references
     * and removes its representation from the {@link PowerGrid}.
     * This is useful for when a block is broken or in some 
     * way disabled. {@link GridLink GridLinks} made to 
     * {@link GridNode GridNodes} that belong to the parent 
     * PGBE will be removed.
     * Calls to this method will keep this DispatchedNode 
     * instance valid so that it can be reused later.
     */
    public void severAndForget() {
        if(getLevel().isClientSide) return;
        if(!isSynced()) return;
        forEachAddress((grid, addr) -> {
            grid.removeNode(addr);
        });
        forget(getLevel());
    }

    /**
     * Executes the given consumer for each address that this DispatchedNode represents.
     * Provides access to the PowerGrid that this DispatchedNode belongs to as well as
     * a mutable key.
     * @param cons
     */
    public void forEachAddress(BiConsumer<PowerGrid, NodeIdentifier.Key> cons) {
        if(getLevel().isClientSide()) return;
        PowerGrid grid = this.owner;
        GlobalServerGrid global = SidedGridDispatcher.server(getLevel());
        NodeIdentifier.Key address = this.strip();
        if(grid == null) {
            grid = global.lookup(address).getFirst();
            if(grid == null) return;
        }
        for(int x = 0; x < nodeCount; x++) {
            address.setIndex(x);
            cons.accept(grid, address);
        }
    }

    public PowerGrid getOwner() {
        return owner;
    }

    @Override
    public @Nullable BlockPos getPos() {
        return pgbe.getBlockPos();
    }

    @Override
    public Tracker makeTrackable() {
        throw new UnsupportedOperationException("DispatchedNodes aren't trackable!");
    }

    @Override
    public int getIndex() {
        return 0;
    }

    public @Nullable Level getLevel() {
        if(pgbe.isRemoved()) return null;
        return pgbe.getLevel();
    }


    public boolean isClientSide() {
        return getLevel().isClientSide;
    }

    public String toString() {
        return "DispatchedNode(" + pgbe + ", " + (isClientSide() ? "CLIENT" : "SERVER") + ", synced? : " + isSynced() + ")";
    }















    public static enum SidedTask {

        RESYNC,
        SYNC,
        UNSYNC;

        public static final StreamCodec<ByteBuf, SidedTask> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SidedTask decode(ByteBuf buffer) {
                return SidedTask.values()[buffer.readByte()];
            }
            @Override
            public void encode(ByteBuf buffer, SidedTask value) {
                buffer.writeByte(value.ordinal());
            }
        };
    }
}
