package com.quattage.mechano.core.electricity.node.base;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.block.orientation.SimpleOrientation;
import com.quattage.mechano.core.block.orientation.VerticalOrientation;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.connection.ElectricNodeConnection;
import com.quattage.mechano.core.electricity.node.connection.NodeConnectResult;
import com.quattage.mechano.core.electricity.node.connection.NodeConnection;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
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
    public final NodeConnection[] connections;

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
    public ElectricNode(BlockEntity target, CompoundTag tag) {
        this.id = tag.getString("id");
        this.index = tag.getInt("num");

        this.location = new NodeLocation(target, tag.getCompound("loc"));

        this.mode = NodeMode.fromTag(tag);
        this.maxConnections = tag.getCompound("nodes").size();
        this.connections = readAllConnections(target, tag.getCompound("nodes"));
    }

    /***
     * Initializes critical data for the NodeConnections stored within this ElectricNode.
     * This is done during the first tick of the parent BlockEntity, since we need the world
     * in this context in order to retrieve the relevent data. 
     * @param target The parent BlockEntity (this is stored in the NodeBank).
     * @throws NullPointerException If the target is null or the target's world is null. Make sure
     * you call this in the correct context (after world load is complete) or you'll encouter this.
     */
    public void initConnections(BlockEntity target) {
        if(target == null || target.getLevel() == null) 
            throw new NullPointerException("ElectricNode cannot initialize - World is null!");

        for(int x = 0; x < connections.length; x++) {
            NodeConnection connection = connections[x];
            if(connection instanceof ElectricNodeConnection ec && ec.needsUpdate()) {
                NodeBank bank = NodeBank.retrieveFrom(target.getLevel(), target, ec.getRelativePos());
                if(bank != null)
                    ec.setTo(target.getBlockPos(), bank.get(ec.getDestinationID()).getPosition());
            }
        }
    }

    /***
     * Populate the given CompoundTag with data from this ElectricNode
     * @param in CompoundTag to modify
     * @return Modified CompoundTag
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

    private NodeConnection[] readAllConnections(BlockEntity target, CompoundTag in) {
        NodeConnection[] out = new NodeConnection[in.size()];
        for(String connect : in.getAllKeys()) {
            if(!in.getCompound(connect).isEmpty()) {
                out[Integer.parseInt(connect.substring(1))] = 
                    new ElectricNodeConnection(target, getPosition(), in.getCompound(connect));
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
        return "'" + id + "' \n\t" + location + ", \n\t" + mode + ", \n\t" 
            + getConnectionsAsString();
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

    public float getHitSize() {
        return location.getHitSize();
    }

    public boolean connectionExists(NodeConnection checkConnection) {
        for(NodeConnection thisConnection : connections)
            if(checkConnection.equals(thisConnection)) return true; 
        return false;
    }
    
    /***
     * Establish a new connection between two ElectricNodes. <p>
     * When an ElectricNode is added, both ends of the connection recieve a
     * NodeConnection. The root connection recieves the normal connnection,
     * and the destination recieves the same connection but inverted.
     * @param connection NodeConnection to add
     * @return NodeConnectResult - NODE_FULL or LINK_EXISTS if this connection 
     * fails, LINK_ADDED if this connection succeeds.
     */
    public NodeConnectResult addConnection(NodeConnection connection) {
        int firstNullIndex = getFirstNullIndex();

        if(firstNullIndex == -1) return NodeConnectResult.NODE_FULL;
        if(connectionExists(connection)) return NodeConnectResult.LINK_EXISTS;

        connections[firstNullIndex] = connection;
        return NodeConnectResult.LINK_ADDED;
    }

    /***
     * Informally swaps out the last connection in this NodeBank with the given connection.
     * Note that this doesn't do any checks or wire dropping. Useful for replacing fake
     * connections with real ones.
     * @param connection
     * @return NodeConnectResult WIRE_SUCCESS if the connection was successfully replaced.
     */
    public NodeConnectResult replaceLastConnection(NodeConnection connection) {
        int firstNull = getConnectionAmount();
        if(connectionExists(connection)) return NodeConnectResult.LINK_EXISTS;
        connections[firstNull] = connection;
        return NodeConnectResult.WIRE_SUCCESS;
    }

    /***
     * Sets the latest connection to this ElectricNode to null.
     */
    public void nullifyLastConnection() {
        int lastIndex = getConnectionAmount(); 
        if(lastIndex >= 0) {
            connections[lastIndex] = null;
        }
    }

    /***
     * Returns true if this ElectricNode contains a NodeConnection
     * that needs to be rendered off-screen. Useful when a NodeConnection
     * is attached to a player, where looking away can cause the visible
     * WireModel to vanish.
     * @return True if this NodeBank contains a NodeConnection that should always render.
     */
    public boolean shouldAlwaysRender() {
        for(NodeConnection c : connections)
            if(c != null && c.shouldIgnoreFrustrum()) return true;
        return false;
    }

    /***
     * The amount of connections this ElectricNode currently has attached to it.
     * @return
     */
    public int getFirstNullIndex() {
        for (int x = 0; x < connections.length; x++) {
            if (connections[x] == null) return x;
        }
        return -1;
    }

    /***
     * Removes all connections from this ElectricNode that target the provided 
     * NodeBank.
     * @param target Connections targeting this NodeBank will be removed
     * @return True if this ElectricNode's connections were altered in any way.
     */
    public boolean removeConnectionsInvolving(NodeBank origin) {
        boolean changed = false;
        for(int x = 0; x < connections.length; x++) {
            BlockPos parentPos = connections[x].getParentPos();
            if(parentPos != null && origin.isAt(parentPos)) {
                clearConnection(x, true, false);
                changed = true;
            }
        }
        if(changed) sortConnections();
        return changed;
    }
    
    /***
     * Gets all of the NodeBanks referenced by the connections in
     * this ElectricNode.
     * @param parent - The parent NodeBank to use as a basis
     * for finding target NodeBanks.
     * @return A new ArrayList of NodeBanks.
     */
    public HashSet<NodeBank> getAllTargetBanks(NodeBank parent) {
        HashSet<NodeBank> out = new HashSet<NodeBank>();
        for(NodeConnection c : connections) {
            if(c instanceof ElectricNodeConnection ec) {
                NodeBank bank = NodeBank.retrieveFrom(parent.getWorld(), 
                    parent.target, ec.getRelativePos());
                if(bank != null) {
                    out.add(bank);
                }
            }
        }
        return out;
    }

    /***
     * Sorts the list of connections, where non-null values are first.
     */
    public void sortConnections() {
        Arrays.sort(connections, new Comparator<NodeConnection>() {
            public int compare(NodeConnection n1, NodeConnection n2) {
                if (n1 == null && n2 == null)
                    return 0;
                if (n1 == null)
                    return 1;
                if (n2 == null)
                    return -1;
                return n1.compareTo(n2);
            }
        });
    }

    /***
     * The gets the amount of connections made to this ElectricNode.
     * @return
     */
    public int getConnectionAmount() {
        int nullIndex = getFirstNullIndex();
        if(nullIndex == 0) return 0;
        if(nullIndex == -1) return connections.length - 1;
        return nullIndex - 1;
    }

    public String getConnectionsAsString() {
        String out = "";
        for(int x = 0; x < connections.length; x++) {
            out += "Connection " + x + ": " + (connections[x] == null ? "Empty" : connections[x]) + "\n\t";
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
     * Rotates this ElectricNode to face the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     * @return This ElectricNode, but rotated.
     */
    public ElectricNode rotateNode(Direction dir) {
        location = location.rotate(dir);
        return this;
    }

    /***
     * Rotates this ElectricNode to face the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     * @return This ElectricNode, but rotated.
     */
    public ElectricNode rotateNode(CombinedOrientation dir) {
        location = location.rotate(dir);
        return this;
    }

    /***
     * Rotates this ElectricNode to face the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     * @return This ElectricNode, but rotated.
     */
    public ElectricNode rotateNode(SimpleOrientation dir) {
        location = location.rotate(dir);
        return this;
    }

    /***
     * Rotates this ElectricNode to face the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     * @return This ElectricNode, but rotated.
     */
    public ElectricNode rotateNode(VerticalOrientation dir) {
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

    /***
     * Gets the position of this ElectircNode. Different than
     * {@link #getPosition() getPosition()} in that it returns
     * the local offset of this ElectricNode, rather than its
     * worldly position.
     */
    public Vec3 getLocalPosition() {
        return location.getDirectionalOffset();
    }

    public Color getColor() {
        return mode.getSelected();
    }

    public Color getColor(float percent) {
        return mode.getColor(percent);
    }

    public NodeMode getMode() {
        return mode;
    }

    /***
     * Removes the connection at the given index. 
     * @param index Numerical index of the Connection to remove.
     * @param shouldDropWire (Optional, true) If true, wire items will spawn in the world
     * when this connection is broken.
     * @param shouldSort (Optional, true) If true, the connections array will be sorted
     * after removal. Sorting is required for subsequent removals.
     */
    public void clearConnection(int index) {
        clearConnection(index, true, true);
    }

    /***
     * Removes the connection at the given index. 
     * @param index Numerical index of the Connection to remove.
     * @param shouldDropWire (Optional, true) If true, wire items will spawn in the world
     * when this connection is broken.
     * @param shouldSort (Optional, true) If true, the connections array will be sorted
     * after removal. Sorting is required for subsequent removals.
     */
    public void clearConnection(int index, boolean shouldDropWire, boolean shouldSort) {
        connections[index] = null;
        if(shouldSort) sortConnections();
        // TODO drop wires
    }
}
