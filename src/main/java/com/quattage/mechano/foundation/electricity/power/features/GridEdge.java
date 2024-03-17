package com.quattage.mechano.foundation.electricity.power.features;

import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.power.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.power.GridSyncDirector;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.quattage.mechano.foundation.network.GridSyncPacketType;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/***
 * A GridEdge is a logical representation of a connection between two verticies in a LocalTransferGrid
 * Where GridVertices represent connectors themselves, edges can be thought of as the wires between connectors.
 */
public class GridEdge {

    private final WireSpool wireType;
    private final GIDPair target;
    private final float distance;
    private boolean canTransfer = true;

    public GridEdge(GlobalTransferGrid parent, GIDPair edgeID, int wireType) {
        if(parent == null) throw new NullPointerException("Error instantiating GridEdge - Parent network is null!");
        if(edgeID == null) throw new NullPointerException("Error instantiating GridEdge - Target is null!");
        this.target = edgeID;
        this.wireType = WireSpool.ofType(wireType);

        BlockPos a = target.getSideA().getPos();
        BlockPos b = target.getSideB().getPos();
        this.distance = (float)Math.sqrt(Math.pow(a.getX() - b.getX(), 2f) + Math.pow(a.getY() - b.getY(), 2f) + Math.pow(a.getZ() - b.getZ(), 2f));

        GridSyncDirector.informPlayerEdgeUpdate(GridSyncPacketType.ADD, this.toLightweight());
    }

    public GridEdge(GlobalTransferGrid parent, CompoundTag tag) {
        if(parent == null) throw new NullPointerException("Error instantiating GridEdge - Parent network is null!");
        this.wireType = WireSpool.ofType(tag.getInt("t"));
        this.target = GIDPair.of(tag.getCompound("e"));

        BlockPos a = target.getSideA().getPos();
        BlockPos b = target.getSideB().getPos();
        this.distance = (float)Math.sqrt(Math.pow(a.getX() - b.getX(), 2f) + Math.pow(a.getY() - b.getY(), 2f) + Math.pow(a.getZ() - b.getZ(), 2f));
    }

    public CompoundTag writeTo(CompoundTag nbt) {
        nbt.putInt("t", wireType.getSpoolID());
        nbt.put("e", target.writeTo(new CompoundTag()));
        return nbt;
    }

    public float getDistance() {
        return this.distance;
    }

    public float calcScore() {
        return 1.0f / (float)this.wireType.getRate();
    }

    public WireSpool getWireType() {
        return wireType;
    }

    /***
     * Whether or not this GridEdge should have a wire rendered for it
     * @return
     */
    public boolean rendersWire() {
        return true;
    }

    /***
     * Whether or not this GridEdge is "real."
     * "Real" wires are tied to a logical representation of some sort in the world - 
     * they can break if stretched too far, they'll drop wire when broken, they can be
     * placed by the player, etc. 
     * 
     * Non-real wires are usually added by the network itself, and are intangible. A good example
     * of a non-real wire would be a wireless or cross-dimensional form of energy transport.
     * @return
    */
    public boolean isReal() { return true; }

    /***
     * Whether or not this GridEdge can transfer any FE/t
     * @return True if this Edge's transfer rate is > 0
     */
    public boolean canTransfer() {
        return getTransferRate() > 0 && this.canTransfer;
    }

    /***
     * Gets the integer transfer rate in FE per Tick of the is edge
     * @return Positive integer from 0 to int MAX_VALUE
     */
    public int getTransferRate() {
        return wireType.getRate();
    }

    /***
     * Whether or not this GridEdge interacts with the SystemNode at the given SVID
     * @param target SVID to check
     * @return True if one of this GridEdge's ends are at this SVID
     */
    public boolean connectsTo(GID target) {
        return target == getSideA() || target == getSideB(); 
    }

    public GID getSideA() {
        return target.getSideA();
    }

    public GID getSideB() {
        return target.getSideB();
    }

    /***
     * @return BlockPos position of this edge's A side
     */
    public BlockPos getPosA() {
        return getSideA().getPos();
    }

    /***
     * @return BlockPos position of this edge's B side
     */
    public BlockPos getPosB() {
        return getSideB().getPos();
    }

    public GIDPair getID() {
        return target;
    }

    public GridClientEdge toLightweight() {
        return new GridClientEdge(this.target, this.wireType.getSpoolID());
    }

    public Pair<Vec3, Vec3> getPositions(Level world) {
        Pair<AnchorPoint, WireAnchorBlockEntity> cA = AnchorPoint.getAnchorAt(world, getSideA());
        Pair<AnchorPoint, WireAnchorBlockEntity> cB = AnchorPoint.getAnchorAt(world, getSideB());

        if(cA == null || cA.getFirst() == null || cB == null || cB.getFirst() == null) return null;
        return Pair.of(cA.getFirst().getPos(), cB.getFirst().getPos());
    }
}
