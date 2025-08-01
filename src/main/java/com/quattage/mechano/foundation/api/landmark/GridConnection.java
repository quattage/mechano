package com.quattage.mechano.foundation.api.landmark;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.VoxelUUID;
import com.quattage.mechano.foundation.api.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.catenary.Tensionable;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public abstract sealed class GridConnection implements Tensionable, TrackedStreamable permits GridLink, GridCatenary, ConnectionKey {
    
    public abstract GridUUID getStart();
    public abstract GridUUID getEnd();
    public abstract boolean isClientSide();

    protected Transmitter<?> trns;

    public static float getEuclideanDistance(LevelReader world, GridUUID a, GridUUID b) {
        Vec3 aPos = a.getPos(world);
        Vec3 bPos = b.getPos(world);
        return (float)Math.sqrt(
            Math.pow(aPos.x - bPos.x, 2) +
            Math.pow(aPos.y - bPos.y, 2) +
            Math.pow(aPos.z - bPos.z, 2)
        );
    }

    public static void sendToClientsTracking(ServerLevel world, GridUUID start, @Nullable GridUUID end, CustomPacketPayload packet, InsertionPolicy mode) {
        MinecraftServer server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        if(mode == InsertionPolicy.SYMMETRIC) {
            for(ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(start.isBeingTrackedBy(player)) {
                    if(end == null) {
                        CatnipServices.NETWORK.sendToClient(player, packet);
                        continue;
                    }
                    if(end.isBeingTrackedBy(player))
                        CatnipServices.NETWORK.sendToClient(player, packet);
                }
            }
            return;
        }
        if(mode == InsertionPolicy.SINGLE) {
            for(ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(start.isBeingTrackedBy(player))
                    CatnipServices.NETWORK.sendToClient(player, packet);
                else if(end != null && end.isBeingTrackedBy(player))
                    CatnipServices.NETWORK.sendToClient(player, packet);
            }
            return;
        }
        if(mode == InsertionPolicy.ORDERED) {
            TrackedStreamable priority = TrackedStreamable.orderedByAssertionPriority(world, start, end)[0];
            for(ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(priority.isBeingTrackedBy(player))
                    CatnipServices.NETWORK.sendToClient(player, packet);
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported InsertionPolicy '" + mode + "'");
    }


    public GridConnection(Transmitter<?> trns) {
        this.trns = trns;
    }

    public TrackedStreamable getPrimaryConstruct(LevelReader world) {
        return TrackedStreamable.orderedByAssertionPriority(world, getStart(), getEnd())[0];
    }

    @Override
    @OnlyIn(Dist.CLIENT) 
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        return hasPoints() ? (getStart().isInFrustum(world, view) && getEnd().isInFrustum(world, view)) : false;
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        return hasPoints() ? (getStart().isBeingTrackedBy(player) || getEnd().isBeingTrackedBy(player)) : false;
    }

    @Override
    public void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet) {
        if(world.isClientSide() || isClientSide()) throw new IllegalArgumentException("Attempted to send a client-bound packet as a client! YOU CAN'T DO THAT!!!!");
        sendToClientsTracking(world, getStart(), getEnd(), packet, InsertionPolicy.SYMMETRIC);
    }

    @Override
    public boolean isInsideOf(LevelReader world, SectionPos section) {
        return hasPoints() ? (getStart().isInsideOf(world, section) || getEnd().isInsideOf(world, section)) : false;
    }

    @Override
    public boolean isInsideOf(LevelReader world, ChunkPos chunk) {
        return hasPoints() ? (getStart().isInsideOf(world, chunk) || getEnd().isInsideOf(world, chunk)) : false;
    }

    @Override
    public int getSectionY(LevelReader world) {
        if(!hasPoints()) throw new IllegalStateException("Can't get sectionY for connection with null point(s)!");
        return getStart().getSectionY(world);
    }

    @Override
    public IAttachmentHolder getDataStorageHolder(LevelReader world) {
        assertWorldly(world);
        if(!hasPoints()) throw new IllegalStateException("Can't get data storage holder for connection with null point(s)");
        return getStart().getDataStorageHolder(world);
    }

    public void updateShape(LevelReader world, float pTicks) {}

    public boolean isBeingTrackedBy(ServerPlayer player, InsertionPolicy mode) {
        if(mode == InsertionPolicy.SINGLE) return getStart().isBeingTrackedBy(player) || getEnd().isBeingTrackedBy(player);
        if(mode == InsertionPolicy.SYMMETRIC) return getStart().isBeingTrackedBy(player) && getEnd().isBeingTrackedBy(player);
        return false;
    }

    /**
     * Ensures that the given world is side-matched with this Connection instance.
     * Throws if this is not the case.
     */
    public void assertWorldly(LevelReader world) {
        if(world.isClientSide() == this.isClientSide()) return;
        if(world.isClientSide() && !this.isClientSide()) {
            throw new IllegalArgumentException("Sided mismatch encountered while removing " 
                + this + " - Server-sided links cannot be added to client holders!");
        } 
        if(!world.isClientSide() && this.isClientSide()) {
            throw new IllegalArgumentException("Sided mismatch encountered while removing " 
                + this + " - Client-sided links cannot be added to server holders!");
        }
    }

    /**
     * Ensures that the start/end {@link GridUUID addresses} can move,
     * regardless of whether or not its owner can. This is used to
     * allow attached wires and other rendering features to refresh
     * dynamically in cases where they would otherwise automatically 
     * freeze to save performance. 
     * 
     * <h3>Broadcasting with Data Scopes</h3>
     * This {@Link GridConnection instance} will need
     * will need to be reasserted at the time of invocation
     * in order for changes to be reflected correctly. 
     * This is because this method call changes the 
     * {@link DataScope} of this AnchorPoint's address,
     * which will change where the {@link GridCatenary}
     * is stored in the {@Link LinkDataStorable}.
     * This is done automatically by calling 
     * 
     * {@link GridCatenary#startMoving} or {@link GridCatenary#freezeInPlace}
     * on the client. On the server, this method does not need to be called
     * at all, since there is no distinction made between LevelChunk and
     * BlockEntity data attachments there.
     * 
     * <p> Calls to this method
     * will result in no change of this AnchorPoint's address
     * already belongs to movable construct, such as an entity.
     */
    public void makeLocallyDynamic(LevelReader world) {
        if(getStart().getDataScope(world) == DataScope.STATIC_CHUNK)
            getStart().setDataScope(DataScope.BLOCKENTITY);
        if(getEnd().getDataScope(world) == DataScope.STATIC_CHUNK)
            getEnd().setDataScope(DataScope.BLOCKENTITY);
    }

    /** 
     * @return <code>true</code> if this Connection's start and end points are not <code>null.</code>
     */
    public boolean hasPoints() {
        return getStart() != null && getEnd() != null;
    }

    /**
     * Creates a shallow copy of this GridConnection retaining all internal 
     * data, but with its start and end addresses swapped.
     * @return A new GridConnection instance
     */
    public abstract GridConnection inverseCopy();
    public abstract CompoundTag writeTo(CompoundTag in);
    public abstract String getConnectionTypeName();
    public boolean canTraverse() { return getStart() != null && getEnd() != null && trns != null && trns.isEnabled(); }
    public Transmitter<?> getTransmitter() { return trns; }
    public boolean startsWith(GridUUID address) { return getStart().equals(address); }
    public boolean endsWith(GridUUID address) { return getEnd().equals(address); }
    public boolean involves(GridUUID address) { return startsWith(address) || endsWith(address); }

    @Override
    public void sendLevelUpdates(Level world) {
        // if(!hasPoints() || getStart().canMoveDynamically() || getEnd().canMoveDynamically()) 
        //     return;
        // if(getStart().isInsideOf(world, new ChunkPos(getEnd().getBlockPos(world)))) {
        //     getEnd().sendChunkUpdates(world);
        //     return;
        // }
        getStart().sendLevelUpdates(world);
        getEnd().sendLevelUpdates(world);
    }

    @Override
    public boolean equals(Object other) {
        if(other == this) return true;
        if(!(other instanceof GridConnection that)) return false;
        return (this.getStart().equals(that.getStart()) && this.getEnd().equals(that.getEnd())) 
            || (this.getStart().equals(that.getEnd()) && this.getEnd().equals(that.getStart()));
    }

    @Override
    public int hashCode() { 
        return getStart().hashCode() + getEnd().hashCode(); 
    }

    @Override
    public String toString() { 
        return getConnectionTypeName() + "[" + getStart() + " -> " + getEnd() + "]"; 
    }

    public float calculateTraversalCost() {
        // TODO traversal cost should vary depending on whether or not
        // the pathfinding gets closer or further away from the target
        if(!canTraverse()) return Float.MAX_VALUE;
        return Math.max(0, getSpan() + trns.getCost());
    }

    @Override
    public abstract float getSpan();
        
    /**
     * Returns an array containing both UUIDs in this connection
     * ordered by their effective "weight." Weight in this
     * case is a loose heuristic based on the attached entity
     * block's physical size. This method is useful to determine
     * which side of the connection should be pulled when 
     * catenaries get taut.
     * @param world
     * @return An array of 2 UUIDs, where the first member 
     * is considered heavier than the second.
     */
    public GridUUID[] orderedByWeight(LevelReader world) {
        GridUUID start = getStart();
        GridUUID end = getEnd();

        if(!start.canMoveDynamically(world) && end.canMoveDynamically(world)) 
            return new GridUUID[] { start, end };
        if(start.canMoveDynamically(world) && !end.canMoveDynamically(world)) 
            return new GridUUID[] { end, start };

        if(start.canMoveDynamically(world) && end.canMoveDynamically(world)) {

            if(start instanceof VoxelUUID && !(end instanceof VoxelUUID))
                return new GridUUID[] { start, end };
            if(end instanceof VoxelUUID && !(start instanceof VoxelUUID))
                return new GridUUID[] { end, start };

            float startSize = start.getAttachedSizeFactor(world);
            float endSize = end.getAttachedSizeFactor(world);

            if(startSize > endSize)
                return new GridUUID[] { start, end };
            if(endSize > startSize) 
                return new GridUUID[] { end, start };
            if(start.isAttachedToPlayer(world) && !end.isAttachedToPlayer(world)) 
                return new GridUUID[] { start, end };
            if(!start.isAttachedToPlayer(world) && end.isAttachedToPlayer(world))
                return new GridUUID[] { end, start };
            return new GridUUID[] { start, end };
        }
        return new GridUUID[] { start, end };
    }

    public static final class ConnectionKey extends GridConnection {

        private final GridUUID start, end;
        public ConnectionKey(GridUUID start, GridUUID end) {
            super(null);
            this.start = start;
            this.end = end;
        }

        @Override public GridConnection inverseCopy() { return new ConnectionKey(end, start); }
        @Override public GridUUID getStart() { return start; }
        @Override public GridUUID getEnd() { return end; }
        @Override public boolean isClientSide() { return false; }
        @Override public CompoundTag writeTo(CompoundTag in) { return in; }
        @Override public String getConnectionTypeName() { return "ConnectionKey"; }
        @Override public void adjustSpan(LevelReader world, float length) { return; }

        @Override
        public float getSpan() {
            return -1;
        }

        @Override
        public float getMaximumSpan() {
            return -1;
        }

        @Override
        public String describeDataScope(LevelReader world) {
            return "Key";
        }

        @Override
        public DataScope getDataScope(LevelReader world) {
            return DataScope.MOVING_ENTITY;
        }

        @Override
        public void setDataScope(DataScope scope) {
            return;
        }
    }

    public static enum InsertionPolicy { SINGLE, SYMMETRIC, ORDERED; }
}
