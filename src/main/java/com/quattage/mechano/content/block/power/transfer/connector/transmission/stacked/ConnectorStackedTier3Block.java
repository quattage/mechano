package com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked;

import java.util.ArrayList;

import com.quattage.mechano.MechanoBlockEntities;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.core.block.upgradable.RootUpgradableBlock;
import com.quattage.mechano.core.block.upgradable.UpgradableBlock;
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

public class ConnectorStackedTier3Block extends UpgradableBlock implements IBE<ConnectorStackedTier3BlockEntity> {

    public static final VoxelShaper SHAPE = ShapeBuilder.newShape(5.5d, 0d, 5.5d, 10.5d, 15d, 10.5d).defaultUp();

    public ConnectorStackedTier3Block(Properties properties) {
        super(properties, MechanoBlocks.CONNECTOR_STACKED_ZERO.get(), 3);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE.get(state.getValue(ORIENTATION).getLocalForward());
    }

    @Override
    public Class<ConnectorStackedTier3BlockEntity> getBlockEntityClass() {
        return ConnectorStackedTier3BlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ConnectorStackedTier3BlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.STACKED_CONNECTOR_THREE.get();
    }

    @Override
    protected Item setUpgradeItem() {
        return getBaseBlock().asItem();
    }

    @Override
    protected ArrayList<RootUpgradableBlock> setUpgradeTiers(ArrayList<RootUpgradableBlock> upgrades) {

        return upgrades;
    }
    
}
