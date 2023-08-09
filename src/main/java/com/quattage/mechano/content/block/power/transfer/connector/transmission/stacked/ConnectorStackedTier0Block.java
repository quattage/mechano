package com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked;

import java.util.ArrayList;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.core.block.upgradable.RootUpgradableBlock;
import com.quattage.mechano.core.util.ShapeBuilder;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConnectorStackedTier0Block extends RootUpgradableBlock implements IBE<ConnectorStackedTier0BlockEntity> {

    public static final VoxelShaper SHAPE = ShapeBuilder.newShape(5.5d, 0d, 5.5d, 10.5d, 15d, 10.5d).defaultUp();

    public ConnectorStackedTier0Block(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(ORIENTATION).getLocalForward());
    }

    @Override
    public Class<ConnectorStackedTier0BlockEntity> getBlockEntityClass() {
        return ConnectorStackedTier0BlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ConnectorStackedTier0BlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.STACKED_CONNECTOR_ZERO.get();
    }

    @Override
    protected Item setUpgradeItem() {
        return getBaseBlock().asItem();
    }

    @Override
    protected ArrayList<RootUpgradableBlock> setUpgradeTiers(ArrayList<RootUpgradableBlock> upgrades) {
        upgrades.add(MechanoBlocks.CONNECTOR_STACKED_ZERO.get());
        upgrades.add(MechanoBlocks.CONNECTOR_STACKED_ONE.get());
        upgrades.add(MechanoBlocks.CONNECTOR_STACKED_TWO.get());
        upgrades.add(MechanoBlocks.CONNECTOR_STACKED_THREE.get());
        return upgrades;
    }
    
}
