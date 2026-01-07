package com.quattage.mechano.content.creative;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.BlockJack;
import com.quattage.mechano.content.connector.BlockWithConnections;
import com.quattage.mechano.foundation.block.CombinedOrientedBlock;
import com.quattage.mechano.foundation.block.hitbox.MechanoHitboxes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CreativeVoltaplastBlock extends BlockWithConnections<CreativeVoltaplastBlockEntity> {

    public CreativeVoltaplastBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean canFloat() {
        return true;
    }


    @Override
    public boolean isConnectorAllowed(LevelReader world, BlockPos connectorPos, BlockState connectorState,
            BlockPos thisPos, BlockState thisState) {
        GriddableBlockEntity gbe = getBlockEntity(world, thisPos);
        if(gbe == null) return false;
        GriddableTerminus terminus = gbe.getTerminus();
        if(terminus.isEmpty()) return false;
        for(AncillaryNode ancillary : terminus.getAncillaries()) {
            if(!(ancillary instanceof BlockJack bj)) continue;
            if(!bj.getPos(thisPos).equals(connectorPos)) continue;
            Direction connectorDir = connectorState.getValue(CombinedOrientedBlock.ORIENTATION).getLocalUp();
            if(connectorDir == bj.getFacing()) return true;
        }
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return MechanoHitboxes.CREATIVE_VOLTAPLAST.get(state);
    }

    @Override
    public Class<CreativeVoltaplastBlockEntity> getBlockEntityClass() {
        return CreativeVoltaplastBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreativeVoltaplastBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.CREATIVE_VOLTAPLAST.get();
    }


}
