package com.quattage.mechano.foundation.electricity.core;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

/***
 * Provides common implementations for directional ForgeEnergy capabilities.
 */
public interface DirectionalEnergyStorable {

    /***
     * Called by LocalEnergyStorage whenever stored energy is changed.
     * Implementations should usually use <code> BlockEntity.setChanged() 
     * </code> here. <p> You may also want to look into creating an S2C
     * packet that gets used here to sync changes.
     */
    public abstract void onEnergyUpdated();

    /***
     * Required in order to set the energy level of a BlockEntity on the client.
     */
    public abstract void setEnergyStored(int energy);

    /***
     * Gets the energy Capability of this LocalEnergyStorage. <p>
     * Usually you'd use:
     * <pre>
     * {@literal LazyOptional<IEnergyStorage>.cast();}
     * </pre>
     * @param <T>
     * @return Infered generic LazyOptional container for the Capability. 
     */
    public abstract <T> @NotNull LazyOptional<T> getEnergyHandler();

    /***
     * Provides energy capabilities for the given side of the parent block. <p>
     * More specifically, it compares the given side with a list of valid sides.
     * This list of valid sides should be stored by your BlockEntity or whatever
     * object is implementing this code.
     * 
     * @param cap The capability in question.
     * @param side Side to check.
     * @param allInteractions List of RelativeDirections. (left, right, up, down, etc.) <p>
     * Actual directions for these RelativeDirections can be obtained by rotating them on the
     * BlockEntity side of things.
     * @return Infered generic LazyOptional container for the Capability.
     */
    default <T> @NotNull LazyOptional<T> getCapabilityForSide(@NotNull Capability<T> cap, 
        @Nullable Direction side, Direction[] energyDirs) {

        if(side == null || cap != ForgeCapabilities.ENERGY) return LazyOptional.empty();
        if(energyDirs.length >= 6) return getEnergyHandler();
        for(int x = 0; x < energyDirs.length; x++) 
            if(side.equals(energyDirs[x])) return getEnergyHandler();

        return LazyOptional.empty();
    }

    static boolean hasMatchingCaps(Level world, BlockPos parentPos, Direction dirToCheck) {
        BlockEntity be = world.getBlockEntity(parentPos.relative(dirToCheck.getOpposite()));
        
        if(be == null) return false;
        return be.getCapability(ForgeCapabilities.ENERGY, dirToCheck).isPresent();
    }

    static boolean hasMatchingCaps(LevelReader world, BlockPos parentPos, Direction dirToCheck) {
        BlockEntity be = world.getBlockEntity(parentPos.relative(dirToCheck.getOpposite()));
        
        if(be == null) return false;
        return be.getCapability(ForgeCapabilities.ENERGY, dirToCheck).isPresent();
    }
}
