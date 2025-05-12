package com.quattage.mechano.foundation.api.landmarks;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmarks.GridNode.Tracker;
import com.quattage.mechano.foundation.api.network.DispatchSyncClientBoundPacket;
import com.quattage.mechano.foundation.api.network.DispatchSyncServerBoundPacket;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

/**
 * A side-agnostic addressable NodeIdentifiable whose index is always 0.
 * This class can be instantiated by implementing BLockEntities as a way
 * to for them to store a {@link GridNode} reference in a thread-safe way.
 * Storing the GridNode at the BE level simplifies the amount of work
 * that has to be done when changes to the {@link GlobalServerGrid} are made.
 */
public class DispatchedNode implements NodeIdentifiable<GridNode> {

    public @Nullable PowerGrid owner;

    private boolean isSyncedAsClient = false;
    private final PowerGridBlockEntity pgbe;

    public DispatchedNode(PowerGridBlockEntity pgbe) {
        Objects.requireNonNull(pgbe);
        this.pgbe = pgbe;
    }

    /**
     * @return <code>true</code> if this DispatchedNode refers
     * to a server-sided {@link GridNode} instance
     */
    public boolean isSynced() {
        return (owner != null) || isSyncedAsClient;
    }

    /**
     * Syncs this DispatchedNode so that it refers to a server-sided
     * {@link GridNode} instance.  
     * @param networked If <code>true</code>, a packet will be sent to
     * call this method on the opposite side - If this is called on the 
     * client, a packet will be sent to the server. (and vice-versa)
     */
    public void sync(boolean networked) {

        Level world = getLevel();
        if(world == null) 
            throw new IllegalStateException("Cannot sync " + this + " - The world couldn't be obtained!");

        if(world.isClientSide()) {
            isSyncedAsClient = true;
            this.owner = null;
            if(networked)
                CatnipServices.NETWORK.sendToServer(new DispatchSyncServerBoundPacket(pgbe.getBlockPos(), SyncTask.SYNC));
            return;
        }

        SidedGridDispatcher.runOnServer(world, (grid) -> {
            Pair<PowerGrid, GridNode> lookup = grid.lookup(new NodeIdentifier.Key(getPos()));
            if(lookup == null) return;
            owner = lookup.getFirst();
        });

        if(networked) 
            CatnipServices.NETWORK.sendToAllClients(new DispatchSyncClientBoundPacket(pgbe.getBlockPos(), SyncTask.SYNC));
    }


    /**
     * Nullifies all synced references that are stored within this 
     * DispatchedNode. This is useful to prevent passive BlockEntities 
     * (ones that aren't connected to anything) from storing references 
     * to irrelevent or potentially stale PowerGrids.
     * @param networked If <code>true</code>, a packet will be sent to
     * call this method on the opposite side - If this is called on the 
     * client, a packet will be sent to the server. (and vice-versa)
     */
    public void forget(boolean networked) {

        Level world = getLevel();
        if(world == null) 
            throw new IllegalStateException("Cannot forget " + this + " - The world couldn't be obtained!");

        if(world.isClientSide()) {
            this.isSyncedAsClient = false;
            this.owner = null;
            if(networked)
                CatnipServices.NETWORK.sendToServer(new DispatchSyncServerBoundPacket(pgbe.getBlockPos(), SyncTask.UNSYNC));
            return;
        }

        this.owner = null;

        if(networked) 
            CatnipServices.NETWORK.sendToAllClients(new DispatchSyncClientBoundPacket(pgbe.getBlockPos(), SyncTask.UNSYNC));
    }

    @Override
    public @Nullable GridNode getValue() {
        if(!isSynced()) return null;
        return owner.nodes.get(pgbe.getBlockPos(), 0);
    }

    public PowerGrid getOwner() {
        return owner;
    }

    @Override
    public @Nullable BlockPos getPos() {
        return pgbe.getBlockPos();
    }

    @Override
    public @Nullable Tracker makeTrackable() {
        GridNode actual = getValue();
        if(actual == null) return null;
        return actual.makeTrackable();
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



















    public static enum SyncTask {

        RESYNC,
        SYNC,
        UNSYNC;

        public static final StreamCodec<ByteBuf, SyncTask> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SyncTask decode(ByteBuf buffer) {
                return SyncTask.values()[buffer.readByte()];
            }
            @Override
            public void encode(ByteBuf buffer, SyncTask value) {
                buffer.writeByte(value.ordinal());
            }
        };
    }
}
