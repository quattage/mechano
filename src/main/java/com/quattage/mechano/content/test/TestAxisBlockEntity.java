package com.quattage.mechano.content.test;

import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.grid.client.AnchorPoints.Builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestAxisBlockEntity extends PowerGridBlockEntity {

    public TestAxisBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void addAnchors(Builder anchors) {
        anchors
        .add()
            .connections(5)
            .radius(2)
            .at(16, 10, 6)
            .make()
        .add()
            .at(0, 6, 11)
            .connections(2)
            .radius(2)
            .make();
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
