package com.quattage.mechano.foundation.electricity.impl;

import com.quattage.mechano.foundation.electricity.WattBatteryHandlable;
import com.quattage.mechano.foundation.electricity.WattBatteryHandler;
import com.quattage.mechano.foundation.helper.builder.WattBatteryHandlerBuilder;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/***
 * The ElectricBlockEntity provides the base functionality and handlers required to 
 * use the sided WattStorable capability.
 * @see {@link com.quattage.mechano.foundation.electricity.impl.ElectroKineticBlockEntity <code>ElectroKineticBlockEntity</code>} for implementing electricity with Kinetics.
 */
public abstract class ElectricBlockEntity extends SmartBlockEntity implements WattBatteryHandlable, IHaveGoggleInformation {

    public final WattBatteryHandler<ElectricBlockEntity> battery;

    public ElectricBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        WattBatteryHandlerBuilder<ElectricBlockEntity> init = new WattBatteryHandlerBuilder<ElectricBlockEntity>().at(this);
        createWattHandlerDefinition(init);

        battery = init.build();
    }

    @Override
    public WattBatteryHandler<? extends WattBatteryHandlable> getWattBatteryHandler() {
        return battery;
    }

    @Override
    public void reOrient(BlockState state) {
        battery.reflectStateChange(state);
    }

    @Override
    public void tick() {
        super.tick();
        if(getLevel().isClientSide()) return;
        battery.tickWatts();
    }

    @Override
    public void onLoad() {
        battery.loadAndUpdate(getBlockState());
        super.onLoad();
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return battery.provideEnergyCapabilities(cap, side);
    }

    @Override // runs on first tick
    public void initialize() {
        reOrient(getBlockState());
        super.initialize();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        battery.invalidate();
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        battery.writeTo(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        battery.readFrom(tag);
        super.read(tag, clientPacket);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        battery.readFrom(tag);
    }
}