package com.quattage.mechano.core.electricity.node.connection;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * A logical representation of a connection between a pair of ElectricNodes.
 * Pertinant information regarding connections are serialized to and from NBT,
 * and include both the relative position of the connection's destination, as well
 * as the type (or "tier") of connection being made.
 */
public class ElectricNodeConnection extends NodeConnection {

    private Vec3i relativePos;
    private String destinationID;

    public ElectricNodeConnection(WireSpool spoolType, NodeBank fromBank, Vec3 sourcePos, NodeBank toBank, String destinationID, boolean inverse) {
        this.sourcePos = sourcePos;
        destPos = toBank.get(destinationID).getPosition();
        relativePos = fromBank.pos.subtract(toBank.pos);
        this.destinationID= destinationID;
        this.spoolType = spoolType;
    }

    public ElectricNodeConnection(WireSpool spoolType, NodeBank fromBank, Vec3 sourcePos, NodeBank toBank, String destinationID) {
        this.sourcePos = sourcePos;
        destPos = toBank.get(destinationID).getPosition();
        relativePos = fromBank.pos.subtract(toBank.pos);
        this.destinationID= destinationID;
        this.spoolType = spoolType;
    }

    /***
     * Builds a new ElectricNodeConnection from the given CompoundTag
     * @param in CompoundTag to pull values from
     */
    public ElectricNodeConnection(BlockEntity target, Vec3 sourcePos, CompoundTag in) {
        this.destinationID = in.getString("to");
        this.spoolType = WireSpool.get(in.getString("type"));
        this.sourcePos = sourcePos;
        this.relativePos = new Vec3i(
            in.getInt("rX"), 
            in.getInt("rY"), 
            in.getInt("rZ")
        );

        Level world = target.getLevel();
        if(world != null) {  // world can be null during world load.
            NodeBank destBank = NodeBank.retrieveFrom(world, target, relativePos);
            if(destBank != null) destPos = destBank.get(destinationID).getPosition();
            else destPos = null;
            this.age = in.getInt("age");
        }
        else {
            destPos = null; 
            age = -1;
        }
    }
    
    /***
     * Populates the given CompoundTag with data from this ElectircNodeConnection.
     * @param in CompoundTag to modify
     * @return The modified CompoundTag.
     */
	public CompoundTag writeTo(CompoundTag in) {
		in.putInt("rX", this.relativePos.getX());
		in.putInt("rY", this.relativePos.getY());
		in.putInt("rZ", this.relativePos.getZ());
        in.putInt("age", this.age);
		in.putString("to", this.destinationID);
        if(spoolType == null) 
            in.putString("type", "none");
        else
            in.putString("type", this.spoolType.getId());
        return in;
	}

    /***
     * Destination positions will be null during world load, so initializing
     * them on the first BlockEntity tick is required when initially populating
     * from NBT.
     * @param vec
     */
    public void initDestPos(Vec3 vec) {
        destPos = vec;
    }

    @Override
    protected boolean setNeedsLerped() {
        return false;
    }

    @Override
    public void updatePosition(float pTicks) {
        super.updatePosition(pTicks);
    }

    @Override
    protected boolean setTransferPower() {
        return true;
    }

    /***
     * Gets the target bank of this ElectricNodeConnection
     * @param from
     * @return NodeBank at the given target, or null if one cannot be found.
     */
    public NodeBank getTargetBank(NodeBank bank) {
        Level world = bank.getWorld();
        if(world == null) return null;
        BlockEntity be = world.getBlockEntity(bank.pos.subtract(relativePos));
        if(be instanceof ElectricBlockEntity ebe)
            return ebe.nodes;
        return null;
    }

    public String getDestinationID() {
        return destinationID;
    }

    public Vec3i getRelativePos() {
        return relativePos;
    }

    public boolean needsUpdate() {
        return destPos == null;
    }
}
