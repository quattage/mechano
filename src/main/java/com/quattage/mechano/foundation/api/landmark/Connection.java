package com.quattage.mechano.foundation.api.landmark;

import com.quattage.mechano.foundation.api.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;
import com.quattage.mechano.foundation.catenary.Tensionable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public abstract class Connection implements Tensionable {
    
    public abstract GridUUID getStart();
    public abstract GridUUID getEnd();
    public abstract boolean isClientSide();

    protected float length;
    protected Tension tension;
    protected Transmitter<?> trns;


    public Connection(Transmitter<?> trns, float length) {
        this.trns = trns;
        this.length = length;
        this.tension = Tension.AVERAGE;
    }

    public Connection(TransmitterType<?> trns, float length) {
        this.trns = trns.make();
        this.length = length;
        this.tension = Tension.AVERAGE;
    }

    public Connection(Transmitter<?> trns, float length, Tension tension) {
        this.trns = trns;
        this.length = length;
        this.tension = tension;
    }

    public Connection(TransmitterType<?> trns, float length, Tension tension) {
        this.trns = trns.make();
        this.length = length;
        this.tension = tension;
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

    public void updateShape(LevelReader world) {
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

    public boolean equals(Object other) {
        if(!(other instanceof GridCatenary that)) return false;
        return getStart().equals(that.getStart()) && getEnd().equals(that.getEnd()) || 
            getStart().equals(that.getEnd()) && getEnd().equals(that.getStart());
    }

    public boolean canTraverse() {
        return getStart() != null && getEnd() != null && trns != null && trns.isEnabled();
    }

    public Transmitter<?> getTransmitter() {
        return trns;
    }

    public TransmitterType<?> getTransmitterType() {
        return trns.getType();
    }

    public boolean startsWith(GridUUID address) {
        return getStart().equals(address);
    }

    public boolean endsWith(GridUUID address) {
        return getEnd().equals(address);
    }

    public boolean involves(GridUUID address) {
        return startsWith(address) || endsWith(address);
    }

    public float calculateTraversalCost() {
        // TODO traversal cost should vary depending on whether or not
        // the pathfinding gets closer or further away from the target
        if(!canTraverse()) return Float.MAX_VALUE;
        return Math.max(0, length + trns.getCost());
    }


    @Override
    public int hashCode() {
        return getStart().hashCode() + getEnd().hashCode();
    }

    public String toString() {
        return (isClientSide() ? "ClientLink[" : "ServerLink[") + getStart() + " -> " + getEnd() + "]";
    }

    public abstract CompoundTag writeTo(CompoundTag in);
}
