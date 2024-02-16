package com.quattage.mechano.foundation.electricity.system;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

public class GlobalTransferNetworkProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    private final GlobalTransferNetwork networkInstance;
    private final LazyOptional<GlobalTransferNetwork> networkOptional;

    public GlobalTransferNetworkProvider(Level world) {
        networkInstance = new GlobalTransferNetwork(world);
        LazyOptional<GlobalTransferNetwork> networkConstant = LazyOptional.of(() -> Objects.requireNonNull(networkInstance));
        networkConstant.resolve();
        networkOptional = networkConstant;
    }

    @Override
    public CompoundTag serializeNBT() {
        return networkInstance.writeTo(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        networkInstance.readFrom(nbt);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return Mechano.NETWORK_CAPABILITY.orEmpty(capability, networkOptional);
    }
    
}
