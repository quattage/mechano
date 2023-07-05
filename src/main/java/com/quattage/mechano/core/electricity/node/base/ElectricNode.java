package com.quattage.mechano.core.electricity.node.base;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
/***
 * Manages serialization of data relating to ElectricNodes as well as
 * active connections to other Electric Nodes. <p> The ElectricNode is 
 * mostly designed to be stored by a NodeBank.
 */
public class ElectricNode {
    private final int maxConnections;
    private final NodeConnection[] connections;
    private NodeLocation location;
    private final String id;

    private NodeMode mode;

    /***
     * Create a new ElectricNode
     * @param location NodeLocation to represent this node in the world
     * @param id User-friendly name (converted to lowercase automatically)
     * @param maxConnections Maximum amount of allowed connections for this ElectricNode
     */
    public ElectricNode(NodeLocation location, String id, int maxConnections) {
        this.maxConnections = maxConnections;
        this.location = location;
        this.id = id.toLowerCase();
        this.mode = NodeMode.from(true, true);
        connections = new NodeConnection[maxConnections];
    }

    /***
     * Create a new ElectricNode
     * @param location NodeLocation to represent this node in the world
     * @param id User-friendly name (converted to lowercase automatically)
     * @param NodeMode Default mode for this ElectricNode
     * @param maxConnections Maximum amount of allowed connections for this ElectricNode
     */
    public ElectricNode(NodeLocation location, String id, NodeMode mode, int maxConnections) {
        this.maxConnections = maxConnections;
        this.location = location;
        this.id = id.toLowerCase();
        this.mode = mode;
        connections = new NodeConnection[maxConnections];
    }

    /***
     * Create a new ElectricNode from the data stored within a CompoundTag. <p>
     * This constructor operates in a vaccuum, you shouldn't ordinarily have to run it yourself  
     * since it's done for you by the NodeBank, but it's public f you want to use it. <p>
     * Make sure you do some sanity checks on your CompoundTag, since this constructor 
     * won't throw anything on its own.
     * @param root Root position of this ElectricNode
     * @param id User-friendly name (converted to lowercase automatically)
     * @param tag Tag containing the relevent data
     */
    public ElectricNode(BlockPos root, String id, CompoundTag tag) {
        this.maxConnections = tag.getInt("Connections");
        this.location = new NodeLocation(root, tag.getCompound("NodeLocation"));
        this.mode = NodeMode.fromTag(tag);
        this.id = id;
        connections = new NodeConnection[maxConnections];
    }

    /***
     * Populate the given CompoundTag with data from this ElectricNode
     * @param in CompoundTag to modify
     * @return Modified compoundTag
     */
    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        out.put("NodeLocation", location.writeTo(new CompoundTag()));
        mode.writeTo(out);
        out.putInt("Connections", maxConnections);
        in.put(id, out);
        return in;
    }

    public String toString() {
        return "'" + id + "' -> {" + location + ", " + mode + "}";
    }

    public String getId() {
        return id;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public AABB getHitbox() {
        return location.getHitbox();
    }

    public double getHitSize() {
        return location.getHitSize();
    }

    /***
     * Create a new connection 
     * @param from from this node
     * @param pos to this BLockPos
     * @param to to this node @ the given BlockPos
     */
    public void createConnection(int from, int to, BlockPos pos) {
        
    }

    /***
     * Gets a NodeConnection at the given index.
     * @param index index to get
     * @return The NodeConnection at the specified index.
     * @throws ArrayIndexOutOfBoundsException
     */
    public NodeConnection getConnection(int index) {
        return connections[index];
    }

    /***
     * Whether there is a connection at the given index.
     * @param index Index to look
     * @return True of a connection exists at this index
     */
    public boolean hasConnection(int index) {
        return getConnection(index) != null;
    }

    /***
     * Gets a NodeConnection at the given index and invalidates it.
     * @param index index to get
     * @return The NodeConnection at the specified index, after it was removed.
     * @throws ArrayIndexOutOfBoundsException
     */
    public NodeConnection popConnection(int index) {
        NodeConnection out = connections[index];
        connections[index] = null;
        return out;
    }

    public ElectricNode setOrient(Direction dir) {
        location = location.rotate(dir);
        return this;
    }

    public ElectricNode setOrient(CombinedOrientation dir) {
        location = location.rotate(dir);
        return this;
    }

    public NodeLocation getNodeLocation() {
        return location;
    }

    public Vec3 getPosition() {
        return location.get();
    }

    public Color getColor() {
        return mode.getColor();
    }

    public Color getColor(float percent) {
        return mode.getColor(percent);
    }

    public NodeMode getMode() {
        return mode;
    }

    public void clearConnection(int index) {
        clearConnection(index, true);
    }

    public void clearConnection(int index, boolean shouldDropWire) {
        NodeConnection removed = popConnection(index);
        
    }
}
