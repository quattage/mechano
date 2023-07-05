package com.quattage.mechano.core.blockEntity;

import java.util.List;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;
import com.quattage.mechano.core.util.nbt.TagManager;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ElectricBlockEntity extends SmartBlockEntity {

    public NodeBank nodes;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        NodeBankBuilder init = new NodeBankBuilder().at(this);
        addConnections(init);
        nodes = init.build();
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
        nodes = nodes.setOrient(dir);
    }

    public void setOrient(CombinedOrientation dir) {
        nodes = nodes.setOrient(dir);
    }
    
    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        nodes.writeTo(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        nodes.readFrom(tag);
        super.read(tag, clientPacket);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return nodes.writeTo(new CompoundTag());
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        nodes.readFrom(tag);
    }
}
