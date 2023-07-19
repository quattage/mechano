package com.quattage.mechano.core.electricity.node;

import java.util.ArrayList;

import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.electricity.node.base.ElectricNodeBuilder;
import com.quattage.mechano.core.electricity.node.base.NodeLocation;
import com.quattage.mechano.core.electricity.node.base.NodeMode;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NodeBankBuilder {
    private BlockEntity target = null;
    private final ArrayList<ElectricNode> nodesToAdd = new ArrayList<ElectricNode>();

    public NodeBankBuilder() {};

    /***
     * Bind this NodeBank to a given BlockEntity
     * @param pos
     * @return
     */
    public NodeBankBuilder at(BlockEntity target) {
        this.target = target;
        return this;
    }

    public NodeBankBuilder add(ElectricNode node) {
        nodesToAdd.add(node);
        return this;
    }

    public ElectricNodeBuilder newNode() {
        return new ElectricNodeBuilder(this, target);
    }

    public NodeBankBuilder add(NodeLocation location, String id, int maxConnections) {
        return add(new ElectricNode(location, id, maxConnections, nodesToAdd.size()));
    }

    public NodeBankBuilder add(NodeLocation location, String id, NodeMode mode, int maxConnections) {
        return add(new ElectricNode(location, id, mode, maxConnections, nodesToAdd.size()));
    }

    public NodeBankBuilder add(int x, int y, int z, String id, int maxConnections) {
        return add(new ElectricNode(new NodeLocation(target.getBlockPos(), x, y, z, Direction.NORTH), id, maxConnections, nodesToAdd.size()));
    }

    public NodeBankBuilder add(int x, int y, int z, Direction defaultDir, String id, int maxConnections) {
        return add(new ElectricNode(new NodeLocation(target.getBlockPos(), x, y, z, defaultDir), id, maxConnections, nodesToAdd.size()));
    }

    private void doCompleteCheck() {
        if(target == null) throw new IllegalStateException("NodeBank cannot be built with a null BlockEntity! use .at() during construction!");
        if(nodesToAdd.isEmpty()) throw new IllegalStateException("NodeBank cannot be built with no ElectricNodes! use .add() to add nodes!");
    }

    public NodeBank build() {
        doCompleteCheck();
        return new NodeBank(target, nodesToAdd);
    }
}
