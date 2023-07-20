package com.quattage.mechano.core.electricity;

import java.util.List;

import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
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
     *  builder
            .newNode()
                .id("out1")
                .at(0, 6, 11)
                .mode("O")
                .connections(2)
                .build()
            .newNode()
                .id("in1")
                .at(16, 10, 6) 
                .mode("I")
                .connections(2)
                .build()
        ;
     * </pre>
     * only 1 connection.
     * @param builder The NodeBuilder to add connections to
     */
    public abstract void addConnections(NodeBankBuilder builder);

    public void setOrient(Direction dir) {
        nodes = nodes.setOrient(dir);
    }

    /***
     * Sets the orientation of this ElectricBlockEntity's NodeBank to the given 
     * CombinedDirection. <p> Changing the orientation of a NodeBank effectively 
     * rotates every ElectricNode about this BlockEntity's origin (0.5, 0.5, 0.5). 
     * @param dir
     */
    public void setOrient(CombinedOrientation dir) {
        nodes = nodes.setOrient(dir);
    }

    /***
     * Sets the orientation of this ElectricBlockEntity's NodeBank to the 
     * current direction of the parent block's BlockState.
     */
    public void refreshOrient() {
        BlockState state = this.getBlockState();
        if(state != null && state.getBlock() instanceof CombinedOrientedBlock) 
            setOrient(state.getValue(CombinedOrientedBlock.ORIENTATION));
    }

    @Override
    public void initialize() {        
        super.initialize();

        nodes.initConnections();
        refreshOrient();
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
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        // TODO Auto-generated method stub
        super.onDataPacket(connection, packet);
    }

    

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        nodes.readFrom(tag);
    }
}
