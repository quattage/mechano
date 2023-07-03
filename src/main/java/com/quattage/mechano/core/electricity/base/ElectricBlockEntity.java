package com.quattage.mechano.core.electricity.base;

import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;
import com.quattage.mechano.core.util.nbt.TagManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ElectricBlockEntity extends SyncableBlockEntity {

    public final NodeBank NODES;

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        NodeBankBuilder init = new NodeBankBuilder().at(this);
        addConnections(init);
        NODES = init.build();
    }

    /***
     * Add connections to the NodeBankBuilder here. Example:<pre>
     * builder
     *  .add(8, 8, 8, Direction.NORTH, "MYNODE1", 1)
     *  .add(8, 8, 8, Direction.NORTH, "MYNODE2", 1);
     * </pre>
     * This example adds two nodes to the NodeBankBuilder. Both of 
     * which lie at the exact center of the parent block, and have allow
     * only 1 connection.
     * @param builder The NodeBuilder to add connections to
     */
    public abstract void addConnections(NodeBankBuilder builder);

    public void setOrient(Direction dir) {
        NODES.setOrient(dir);
    }

    public void setOrient(CombinedOrientation dir) {
        NODES.setOrient(dir);
    }
    
    @Override
    protected void setData() {}

    @Override
    protected void getData(TagManager data) {}
}
