package com.quattage.mechano.core.electricity.blockEntity;

import java.util.List;

import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class WireNodeBlockEntity extends ElectricBlockEntity {

    public final NodeBank<WireNodeBlockEntity> nodeBank;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public WireNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
        NodeBankBuilder<WireNodeBlockEntity> init = new NodeBankBuilder<WireNodeBlockEntity>().at(this);
        populateNodeSettings(init);
        nodeBank = init.build();
    }

    /***
     * Prepare this ElectricBlockEntity instance with all of its associated properties.
     * <pre>
        nodeBank
        .capacity(7500)       // this bank can hold up to 7500 FE
        .maxIO(70)            // this bank can input and output up to 70 FE/t
        .newNode()            // build a new node
            .id("out1")       // set the name of the node
            .at(0, 6, 11)     // define the pixel offset of the node
            .mode("O")        // this node is an output node
            .connections(2)   // this node can connect to up to two other nodes
            .build()          // finish building this node
        .newNode()            // build a new node
            .id("in1")        // set the name of the node
            .at(16, 10, 6)    // define the pixel offset of the node
            .mode("I")        // this node is an input node
            .connections(2)   // this node can connect to up to two other nodes
            .build()          // finish building this node
        ;
     * </pre>
     * @param builder The NodeBuilder to add connections to
     */
    public abstract void populateNodeSettings(NodeBankBuilder<WireNodeBlockEntity> builder);

    @Override
    public void reOrient() {
        nodeBank.reflectStateChange(this.getBlockState());
        super.reOrient();
    }

    @Override
    public void remove() {
        if(!this.level.isClientSide)
            nodeBank.destroy();
        super.remove();
    }

    @Override
    public void initialize() {
        super.initialize();
        nodeBank.init();
        reOrient();
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }
    
    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        nodeBank.writeTo(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        nodeBank.readFrom(tag);
        super.read(tag, clientPacket);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        nodeBank.readFrom(tag);
    }
}
