package com.quattage.mechano.foundation.api.landmark;

import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;

public class GridLink extends Connection {

    private GridNode start;
    private GridNode end;
    private Tension tension;

    public GridLink(LevelReader world, GridNode start, GridNode end, Transmitter<?> trns) {
        super(trns, Math.round(getEuclideanDistance(world, start.getAddress(), end.getAddress())));
        if(start.getAddress().equals(end.getAddress()))
            throw new IllegalArgumentException("Can't instantiate a GridLink where both the start and end positions are the same!");
        this.start = start;
        this.end = end;
        this.tension = trns.getType().defaults.getTension();
    }

    private GridLink(GridNode start, GridNode end, Transmitter<?> trns, Tension tension, float length) {
        super(trns, length);
        this.start = start;
        this.end = end;
        this.tension = tension;
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
    @Override public String getConnectionTypeName() { return "GridLink"; }

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
