package com.quattage.mechano.foundation.api.landmark;

import java.util.Objects;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.SidedGridDispatcher.LinkData;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;
import com.quattage.mechano.foundation.catenary.Tensionable;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public abstract class Connection implements Tensionable {
    
    public abstract GridUUID getStart();
    public abstract GridUUID getEnd();
    public abstract boolean isClientSide();

    protected float length;
    protected Transmitter<?> trns;

    public static void sendToClientsTracking(Connection link, CustomPacketPayload packet) {
        if(link.isClientSide()) throw new IllegalArgumentException("Attempted to send a client-bound packet");
        sendToClientsTracking(link.getStart(), link.getEnd(), packet);
    }

    public static void sendToClientsTracking(GridUUID start, @Nullable GridUUID end, CustomPacketPayload packet) {
        MinecraftServer server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        for(ServerPlayer player : server.getPlayerList().getPlayers()) {
            if(start.isBeingTrackedBy(player))
                CatnipServices.NETWORK.sendToClient(player, packet);
            else if(end != null && end.isBeingTrackedBy(player))
                CatnipServices.NETWORK.sendToClient(player, packet);
        }
    }

    public Connection(Transmitter<?> trns, float length) {
        this.trns = trns;
        this.length = length;
    }

    public void updateShape(LevelReader world, float pTicks) {
        this.length = getEuclideanDistance(world, getStart(), getEnd());
    }

    public static float getEuclideanDistance(LevelReader world, GridUUID a, GridUUID b) {
        Vec3 aPos = a.getPos(world);
        Vec3 bPos = b.getPos(world);
        return (float)Math.sqrt(
            Math.pow(aPos.x - bPos.x, 2) +
            Math.pow(aPos.y - bPos.y, 2) +
            Math.pow(aPos.z - bPos.z, 2)
        );
    }

    public void pushTo(LevelReader world) { pushTo(world, InsertionMode.SYMMETRIC); }
    public void pushTo(LevelReader world, InsertionMode mode) {
        assertWorldly(world);
        switch(mode) {
            case SINGLE -> LinkData.add(world, this);
            case SYMMETRIC -> {
                LinkData.add(world, this);
                LinkData.add(world, this.inverseCopy());
            }
            case null, default -> { throw new UnsupportedOperationException("Insertion mode " + mode + " has not been implemented!"); }
        }
    }

    public boolean existsIn(LevelReader world) {

        IAttachmentHolder startHolder = getStart().getDataHolder(world);
        LinkData startData = startHolder.getData(MechanoData.LINK_ATTACHMENT);
        if(startData.contains(this)) return true;

        IAttachmentHolder endHolder = getEnd().getDataHolder(world);
        LinkData endData = endHolder.getData(MechanoData.LINK_ATTACHMENT);
        
        if(endData.contains(this)) {
            if(startData.isEmpty())
                startHolder.removeData(MechanoData.LINK_ATTACHMENT);
            return true;
        }

        if(startData.isEmpty())
            startHolder.removeData(MechanoData.LINK_ATTACHMENT);
        if(endData.isEmpty())
            endHolder.removeData(MechanoData.LINK_ATTACHMENT);

        return false;
    }

    public @Nullable Connection findIn(LevelReader world) {
        IAttachmentHolder startHolder = getStart().getDataHolder(world);
        LinkData startData = startHolder.getData(MechanoData.LINK_ATTACHMENT);
        Connection found = startData.get(this);
        if(found != null) {
            if(found.hasPoints()) return found;   
            found.removeFrom(world);
        }

        IAttachmentHolder endHolder = getEnd().getDataHolder(world);
        LinkData endData = endHolder.getData(MechanoData.LINK_ATTACHMENT);
        found = endData.get(this);
        
        if(found != null) {
            if(startData.isEmpty())
                startHolder.removeData(MechanoData.LINK_ATTACHMENT);
            if(found.hasPoints()) return found;
            found.removeFrom(world);
        }

        if(startData.isEmpty())
            startHolder.removeData(MechanoData.LINK_ATTACHMENT);
        if(endData.isEmpty())
            endHolder.removeData(MechanoData.LINK_ATTACHMENT);
        return null;
    }

    public void removeFrom(LevelReader world) {
        assertWorldly(world);
        LinkData.remove(world, getStart(), this);
        LinkData.remove(world, getEnd(), this);
    }

    /**
     * Useful for any action that may change the start/endpoints of this Connection.
     * The {@link SidedGridDispatcher.LinkData link data store} will need to be informed
     * of any changes that may affect the value of this Connection's {@link #hashCode hash code}
     * so that this Connection may re-assert themselves something something hashset buckets blah haha
     * @param world
     * @param action
     */
    public void reassertAndDo(LevelReader world, Runnable action) {
        assertWorldly(world);
        LinkData.remove(world, getStart(), this);
        LinkData.remove(world, getEnd(), this);
        try { action.run(); } 
        catch(Exception e) {
            Mechano.LOGGER.error("Failed executing reassertion task for " + this + ": ");
            e.printStackTrace();
            return;
        }
        pushTo(world);
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
     * @return <code>true</code> if this Connection's start and end points are not <code>null.</code>
     */
    public boolean hasPoints() {
        return getStart() != null && getEnd() != null;
    }

    public abstract Connection inverseCopy();
    public abstract CompoundTag writeTo(CompoundTag in);
    public abstract String getConnectionTypeName();
    public boolean canTraverse() { return getStart() != null && getEnd() != null && trns != null && trns.isEnabled(); }
    public Transmitter<?> getTransmitter() { return trns; }
    public boolean startsWith(GridUUID address) { return getStart().equals(address); }
    public boolean endsWith(GridUUID address) { return getEnd().equals(address); }
    public boolean involves(GridUUID address) { return startsWith(address) || endsWith(address); }

    @Override
    public boolean equals(Object other) {
        if(other == this) return true;
        if(!(other instanceof Connection that)) return false;
        return (getStart().equals(that.getStart()) && getEnd().equals(that.getEnd())) || (getStart().equals(that.getEnd()) && getEnd().equals(that.getStart()));
    }

    @Override
    public int hashCode() { 
        if(getStart().equals(getEnd())) return getStart().hashCode();
        return getStart().hashCode() ^ getEnd().hashCode(); 
    }

    @Override
    public String toString() { 
        return getConnectionTypeName() + "[" + getStart() + " -> " + getEnd() + "]"; 
    }

    public float calculateTraversalCost() {
        // TODO traversal cost should vary depending on whether or not
        // the pathfinding gets closer or further away from the target
        if(!canTraverse()) return Float.MAX_VALUE;
        return Math.max(0, length + trns.getCost());
    }
        
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

        if(!start.canMoveDynamically() && end.canMoveDynamically()) 
            return new GridUUID[] { start, end };
        if(start.canMoveDynamically() && !end.canMoveDynamically()) 
            return new GridUUID[] { end, start };

        if(start.canMoveDynamically() && end.canMoveDynamically()) {
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


    public static final class ConnectionKey extends Connection {

        private final GridUUID start, end;
        public ConnectionKey(GridUUID start, GridUUID end) {
            super(null, 0);
            this.start = start;
            this.end = end;
        }

        @Override public Connection inverseCopy() { return new ConnectionKey(end, start); }
        @Override public GridUUID getStart() { return start; }
        @Override public GridUUID getEnd() { return end; }
        @Override public boolean isClientSide() { return false; }
        @Override public CompoundTag writeTo(CompoundTag in) { return in; }
        @Override public String getConnectionTypeName() { return "ConnectionKey"; }
        @Override public Tension getTension() { return Tension.AVERAGE; }
        @Override public boolean setTension(Tension tension) { return false; }

        @Override
        public float getLength() {
            return -1;
        }

        @Override
        public float getMaxLength() {
            return -1;
        }

        @Override
        public void pushTo(LevelReader world, InsertionMode mode) {
            throw new UnsupportedOperationException("ConnectionKeys have no implementation and cannot be pushed to data attachments!");
        }

        @Override
        public void removeFrom(LevelReader world) {
            LinkData.remove(world, getStart(), this);
            LinkData.remove(world, getEnd(), this);
        }
    }

    public static enum InsertionMode { SINGLE, SYMMETRIC; }
}
