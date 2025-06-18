package com.quattage.mechano.foundation.api.landmark;

import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;

public class GridCatenary extends Connection {

    private AnchorPoint start;
    private AnchorPoint end;

    public GridCatenary(LevelReader world, AnchorPoint start, AnchorPoint end, TransmitterType<?> trns) {
        super(trns, Math.round(getEuclideanDistance(world, start.getAddress(), end.getAddress())));
        if(!world.isClientSide())
            throw new IllegalArgumentException("Can't instantiate ClientGridLink in a server-sided world!");
    }

    public boolean equals(Object other) {
        if(!(other instanceof GridCatenary that)) return false;
        return this.start.getAddress().equals(that.start.getAddress()) && this.end.getAddress().equals(that.end.getAddress()) || 
            this.start.getAddress().equals(that.end.getAddress()) && this.end.getAddress().equals(that.start.getAddress());
    }

    @Override
    public GridUUID getStart() {
        return start.getAddress();
    }

    @Override
    public GridUUID getEnd() {
        return end.getAddress();
    }

    @Override
    public boolean isClientSide() {
        return true;
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        start.writeTo(in);
        end.writeTo(in);
        trns.writeTo(in);
        in.putByte("ten", (byte)tension.ordinal());
        if(trns.needsSerialization()) {
            CompoundTag extras = new CompoundTag();
            trns.writeTo(extras);
            in.put("data", extras);
        }
        return in;
    }
}
