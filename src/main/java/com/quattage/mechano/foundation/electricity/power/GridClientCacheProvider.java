package com.quattage.mechano.foundation.electricity.power;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

public class GridClientCacheProvider implements ICapabilityProvider {

    private final GridClientCache cacheInstance;
    private final LazyOptional<GridClientCache> cacheOptional;

    public GridClientCacheProvider(Level world) {
        this.cacheInstance = new GridClientCache(world);
        LazyOptional<GridClientCache> networkConstant = LazyOptional.of(() -> Objects.requireNonNull(cacheInstance));
        networkConstant.resolve();
        this.cacheOptional = networkConstant;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return Mechano.CLIENT_CACHE_CAPABILITY.orEmpty(capability, cacheOptional);
    }
}
