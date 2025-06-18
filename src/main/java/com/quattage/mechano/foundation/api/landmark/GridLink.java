package com.quattage.mechano.foundation.api.landmark;

import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;

public class GridLink extends Connection {

    private final GridNode start;
    private GridNode end;

    public GridLink(LevelReader world, GridNode start, GridNode end, Transmitter<?> trns) {
        super(trns, Math.round(getEuclideanDistance(world, start.getAddress(), end.getAddress())));
        this.start = start;
        this.end = end;
    }

    private GridLink(GridNode start, GridNode end, Transmitter<?> trns, Tension tension, float length) {
        super(trns, length, tension);
        this.start = start;
        this.end = end;
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
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

    @Override
    public GridUUID getStart() {
        return end.getAddress();
    }

    @Override
    public GridUUID getEnd() {
        return start.getAddress();
    }

    public GridNode getStartNode() {
        return start;
    }

    public GridNode getEndNode() {
        return end;
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    public GridLink copyAndFlip() {
        return new GridLink(start, end, this.getTransmitter(), getTension(), this.length);
    }
}
