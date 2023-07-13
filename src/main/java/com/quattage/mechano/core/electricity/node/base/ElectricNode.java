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
 * An ElectricNode is an object that stores important information about the relative location,
 * active connections, and insertion/extraction modes of an arbitrary electrical connection.
 */
public class ElectricNode {

    private final String id;
    private final int index;

    private final int maxConnections;
    private final NodeConnection[] connections;

    private NodeLocation location;
    private NodeMode mode;

    /***
     * Create a new ElectricNode
     * @param location NodeLocation to represent this node in the world
     * @param id User-friendly name (converted to lowercase automatically)
     * @param maxConnections Maximum amount of allowed connections for this ElectricNode
     */
    public ElectricNode(NodeLocation location, String id, int maxConnections, int index) {
        this.index = index;
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
    public ElectricNode(NodeLocation location, String id, NodeMode mode, int maxConnections, int index) {
        this.index = index;
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
    public ElectricNode(BlockPos root, CompoundTag tag) {
        this.id = tag.getString("id");
        this.index = tag.getInt("num");
        this.location = new NodeLocation(root, tag.getCompound("loc"));
        this.mode = NodeMode.fromTag(tag);
        this.maxConnections = tag.getCompound("nodes").size();
        this.connections = readAllConnections(tag.getCompound("nodes"));
    }

    /***
     * Populate the given CompoundTag with data from this ElectricNode
     * @param in CompoundTag to modify
     * @return Modified compoundTag
     */
    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        out.putString("id", id);
        out.putInt("num", index);
        out.put("loc", location.writeTo(new CompoundTag()));
        mode.writeTo(out);
        out.put("nodes", writeAllConnections(new CompoundTag()));
        in.put(id, out);
        return in;
    }

    private NodeConnection[] readAllConnections(CompoundTag in) {
        NodeConnection[] out = new NodeConnection[in.size()];
        for(String connect : in.getAllKeys()) {
            //Mechano.log(connect + " READ: " + in.getCompound(connect));
            if(!in.getCompound(connect).isEmpty()) {
                Mechano.log("TAG CONTAINS: " + in.getCompound(connect));
                out[Integer.parseInt(connect.substring(1))] = 
                    new NodeConnection(in.getCompound(connect));

                Mechano.log("OBJECT GENERATED: " + out[Integer.parseInt(connect.substring(1))]);
            }
        }
        
        return out;
    }   

    private CompoundTag writeAllConnections(CompoundTag in) {
        for(int x = 0; x < connections.length; x++) {
            CompoundTag connect = new CompoundTag();
            if(connections[x] != null) 
                connect = connections[x].writeTo(new CompoundTag());
            in.put("n" + x, connect);
        }
        return in;
    }

    public String toString() {
        return "'" + id + "' -> {" + location + ", " + mode + ", " 
            + "Connections: " + getConnectionsAsString() + "}";
    }

    public String getId() {
        return id;
    }

    public int getIndex() {
        return index;
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
     * Establish a new connection between two ElectricNodes. <p>
     * When an ElectricNode is added, both ends of the connection recieve a
     * NodeConnection. The root connection recieves the normal connnection,
     * and the destination recieves the same connection but inverted.
     * @param connection NodeConnection to add
     * @return True if this Connection was successfully added
     */
    public boolean addConnection(NodeConnection connection) {
        int firstNotNullIndex = getConnectionAmount();
        if(firstNotNullIndex == maxConnections) return false;    // this node is full
        connections[firstNotNullIndex] = connection;
        return true;
        //TODO verbose connection results
    }

    /***
     * The amount of connections this ElectricNode currently has attached to it.
     * @return
     */
    public int getConnectionAmount() {
        for(int x = 0; x < connections.length; x++) {
            if(connections[x] == null) return x;
        }
        return connections.length;
    }

    public String getConnectionsAsString() {
        String out = "";
        for(int x = 0; x < connections.length; x++) {
            out += "" + x + ": " + connections[x] + "\n";
        }
        return out;
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

    public NodeConnection[] getConnections() {
        return connections;
    }

    /***
     * Whether there is a connection at the given index.
     * @param index Index to look
     * @return True of a connection exists at this index
     */
    public boolean hasConnection(int index) {
        //Mechano.log("C:" + getConnection(index));
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

    /***
     * Rotates this ElectricNode to face the given Direction.
     * @param dir Direction to rotate.
     * @return This ElectricNode, but rotated.
     */
    public ElectricNode setOrient(Direction dir) {
        location = location.rotate(dir);
        return this;
    }

    /***
     * Rotates this ElectricNode to face the given CombinedDirection. <p>
     * CombinedDirection accomodates for any combination of directions, so
     * you should probably use a CombinedDirection over 
     * {@link #setOrient(Direction dir) a normal one}.
     * @param dir CombinedDirection to rotate.
     * @return This ElectricNode, but rotateed.
     */
    public ElectricNode setOrient(CombinedOrientation dir) {
        location = location.rotate(dir);
        return this;
    }

    /***
     * Gets the NodeLocation object attached to this ElectricNode
     * @return The NodeLocation attached to this ELectricNode
     */
    public NodeLocation getNodeLocation() {
        return location;
    }

    /***
     * Gets the position of this ElectircNode
     * @return A Vec3 representing the absolute position of this ElectricNode
     */
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
        //NodeConnection removed = popConnection(index);
        
    }
}
