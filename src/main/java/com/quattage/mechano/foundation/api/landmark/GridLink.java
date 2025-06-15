package com.quattage.mechano.foundation.api.landmark;

import java.util.Objects;

import com.quattage.mechano.foundation.api.landmark.uuid.GridUUID;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public class GridLink {

    private final GridNode start;
    private GridNode end;
    private float length;
    public final Transmitter<?> transmitter;

    public GridLink(GridNode start, GridNode end, Transmitter<?> transmitter) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(transmitter);
        this.start = start;
        this.end = end;
        this.length = Math.round(getEuclideanDistance(start.getAddress(), end.getAddress()));
        this.transmitter = transmitter;
    }

    private GridLink(GridNode start, GridNode end, Transmitter<?> transmitter, float length) {
        this.start = start;
        this.end = end;
        this.length = length;
        this.transmitter = transmitter;
    }

    public static float getEuclideanDistance(GridUUID a, GridUUID b) {
        Vec3 aPos = a.getPos();
        Vec3 bPos = b.getPos();
        return (float)Math.sqrt(
            Math.pow(aPos.x - bPos.x, 2) +
            Math.pow(aPos.y - bPos.y, 2) +
            Math.pow(aPos.z - bPos.z, 2)
        );
    }

    public CompoundTag writeTo(CompoundTag in) {
        end.getAddress().writeTo(in);
        transmitter.getType().writeTo(in);
        if(transmitter.needsSerialization()) {
            CompoundTag extras = new CompoundTag();
            transmitter.writeTo(extras);
            in.put("data", extras);
        }
        return in;
    }

    public float calculateTraversalCost() {
        // TODO traversal cost should vary depending on whether or not
        // the pathfinding gets closer or further away from the target
        if(end == null) return Float.MAX_VALUE;
        return Math.max(0, length + transmitter.getCost());
    }

    public GridLink copyAndFlip() {
        return new GridLink(end, start, transmitter, length);
    }

    public boolean startsWith(GridUUID address) {
        return start.getAddress().equals(address);
    }

    public boolean endsWith(GridUUID address) {
        return end.getAddress().equals(address);
    }

    public boolean involves(GridUUID address) {
        return startsWith(address) || endsWith(address);
    }

    public GridNode getStart() {
        return start;
    }

    public GridNode getEnd() {
        return end;
    }

    public boolean canTraverse() {
        return end != null && transmitter.isEnabled();
    }

    public boolean equals(Object other) {
        if(!(other instanceof GridLink that)) return false;
        return this.start.equals(that.start) && this.end.equals(that.end);
    }

    public int hashCode() {
        return start.hashCode() * 31 + end.hashCode();
    }

    public String toString() {
        return start + " -> " + end;
    }

    public Transmitter<?> getConnection() {
        return transmitter;
    }
}
