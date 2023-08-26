package com.quattage.mechano.core.electricity.blockEntity;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.SimpleOrientedBlock;
import com.quattage.mechano.core.block.VerticallyOrientedBlock;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.NodeBankBuilder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public abstract class ElectricBlockEntity extends SmartBlockEntity {

    public final NodeBank nodeBank;

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(20);
        NodeBankBuilder init = new NodeBankBuilder().at(this);
        prepare(init);
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
    public abstract void prepare(NodeBankBuilder nodeBank);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        super.getCapability(cap, side);
        return nodeBank.provideEnergyCapabilities(cap, side);
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
                nodeBank.rotate(state.getValue(DirectionalBlock.FACING));
            }
            else if(caller instanceof HorizontalDirectionalBlock hb) {
                nodeBank.rotate(state.getValue(HorizontalDirectionalBlock.FACING));
            }
            else if(caller instanceof CombinedOrientedBlock cb) {
                nodeBank.rotate(state.getValue(CombinedOrientedBlock.ORIENTATION));
            }
            else if (caller instanceof SimpleOrientedBlock sb) {
                nodeBank.rotate(state.getValue(SimpleOrientedBlock.ORIENTATION));
            }
            else if (caller instanceof VerticallyOrientedBlock vb) {
                nodeBank.rotate(state.getValue(VerticallyOrientedBlock.ORIENTATION));
            }
        }
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
        refreshOrient();
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        nodeBank.loadEnergy();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        nodeBank.invalidateEnergy();
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
