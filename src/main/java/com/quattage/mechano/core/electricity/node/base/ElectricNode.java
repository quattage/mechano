package com.quattage.mechano.core.electricity.node.base;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ElectricNode {
    private final int maxConnections;
    private final NodeConnection[] connections;
    private NodeLocation location;
    private final String id;

    private NodeMode mode;

    public ElectricNode(NodeLocation location, String id, int maxConnections) {
        this.maxConnections = maxConnections;
        this.location = location;
        this.id = id.toLowerCase();
        this.mode = NodeMode.from(true, true);
        connections = new NodeConnection[maxConnections];
    }

    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        out.put("NodeLocation", location.writeTo(new CompoundTag()));
        mode.writeTo(out);
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

    public void clearConnection(int index) {
        clearConnection(index, true);
    }

    public void clearConnection(int index, boolean shouldDropWire) {
        NodeConnection removed = popConnection(index);
        
    }
}
