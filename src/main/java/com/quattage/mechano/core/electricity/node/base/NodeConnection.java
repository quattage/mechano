package com.quattage.mechano.core.electricity.node.base;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.spool.WireSpool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

/**
 * A logical representation of a connection between a pair of ElectricNodes.
 * Pertinant information regarding connections are serialized to and from NBT,
 * and include both the relative position of the connection's destination, as well
 * as the type (or "tier") of connection being made.
 */
public class NodeConnection {

	private final String destinationId;
	private final WireSpool spoolType;
	private Vec3i relativePos;

	public NodeConnection(String destinationId, WireSpool spoolType, BlockPos from, BlockPos to) {
		this.destinationId = destinationId;
		this.spoolType = spoolType;
		this.relativePos = from.subtract(to);
	}

    public NodeConnection(String destinationId, WireSpool spoolType, Vec3i relativePos) {
		this.destinationId = destinationId;
		this.spoolType = spoolType;
		this.relativePos = relativePos;
	}

    /***
     * Create a new NodeConnection from the data stored within a CompoundTag. <p>
     * This constructor operates in a vaccuum, you shouldn't ordinarily have to run it yourself  
     * since it's done for you by its parent ElectricNode. <p>
     * Make sure you do some sanity checks on your CompoundTag, since this constructor 
     * won't throw anything on its own.
     * @param target Target of this NodeConnection (should be the same as this connection's parent ElectricNode)
     * @param tag CompoundTag to pull data from
     */
	public NodeConnection(CompoundTag tag) {
        Mechano.log("TAG: " + tag);
		this.destinationId = tag.getString("target");
		this.spoolType = WireSpool.get(tag.getString("type"));
		this.relativePos = new Vec3i(
            tag.getInt("rX"), 
            tag.getInt("rY"), 
            tag.getInt("rZ")
        );
	}

    public String toString() {
        String type = "None";
        if(spoolType != null) 
            type = spoolType.getId(); 

        return "NodeConnection: {At [" 
                + relativePos.getX() + ", " 
                + relativePos.getY() + ", " 
                + relativePos.getZ() + "]"
                + ", Destination: " + destinationId
                + ", Type: " + type 
                + "}";
    }

    /***
     * Po
     * @param in CompoundTag to modify
     */
	public CompoundTag writeTo(CompoundTag in) {
		in.putInt("rX", this.relativePos.getX());
		in.putInt("rY", this.relativePos.getY());
		in.putInt("rZ", this.relativePos.getZ());
		in.putString("target", this.destinationId);
        if(spoolType == null) 
            in.putString("type", "none");
        else
            in.putString("type", this.spoolType.getId());
        return in;
	}

	public void updateRelative(NodeRotation rotation) {
		this.relativePos = rotation.updateRelative(this.relativePos);
	}

    /***
     * Gets the destination of this NodeConnection
     * @return The ID of the ElectricNode targeted by this NodeConnection
     */
	public String getDestinationId() {
		return destinationId;
	}

    /***
     * Get the WireSpool item represented by this connection.
     * @return
     */
	public WireSpool getSpool() {
		return spoolType;
	}

    /***
     * Get the relative position from this connection's root to this connection's target
     * @return
     */
	public Vec3i getRelativePos() {
		return this.relativePos;
	}
}
