package com.quattage.mechano.core.electricity.blockEntity;

import java.util.List;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.SimpleOrientedBlock;
import com.quattage.mechano.core.block.VerticallyOrientedBlock;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.block.orientation.SimpleOrientation;
import com.quattage.mechano.core.block.orientation.VerticalOrientation;
import com.quattage.mechano.core.electricity.block.ElectricBlock;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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

    /***
     * Rotates this ElectricBlockEntity's NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     */
    public void rotateNodeBank(Direction dir) {
        nodes = nodes.rotateAllNodes(dir);
    }

    /***
     * Rotates this ElectricBlockEntity's NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     */
    public void rotateNodeBank(CombinedOrientation dir) {
        nodes = nodes.rotateAllNodes(dir);
    }

    /***
     * Rotates this ElectricBlockEntity's NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     */
    public void rotateNodeBank(SimpleOrientation dir) {
        nodes = nodes.rotateAllNodes(dir);
    }

    /***
     * Rotates this ElectricBlockEntity's NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     */
    public void rotateNodeBank(VerticalOrientation dir) {
        nodes = nodes.rotateAllNodes(dir);
    }

    /***
     * Sets the orientation of this ElectricBlockEntity's NodeBank to the 
     * current direction of the parent block's BlockState.
     */
    public void refreshOrient() {
        BlockState state = this.getBlockState();
        Block caller = state.getBlock();
        if(state != null && caller != null) {
            if(caller instanceof DirectionalBlock db) {
                rotateNodeBank(state.getValue(DirectionalBlock.FACING));
            }
            else if(caller instanceof HorizontalDirectionalBlock hb) {
                rotateNodeBank(state.getValue(HorizontalDirectionalBlock.FACING));
            }
            else if(caller instanceof CombinedOrientedBlock cb) {
                rotateNodeBank(state.getValue(CombinedOrientedBlock.ORIENTATION));
            }
            else if (caller instanceof SimpleOrientedBlock sb) {
                rotateNodeBank(state.getValue(SimpleOrientedBlock.ORIENTATION));
            }
            else if (caller instanceof VerticallyOrientedBlock vb) {
                rotateNodeBank(state.getValue(VerticallyOrientedBlock.ORIENTATION));
            }
        }
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
