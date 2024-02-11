package com.quattage.mechano.foundation.electricity;

import java.util.List;

import com.quattage.mechano.foundation.electricity.builder.AnchorBankBuilder;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class WireAnchorBlockEntity extends ElectricBlockEntity {

    private final AnchorPointBank<WireAnchorBlockEntity> anchors;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public WireAnchorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
        AnchorBankBuilder<WireAnchorBlockEntity> init = new AnchorBankBuilder<WireAnchorBlockEntity>().at(this);
        createWireNodeDefinition(init);
        anchors = init.build();
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
    public abstract void createWireNodeDefinition(AnchorBankBuilder<WireAnchorBlockEntity> builder);

    public AnchorPointBank<?> getAnchorBank() {
        return anchors;
    }

    @Override
    public void reOrient() {
        anchors.reflectStateChange(this.getBlockState());
        super.reOrient();
    }

    @Override
    public void remove() {
        if(!this.level.isClientSide)
            anchors.destroy();
        super.remove();
    }

    @Override
    public void initialize() {
        super.initialize();
        reOrient();
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }
}
