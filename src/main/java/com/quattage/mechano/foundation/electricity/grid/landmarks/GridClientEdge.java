package com.quattage.mechano.foundation.electricity.grid.landmarks;

import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;

import net.createmod.catnip.data.Pair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/***
 * A lightweight alternative to GridEdge <p>
 * Can be written to and from a buffer to
 * send to the client. <p>
 */
public class GridClientEdge {

    private final GID sideA;
    private final GID sideB;
    private final int typeID;

    private final int initialAge;
    
    private float age = (float)MechanoSettings.WIRE_ANIM_LENGTH;

    public GridClientEdge(GridEdge edge, int typeID) {
        this.sideA = edge.getOriginVertex().getID();
        this.sideB = edge.getDestinationVertex().getID();
        this.typeID = typeID;
        this.age = age * (int)((float)Mth.clamp(cheb(), 3, 90) * 0.4f);
        this.initialAge = 0;
    }

    public GridClientEdge(GID sideA, GID sideB, int typeID) {
        this.sideA = sideA;
        this.sideB = sideB;
        this.typeID = typeID;
        this.age = age * (int)((float)Mth.clamp(cheb(), 3, 90) * 0.4f);
        this.initialAge = 0;
    }

    public GridClientEdge(FriendlyByteBuf buf) {
        this.sideA = new GID(buf.readBlockPos(), buf.readInt());
        this.sideB = new GID(buf.readBlockPos(), buf.readInt());
        this.typeID = buf.readInt();
        this.age = age * (int)((float)Mth.clamp(cheb(), 3, 90) * 0.4f);
        this.initialAge = (int)age;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(sideA.getBlockPos());
        buf.writeInt(sideA.getSubIndex());
        buf.writeBlockPos(sideB.getBlockPos());
        buf.writeInt(sideB.getSubIndex());
        buf.writeInt(typeID);
    }

    public int cheb() {
        BlockPos a = sideA.getBlockPos();
        BlockPos b = sideB.getBlockPos();
        return 
            Math.max(Math.max(
                Math.abs(a.getX() - b.getX()), 
                Math.abs(a.getY() - b.getY())), 
                Math.abs(a.getZ() - b.getZ()
            ));
    } 

    public GID getSideA() {
        return sideA;
    }

    public GID getSideB() {
        return sideB;
    }

    public String toString() {
        BlockPos a = sideA.getBlockPos();
        BlockPos b = sideB.getBlockPos();
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

    public int hashCode() {
        return (sideA.hashCode() + sideB.hashCode()) * 31;
    }

    public boolean contains(GID id) {
        return sideA.equals(id) || sideB.equals(id);
    }

    public boolean existsIn(ClientLevel world) {
        return getPositions(world) != null;
    }

    public float getAge() {
        return this.age;
    }

    public void tickAge(long delta) {
        this.age -=  1l * (delta * 0.0000008d);
        if(age < 0) age = 0;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getInitialAge() {
        return initialAge;
    }

    public GIDPair toHashable() {
        return new GIDPair(sideA, sideB);
    }

    public boolean containsPos(BlockPos pos) {
        if(sideA.getBlockPos().equals(pos)) return true;
        if(sideB.getBlockPos().equals(pos)) return true;
        return false;
    }

    public boolean goesNowhere() {
        return sideA.equals(sideB);
    }

    public int getTypeID() {
        return typeID;
    }
}