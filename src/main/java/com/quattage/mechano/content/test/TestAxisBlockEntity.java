package com.quattage.mechano.content.test;

import com.quattage.mechano.api.anchor.AnchorCollection.DynamicAnchorArray;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestAxisBlockEntity extends GriddableBlockEntity {

    public TestAxisBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void constructAnchors(DynamicAnchorArray builder) {
        builder
        .newAnchor()
            .connections(5)
            .radius(2)
            .at(16, 10, 6)
            .addTo(this)
        .newAnchor()
            .connections(2)
            .radius(2)
            .at(0, 6, 11)
            .add();
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    protected void saveTo(CompoundTag tag, Provider registries) {

    }

    @Override
    protected void loadFrom(CompoundTag tag, Provider registries) {

    }
}
