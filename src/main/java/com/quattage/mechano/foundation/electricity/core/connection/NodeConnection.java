package com.quattage.mechano.foundation.electricity.core.connection;

import com.quattage.mechano.content.item.spool.WireSpool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/***
 * The Abstract NodeConnection class contains implementation common to all types of connections
 */
public abstract class NodeConnection {

    /***
     * Whether or not this NodeConnection needs to be interpolated between frames.
     * If the connection is static, (e.g. between two blocks) it won't need to be lerped.
     */
    protected final boolean needsLerped = setNeedsLerped();
    /***
     * Whether or not this NodeConnection represents a connection between two blocks with
     * energy capabilites. If it doesn't, this wire would just be aesthetic.
     */
    protected final boolean canTransferPower = setTransferPower();
    /***
     * The BlockPos of the NodeBank that hosts this connection.
     */
    protected BlockPos parentPos;
    protected int age = 0;

    public NodeConnection(BlockPos parentPos) {
        this.parentPos = parentPos;
    }

    /***
     * The type of spool used to make the connection. Determines things like transfer rate.
     */
	protected WireSpool spoolType;

    /***
     * A Vec3 representing the absolute position of the NodeConnection's source
     */
    protected Vec3 sourcePos;

    /***
     * A Vec3 representing the absolute position of the NodeConnection's destination
     */
    protected Vec3 destPos;

    public String toString() {
        String type = "None";
        if(spoolType != null) 
            type = spoolType.getId(); 

        if(destPos != null) {
            return "From [" 
                + sourcePos.x + ", " 
                + sourcePos.y + ", " 
                + sourcePos.z + "]"
                + ", to [ " +
                + destPos.x + ", " 
                + destPos.y + ", " 
                + destPos.z + "]"
                + ", Type: " + type;
        }
        return "From [" 
                + sourcePos.x + ", " 
                + sourcePos.y + ", " 
                + sourcePos.z + "]"
                + ", to [NULL]"
                + ", Type: " + type;
    }

    /***
     * Increments the age value and returns it. <p>
     * A connection's age, up to 120 ticks, can be used to define special
     * behaviors such as decay time. It can also be used to drive animation
     * steps, which is how cables wiggle when they're created.
     * @return age++ or -1 if age > 120
     */
    public int getAge() {
        if(age > 120 || age == -1) {
            age = -1;
            return -1;
        }
        return (age++) - 1;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean isValid() {
        return spoolType != null && sourcePos != null && destPos != null;
    }

    protected abstract boolean setNeedsLerped();
    protected abstract boolean setTransferPower();

    /***
     * Whether or not this NodeConnection should always render, even if the Player is
     * not looking at it.
     * @return True if this NodeConnection should render its wire no matter what.
     */
    public boolean shouldIgnoreFrustrum() {
        return false;
    }
    
    /***
     * Updates the position of this NodeConnection.
     * Used for lerping and other cool things.
     */
    public void updatePosition(float pTicks) {}

    /***
     * Called by the NodeBank when it's time to write NBT data
     * to the parent BlockEntity.
     * @return
     */
    public abstract CompoundTag writeTo(CompoundTag in);

    /***
     * Gets the WireSpool item represented by this connection.
     * @return
     */
	public WireSpool getSpoolType() {
		return spoolType;
    }

    /***
     * Gets the position of this connection's source.
     * @return
     */
    public Vec3 getSourcePos() {
        return sourcePos;
    }

    /***
     * Gets the position of this connection's destination
     * @return
     */
    public Vec3 getDestPos() {
        return destPos;
    }

    public boolean needsLerped() {
        return needsLerped;
    }

    public BlockPos getParentPos() {
        return parentPos;
    }

    /***
     * Converts this NodeConnection's source and destination positions into a single Vec3
     * representing the relative position from source to destination.
     * @return A Vec3 ({@code sourcePos - destPos})
     */
    public Vec3 getRelative() {
        return sourcePos.subtract(destPos);
    }

    /***
     * Compare two NodeConnections based on their source and destination positions.
     * @param other NodeConnection to compare.
     * @return True if both the source and destination positions the same.
     */
    public boolean equals(Object other) {
        if(other instanceof NodeConnection otherConnection) {
            Vec3[] positions = new Vec3[4];
            positions[0] = this.getSourcePos();
            positions[1] = this.getDestPos();
            positions[2] = otherConnection.getSourcePos();
            positions[3] = otherConnection.getDestPos(); 

            for(Vec3 pos : positions)
                if(pos == null) return false;

            return positions[0].equals(positions[2]) && positions[1].equals(positions[3]);
        }
        return false;
    }
    
    /***
     * Compares NodeConnections by distance
     * @param other
     * @return
     */
    public int compareTo(NodeConnection other) {
        if(this.equals(other)) return 0;
        double distance1 = sourcePos.distanceTo(destPos);
        double distance2 = other.getSourcePos().distanceTo(other.getDestPos());
        
        return Double.compare(distance1, distance2);
    }


    /***
     * This connection "goes nowhere" if its source and destination positions are the same, or if either
     * position is null.
     * @return True if this connection's source and destination positions are equivalent or if either is null.
     */
    public boolean goesNowhere() {
        if(sourcePos == null || destPos == null) return true;
        return sourcePos.equals(destPos);
    }

    /***
     * Exactly the same as {@link #toRelative()}, but returns a Vec3i instead.
     * This is useful for working with BlockPos objects, where less grainularity is required.
     */
    public Vec3i getRelativeI() {
        Vec3 out = getRelative();
        return new Vec3i((int)out.x, (int)out.y, (int)out.z);
    }
}
