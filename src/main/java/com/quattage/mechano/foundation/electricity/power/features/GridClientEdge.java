package com.quattage.mechano.foundation.electricity.power.features;

import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/***
 * A lightweight alternative to GridEdge <p>
 * Can be written to and from a buffer to
 * send to the client. <p>
 * 
 * Note: At the moment, the ClientGridEdge stores the same data as the GridEdge.
 * This may seem strage, but the distinction will become more obvious when
 * more functionality is added to the GridEdge. There will be lots of data that the
 * ClientGridEdge doesn't need to track.
 */
public class GridClientEdge {

    private final GID sideA;
    private final GID sideB;
    private final int wireType;

    public GridClientEdge(GridEdge edge, int wireType) {
        this.sideA = edge.getSideA();
        this.sideB = edge.getSideB();
        this.wireType = wireType;
    }

    public GridClientEdge(GIDPair edge, int wireType) {
        this.sideA = edge.getSideA();
        this.sideB = edge.getSideB();
        this.wireType = wireType;
    }

    public GridClientEdge(FriendlyByteBuf buf) {
        this.sideA = new GID(buf.readBlockPos(), buf.readInt());
        this.sideB = new GID(buf.readBlockPos(), buf.readInt());
        this.wireType = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(sideA.getPos());
        buf.writeInt(sideA.getSubIndex());
        buf.writeBlockPos(sideB.getPos());
        buf.writeInt(sideB.getSubIndex());
        buf.writeInt(wireType);
    }

    public GID getSideA() {
        return sideA;
    }

    public GID getSideB() {
        return sideB;
    }

    public String toString() {
        BlockPos a = sideA.getPos();
        BlockPos b = sideB.getPos();
        return "GridClientEdge{[" + a.getX() + ", " + a.getY() + ", " + a.getZ() + ", " + sideA.getSubIndex() 
            + "], [" + b.getX() + ", " + b.getY() + ", " + b.getZ() + ", " + sideB.getSubIndex() + "]}";
    }

    public Pair<Vec3, Vec3> getPositions(Level world) {
        Pair<AnchorPoint, WireAnchorBlockEntity> cA = AnchorPoint.getAnchorAt(world, sideA);
        Pair<AnchorPoint, WireAnchorBlockEntity> cB = AnchorPoint.getAnchorAt(world, sideB);

        if(cA == null || cA.getFirst() == null || cB == null || cB.getFirst() == null) return null;
        return Pair.of(cA.getFirst().getPos(), cB.getFirst().getPos());
    }

    public boolean equals(Object other) {
        if(other instanceof GridClientEdge otherEdge)
            return (sideA.equals(otherEdge.sideA) && sideB.equals(otherEdge.sideB)) ||  
                (sideB.equals(otherEdge.sideA) && sideA.equals(otherEdge.sideB));
        return  false;
    }

    public boolean contains(GID id) {
        return sideA.equals(id) || sideB.equals(id);
    }

    public boolean containsPos(BlockPos pos) {
        return sideA.getPos().equals(pos) || sideB.getPos().equals(pos);
    }

    public int getTypeID() {
        return wireType;
    }
}
