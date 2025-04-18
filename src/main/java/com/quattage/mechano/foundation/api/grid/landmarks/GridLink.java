package com.quattage.mechano.foundation.api.grid.landmarks;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.SimpleTransferProtocol;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.grid.PowerGrid;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GridLink {

    private final NodeIdentifiable<GridNode> sideA;
    private @Nullable NodeIdentifiable<GridNode> sideB;
    private float length;
    private SimpleTransferProtocol protocol;

    public GridLink(NodeIdentifiable<GridNode> sideA, NodeIdentifiable<GridNode> sideB, SimpleTransferProtocol protocol) {
        this.sideA = sideA;
        this.sideB = sideB;
        this.length = Math.round(getEuclideanDistance(sideA, sideB));
    }

    private GridLink(NodeIdentifiable<GridNode> sideA, NodeIdentifiable<GridNode> sideB, SimpleTransferProtocol protocol, float length) {
        this.sideA = sideA;
        this.sideB = sideB;
        this.length = length;
        this.protocol = protocol;
    }


    public static GridLink loadFrom(LevelReader world, CompoundTag in, NodeIdentifiable<GridNode> caller, PowerGrid instantiator) {
        if(!(in.contains("x") && in.contains("y") && in.contains("z") && in.contains("i")))
            throw new IllegalArgumentException("Can't deserialize GridLink from " + caller + " - The provided tag (" + in + ") doesn't contain the required data!");
        BlockPos destinationPos = new BlockPos(in.getInt("x"), in.getInt("y"), in.getInt("z"));
        int index = in.getByte("i");
        GridNode destination = instantiator.nodes.get(destinationPos, index);
        if(destination == null) {
            BlockEntity be = world.getBlockEntity(destinationPos);
            if(be == null) throw new NullPointerException("Error instaitiating transitive GridLink destination node - No BlockEntity could be found at " + destinationPos);
            if(!(be instanceof PowerGridBlockEntity pgbe))
                throw new IllegalArgumentException("Error instaitiating transitive GridLink destination node - BlockEntity at " + destinationPos + " is not an instance of PowerGridBlockEntity!");
            destination = new GridNode(instantiator, pgbe, destinationPos, index);
            instantiator.nodes.add(destination);
        }
        return new GridLink(caller, destination, SimpleTransferProtocol.loadFrom(in));
    }

    public static float getEuclideanDistance(NodeIdentifiable<?> a, NodeIdentifiable<?> b) {
        return (float)Math.sqrt(
            Math.pow(a.getX() - b.getX(), 2) +
            Math.pow(a.getY() - b.getY(), 2) +
            Math.pow(a.getZ() - b.getZ(), 2)
        );
    }

    public GridLink inverseCopy() {
        return new GridLink(sideB, sideA, this.protocol, length);
    }

    public boolean startsWith(NodeIdentifiable<?> address) {
        return sideA.equals(address);
    }

    public boolean endsWith(NodeIdentifiable<?> address) {
        return address.equals(sideB);
    }

    public boolean involves(NodeIdentifiable<?> address) {
        return startsWith(address) || endsWith(address);
    }

    public GridNode getStart() {
        if(sideA == null) return null;
        return sideA.getValue();
    }

    public GridNode getEnd() {
        if(sideB == null) return null;
        return sideB.getValue();
    }

    public boolean canTraverse() {
        return sideB != null;
    }

    public float calculateTraversalCost() {
        // TODO traversal cost should vary depending on whether or not
        // the pathfinding gets closer or further away from the target
        if(sideB == null) return Float.MAX_VALUE;
        return Math.max(0, length + protocol.getCost());
    }

    public boolean equals(Object other) {
        if(!(other instanceof GridLink that)) return false;
        return this.sideA.equals(that.sideA) && this.sideB.equals(that.sideB);
    }

    public int hashCode() {
        return sideA.hashCode() * 31 + sideB.hashCode();
    }

    public String toString() {
        return sideA + " -> " + sideB;
    }

    public SimpleTransferProtocol getConnection() {
        return protocol;
    }

    public CompoundTag writeTo(CompoundTag in) {
        sideB.writeOnlyAddress(in);
        in.putByte("id", (byte)(protocol.getID() - 128));
        protocol.writeTo(in);
        return in;
    }
}
