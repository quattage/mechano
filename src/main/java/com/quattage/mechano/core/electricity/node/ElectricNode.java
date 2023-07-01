package com.quattage.mechano.core.electricity.node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class ElectricNode {
    private final int maxConnections;
    private final NodeConnection[] connections;
    private final NodeLocation location;
    private boolean isInput;
    private boolean isOutput;

    public ElectricNode(int maxConnections, NodeLocation location) {
        this.maxConnections = maxConnections;
        this.location = location;
        connections = new NodeConnection[maxConnections];
    }
    
    public int getMaxConnections() {
        return maxConnections;
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
