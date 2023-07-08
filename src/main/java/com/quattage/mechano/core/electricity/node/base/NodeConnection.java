package com.quattage.mechano.core.electricity.node.base;

import com.quattage.mechano.content.item.spool.WireSpool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

/**
 * A class representing a node connected to a {@link IWireNode}.
 */
public class NodeConnection {

	private final int fromIndex;
	private final int toIndex;
	private final WireSpool spoolType;
	private Vec3i relativePos;

	public NodeConnection(int fromIndex, int toIndex, WireSpool spoolType, BlockPos from, BlockPos to) {
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
		this.spoolType = spoolType;
		this.relativePos = from.subtract(to);
	}

    public NodeConnection(int fromIndex, int toIndex, WireSpool spoolType, Vec3i relativePos) {
		this.fromIndex = fromIndex;
		this.toIndex = toIndex;
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
		this.fromIndex = tag.getInt("From");
		this.toIndex = tag.getInt("To");
		this.spoolType = WireSpool.get(tag.getString("SpoolType"));
		this.relativePos = new Vec3i(
            tag.getInt("Rx"), 
            tag.getInt("Ry"), 
            tag.getInt("Rz")
        );
	}

    public String toString() {
        return "NodeConnection: {At [" 
                + relativePos.getX() + ", " 
                + relativePos.getY() + ", " 
                + relativePos.getZ() + "]"
                + ", From Index: " + fromIndex +
                ", To Index: " + toIndex + "}";
    }

    /***
     * Returns the inverse NodeConnection, where the toIndex and fromIndex are inverted.
     * @return
     */
    public NodeConnection getInverse() {
        return new NodeConnection(this.toIndex, this.fromIndex, this.spoolType, this.relativePos);
    }

    /***
     * Po
     * @param in CompoundTag to modify
     */
	public CompoundTag writeTo(CompoundTag in) {
		in.putInt("Rx", this.relativePos.getX());
		in.putInt("Ry", this.relativePos.getY());
		in.putInt("Rz", this.relativePos.getZ());
		in.putInt("From", this.fromIndex);
		in.putInt("To", this.toIndex);
        if(spoolType == null) 
            in.putString("SpoolType", "none");
        else
            in.putString("SpoolType", this.spoolType.getId());
        return in;
	}

	public void updateRelative(NodeRotation rotation) {
		this.relativePos = rotation.updateRelative(this.relativePos);
	}

    /***
     * Gets the index of this NodeConnection's "from" connection
     * @return Index of the "from" ElectricNode
     */
	public int getFromIndex() {
		return fromIndex;
	}

    /***
     * Gets the index of this NodeConnection's "to" connection
     * @return Index of the "to" ElectricNode
     */
	public int getToIndex() {
		return toIndex;
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
