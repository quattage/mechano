package com.quattage.mechano.foundation.api.landmarks;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmarks.GridNode.Tracker;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/**
 * A side-agnostic addressable GridNode whose index is always 0.
 * Used as a quick way to cache power grid information at the BE level
 * so that it doesn't have to be searched for using brute-force methods.
 */
public class DispatchedNode implements NodeIdentifiable<GridNode> {

    private @Nullable PowerGrid owner;

    private boolean isSyncedAsClient = false;
    private final PowerGridBlockEntity pgbe;

    public DispatchedNode(PowerGridBlockEntity pgbe) {
        Objects.requireNonNull(pgbe);
        this.pgbe = pgbe;
    }

    public boolean isSynced() {
        return (owner != null) || isSyncedAsClient;
    }

    public void sync(LevelReader world, boolean networked) {
        if(world.isClientSide()) {
            isSyncedAsClient = true;
            // CatnipServices.NETWORK.sendToAllClients(new DispatchSyncServerBoundPacket())
        }
    }

    public void forget(boolean networked) {
        if(!isSynced()) return;
        this.owner = null;
    }

    @Override
    public @Nullable GridNode getValue() {
        if(!isSynced()) return null;
        return owner.nodes.get(pgbe.getBlockPos(), 0);
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
