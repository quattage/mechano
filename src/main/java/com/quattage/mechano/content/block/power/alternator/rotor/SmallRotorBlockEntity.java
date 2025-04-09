package com.quattage.mechano.content.block.power.alternator.rotor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SmallRotorBlockEntity extends AbstractRotorBlockEntity implements RotorReferable {

    public SmallRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public int getStatorCircumference() {
        return 8;
    }

    @Override
    public int getStatorRadius() {
        return 1;
    }

    @Override
    protected float getEfficiencyBonus() {
        return 1f;
    }

    @Override
    public AbstractRotorBlockEntity getRotorBE() {
        return this;
    }

    @Override
    public BlockState getRotorState() {
        return getBlockState();
    }

    @Override
    public float calculateStressApplied() {
        float impact = 4;
        this.lastStressApplied = impact;
        return impact;
    }
}
