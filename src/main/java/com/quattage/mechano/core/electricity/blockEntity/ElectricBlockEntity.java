package com.quattage.mechano.core.electricity.blockEntity;

import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.core.electricity.battery.BatteryBank;
import com.quattage.mechano.core.electricity.battery.BatteryBankBuilder;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

public abstract class ElectricBlockEntity extends SmartBlockEntity {

    public final BatteryBank<ElectricBlockEntity> batteryBank;

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        BatteryBankBuilder<ElectricBlockEntity> init = new BatteryBankBuilder<ElectricBlockEntity>().at(this);
        populateBatterySettings(init);
        batteryBank = init.build();
    }

    public abstract void populateBatterySettings(BatteryBankBuilder<ElectricBlockEntity> builder);

    /***
     * Refreshes this ElectricBlockEntity's interactions to reflect a BlockState change.<p>
     * This would typically be used after this block is rotated.
     */
    public void reOrient() {
        batteryBank.reflectStateChange(this.getBlockState());
    }
    
    /***
     * Called whenever the Energy stored within this ElectricBlockEntity is
     * changed in any way. Sending block updates and packets is handled by 
     * the BatteryBank object, so you won't have to do that here.
     */
    public void onEnergyUpdated() {

    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return batteryBank.provideEnergyCapabilities(cap, side);
    }

    @Override
    public void initialize() {
        super.initialize();
        reOrient();
        this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        batteryBank.invalidate();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        batteryBank.writeTo(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        batteryBank.readFrom(tag);
        super.read(tag, clientPacket);
    }

    

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        batteryBank.readFrom(tag);
    }
}
