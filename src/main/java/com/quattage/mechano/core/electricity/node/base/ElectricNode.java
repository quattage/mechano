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
        this.connections = readAllConnections(tag);
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
        CompoundTag connections = in.getCompound("nodes");
        NodeConnection[] out = new NodeConnection[connections.size()];
        for(String connect : connections.getAllKeys()) {
            Mechano.log("IN " + connect + ": " + in.getCompound(connect));
            if(!in.getCompound(connect).isEmpty())
                out[Integer.parseInt(connect)] = new NodeConnection(in.getCompound(connect));
        }
        return out;
    }   

    private CompoundTag writeAllConnections(CompoundTag in) {
        for(int x = 0; x < connections.length; x++) {
            CompoundTag connect = new CompoundTag();
            if(connections[x] != null) 
                connect = connections[x].writeTo(new CompoundTag());
            in.put("" + x, connect);
        }
        return in;
    }

    public String toString() {
        return "'" + id + "' -> {" + location + ", " + mode + "}";
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
        if(firstNotNullIndex == maxConnections) return false;     // this node is full

        Mechano.log("Connection to " + connection.getDestinationId());
        connections[firstNotNullIndex] = connection;
        Mechano.log(getConnectionsAsString());

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
        //NodeConnection removed = popConnection(index);
        
    }
}
