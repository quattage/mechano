package com.quattage.mechano.foundation.api.landmarks;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.transmission.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmission.Transmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

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

    // REWRITE THIS LLMAOOO
    public static GridLink loadFrom(LevelReader world, CompoundTag in, GridNode caller) {
        if(!(in.contains("x") && in.contains("y") && in.contains("z") && in.contains("i")))
            throw new IllegalArgumentException("Can't deserialize GridLink from " + caller 
                + " - The provided tag (" + in + ") doesn't contain the required data!");
        BlockPos destinationPos = new BlockPos(in.getInt("x"), in.getInt("y"), in.getInt("z"));
        int index = in.getByte("i");
        GridNode destination = caller.owner.nodes.get(destinationPos, index);
        if(destination == null) {
            BlockEntity be = world.getBlockEntity(destinationPos);
            if(be == null) throw new NullPointerException("Error instaitiating transitive GridLink destination node - No BlockEntity could be found at " + destinationPos);
            if(!(be instanceof PowerGridBlockEntity pgbe))
                throw new IllegalArgumentException("Error instaitiating transitive GridLink destination node - BlockEntity at " + destinationPos + " is not an instance of PowerGridBlockEntity!");
            destination = new GridNode(caller.owner, pgbe, index);
            caller.owner.nodes.add(destination);
        }
        return new GridLink(caller, destination, MechanoTransmissionTypes.REGISTRY.get(in));
    }

    public static float getEuclideanDistance(NodeIdentifiable<?> a, NodeIdentifiable<?> b) {
        return (float)Math.sqrt(
            Math.pow(a.getX() - b.getX(), 2) +
            Math.pow(a.getY() - b.getY(), 2) +
            Math.pow(a.getZ() - b.getZ(), 2)
        );
    }

    public GridLink copyAndFlip() {
        return new GridLink(end, start, this.transmitter, length);
    }

    public boolean startsWith(NodeIdentifiable<?> address) {
        return start.equals(address);
    }

    public boolean endsWith(NodeIdentifiable<?> address) {
        return address.equals(end);
    }

    public boolean involves(NodeIdentifiable<?> address) {
        return startsWith(address) || endsWith(address);
    }

    public GridNode getStart() {
        if(start == null) return null;
        return start.getValue();
    }

    public GridNode getEnd() {
        if(end == null) return null;
        return end.getValue();
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
