package com.quattage.mechano.core.blockEntity;

import java.util.HashSet;
import java.util.Set;

import com.mrh0.createaddition.energy.IWireNode;
import com.mrh0.createaddition.energy.LocalNode;
import com.mrh0.createaddition.energy.network.EnergyNetwork;
import com.quattage.mechano.core.nbt.TagManager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class PassiveElectricBlockEntity extends SyncableBlockEntity implements IWireNode {
    private boolean wasContraption = false;
    private boolean firstTick = true;

    private final 
    private final Set<LocalNode> wireCache = new HashSet<>();
    private final LocalNode[] nodes;
    private final IWireNode[] nodeCache;
    

    public PassiveElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.nodes = new LocalNode[getNodeCount()];
		this.nodeCache = new IWireNode[getNodeCount()];
    }

    @Override
    public int getNodeCount() {
        return 
    }

    @Override
    protected void setData() {
        
    }

    @Override
    protected void getData(TagManager data) {
        
    }

    
}
