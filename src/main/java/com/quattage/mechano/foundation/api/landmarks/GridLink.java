package com.quattage.mechano.foundation.api.landmarks;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.transmission.Transmitter;

import net.minecraft.nbt.CompoundTag;

public class GridLink {

    private final GridNode start;
    private @Nullable GridNode end;
    private float length;
    public final Transmitter<?> transmitter;

    public GridLink(GridNode start, GridNode end, Transmitter<?> transmitter) {
        this.start = start;
        this.end = end;
        this.length = Math.round(getEuclideanDistance(start, end));
        this.transmitter = transmitter;
    }

    private GridLink(GridNode start, GridNode end, Transmitter<?> transmitter, float length) {
        this.start = start;
        this.end = end;
        this.length = length;
        this.transmitter = transmitter;
    }

    

    public static float getEuclideanDistance(NodeIdentifiable a, NodeIdentifiable b) {
        return (float)Math.sqrt(
            Math.pow(a.getX() - b.getX(), 2) +
            Math.pow(a.getY() - b.getY(), 2) +
            Math.pow(a.getZ() - b.getZ(), 2)
        );
    }

    public GridLink copyAndFlip() {
        return new GridLink(end, start, transmitter, length);
    }

    public boolean startsWith(NodeIdentifiable address) {
        return start.equals(address);
    }

    public boolean endsWith(NodeIdentifiable address) {
        return address.equals(end);
    }

    public boolean involves(NodeIdentifiable address) {
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

    public float calculateTraversalCost() {
        // TODO traversal cost should vary depending on whether or not
        // the pathfinding gets closer or further away from the target
        if(end == null) return Float.MAX_VALUE;
        return Math.max(0, length + transmitter.getCost());
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

    public CompoundTag writeTo(CompoundTag in) {
        end.writeOnlyAddress(in);
        transmitter.getType().writeTo(in);
        if(transmitter.needsSerialization()) {
            CompoundTag extras = new CompoundTag();
            transmitter.writeTo(extras);
            in.put("data", extras);
        }
        return in;
    }
}
