package com.quattage.mechano.core.electricity.node;

import java.util.ArrayList;

import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NodeBank {

    private final BlockEntity target;
    private final BlockPos pos;
    private final ElectricNode[] NODES;

    /***
     * Creates a new NodeBank from an ArrayList of nodes.
     * This NodeBank will have its ElectricNodes pre-populated.
     * @param nodesToAdd ArrayList of ElectricNodes to populate
     * this NodeBank with.
     */
    public NodeBank(BlockEntity target, ArrayList<ElectricNode> nodesToAdd) {
        this.target = target;
        this.pos = target.getBlockPos();
        this.NODES = populate(nodesToAdd);
    }

    /***
     * Creates a new NodeBank populated with null ElectricNodes.
     * As you can probably imagine, a completely blank Nodebank is 
     * entirely useless. I have no idea if this will ever be used, 
     * but if anyone else happens to be reading this, just use the
     * {@link #NodeBank(ArrayList) standard constructor} instead.
     * @param size Size of this NodeBank
     */
    public NodeBank(int size) {
        this.target = null;
        this.pos = null;
        this.NODES = new ElectricNode[size];
    }

    /***
     * Fills this NodeBank with ElectricNodes from the provided ArrayList
     * @param nodesToAdd
     * @return
     */
    private ElectricNode[] populate(ArrayList<ElectricNode> nodesToAdd) {
        ElectricNode[] out = new ElectricNode[nodesToAdd.size()];
        for(int x = 0; x < out.length; x++) {
            out[x] = nodesToAdd.get(x);
        }
        return out;
    }

    public void setOrient(Direction dir) {
        for(ElectricNode node : NODES) {
            node.setOrient(dir);
        }
    }

    public void setOrient(CombinedOrientation dir) {
        for(ElectricNode node : NODES) {
            node.setOrient(dir);
        }
    }

    public ElectricNode[] values() {
        return NODES;
    }

    public int length() {
        return NODES.length;
    }

    public String toString() {
        String out = "NodeBank bound to " + target.getClass().getSimpleName() + " at " + target.getBlockPos() + ":\n";
        for(int x = 0; x < NODES.length; x++) {
            out += "Node " + x + ": " + NODES[x] + "\n";
        }
        return out;
    }
}
