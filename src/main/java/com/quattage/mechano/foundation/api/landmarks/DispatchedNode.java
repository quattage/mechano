package com.quattage.mechano.foundation.api.landmarks;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.GlobalServerGrid;
import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
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
 * This class can be instantiated by implementing BLockEntities as a way
 * to for them to store a {@link GridNode} reference in a thread-safe way.
 * Storing the GridNode at the BE level simplifies the amount of work
 * that has to be done when changes to the {@link GlobalServerGrid} are made.
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

    public DispatchedNode(PowerGridBlockEntity pgbe) {
        Objects.requireNonNull(pgbe);
        this.pgbe = pgbe;
    }

    /**
     * @return <code>true</code> if this DispatchedNode refers
     * to a server-sided {@link GridNode} instance
     */
    public boolean isSynced() {
        return (owner != null) || belongsToNetwork;
    }

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
