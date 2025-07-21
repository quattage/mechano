package com.quattage.mechano.foundation.api.landmark;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.api.switchboard.LinkResponsePacket;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;

public final class GridLink extends GridConnection {

    private GridNode start;
    private GridNode end;
    private Tension tension;
    private float length;

    public GridLink(LevelReader world, GridNode start, GridNode end, Transmitter<?> trns) {
        super(trns); 
        Objects.requireNonNull(world);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);

        start.assertNotDestroyed("Failed to instantiate GridLink with invalid starting GridNode at" + start.getAddress().toString(world) + " - This node is invalid or has been previously destroyed!");
        end.assertNotDestroyed("Failed to instantiate GridLink with invalid ending GridNode at" + end.getAddress().toString(world) + " - This node is invalid or has been previously destroyed!");
        if(start.getAddress().equals(end.getAddress()))
            throw new IllegalArgumentException("Can't instantiate a GridLink where both the start and end positions are the same!");
        if(!start.getOwner().equals(end.getOwner()))
            throw new IllegalStateException("Attempted to add two nodes that don't belong to the same grid, got start: " + start.getOwner() + ", and end: " + end.getOwner());
            
        this.length = Math.round(getEuclideanDistance(world, start.getAddress(), end.getAddress()));

        this.start = start;
        this.end = end;
        this.tension = trns.getType().defaults.getTension();
    }

    private GridLink(GridNode start, GridNode end, Transmitter<?> trns, Tension tension, float length) {
        super(trns);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        this.length = length;
        this.start = start;
        this.end = end;
        this.tension = tension;
    }

    @Override
    public void broadast() { broadcast(GridResponse.TASK_CREATE_LINK); }

    @Override
    public void broadcast(GridResponse response) {
        switch(response) {
            case TASK_CREATE_LINK -> {
                start.broadcast(response);
                end.broadcast(response);
                start.getGriddable().onConnectionCreated(start.getOwner().getWorld(), this);
                end.getGriddable().onConnectionCreated(end.getOwner().getWorld(), this);
                trns.onConnectionCreated(end.getOwner().getWorld(), this);
                sendToClientsTracking(LinkResponsePacket.of(start, end, trns, response));
            }
            case TASK_DESTROY_LINK -> {
                start.broadcast(response);
                end.broadcast(response);
                start.getGriddable().onConnectionDestroyed(start.getOwner().getWorld(), this);
                end.getGriddable().onConnectionDestroyed(end.getOwner().getWorld(), this);
                trns.onConnectionDestroyed(end.getOwner().getWorld(), null, this);
                sendToClientsTracking(LinkResponsePacket.of(start, end, trns, response));
            }
            case null, default -> Mechano.LOGGER.error("Respose type '" + response + "' is unsupported for braodcasting");
        }
    }

    @Override
    public GridLink inverseCopy() {
        return new GridLink(end, start, this.getTransmitter(), getTension(), this.length);
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        UUIDDiscriminator.write(end.getAddress(), in);
        end.getAddress().writeTo(in);
        trns.getType().writeTo(in);
        in.putByte("ten", (byte)tension.ordinal());
        if(trns.needsSerialization()) {
            CompoundTag extras = new CompoundTag();
            trns.writeTo(extras);
            in.put("data", extras);
        }
        return in;
    }

    public GridNode getStartNode() { return start; }
    public GridNode getEndNode() { return end; }
    @Override public GridUUID getStart() { return start.getAddress(); }
    @Override public GridUUID getEnd() { return end.getAddress(); }
    @Override public boolean isClientSide() { return false; }
    @Override public String getConnectionTypeName() { 
        return "GridLink(" + trns.getType() + ")";
    }

    @Override
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        throw new UnsupportedOperationException("Can't evalute frustum culling status of a server-sided GridLink! This method is only designed to work on clients!");
    }

    @Override
    public Tension getTension() {
        return tension;
    }

    @Override
    public boolean setTension(Tension tension) {
        if(tension == null) return setTension();
        if(this.tension.equals(tension)) return false;
        this.tension = tension;
        return true;
    }

    @Override
    public boolean resetTension() {
        Tension defaultTension = trns.getType().defaults.getTension();
        if(tension == defaultTension) return false;
        this.tension = defaultTension;
        return true;
    }

    @Override
    public float getLength() {
        return length;
    }

    @Override
    public float getMaxLength() {
        return trns.getType().getMaxLength();
    }
}
