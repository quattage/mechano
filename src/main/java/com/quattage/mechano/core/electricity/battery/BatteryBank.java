package com.quattage.mechano.core.electricity.battery;

import java.util.HashSet;
import java.util.Optional;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.core.block.DirectionTransformer;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.core.electricity.DirectionalEnergyStorable;
import com.quattage.mechano.core.electricity.LocalEnergyStorage;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.network.EnergySyncS2CPacket;
import com.quattage.mechano.network.MechanoPackets;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class BatteryBank<T extends ElectricBlockEntity> implements DirectionalEnergyStorable {

    private final T target;
    @Nullable
    private final InteractionPolicy[] interactions;
    public final LocalEnergyStorage<BatteryBank<T>> battery;    
    public LazyOptional<IEnergyStorage> energyHandler = LazyOptional.empty();

    public BatteryBank(T target, InteractionPolicy[] interactions, int capacity, int maxRecieve, int maxExtract, int energy) {
        this.target = target;
        this.interactions = interactions;
        this.battery = new LocalEnergyStorage<BatteryBank<T>>(this, capacity, maxRecieve, maxExtract, energy);
    }

    /***
     * @return The world that this BatteryBank's parent 
     * BlockEntity belongs to
     */
    public Level getWorld() {
        return target.getLevel();
    }

    /***
     * Checks whether this BatteryBank is allowed to 
     * send power to the given block.
     * @param block Block to check
     * @return True if this BatteryBank can send power to
     * the given block.
     */
    public boolean canSendTo(Block block) {
        if(!canInteractDirectly()) return false;
        for(InteractionPolicy p : interactions)
            if(p.canSendTo(block)) return true;
        return false;
    }

    /***
     * Checks whether this BatteryBank is allowed to
     * recieve power from the given block.
     * @param block Block to check
     * @return True if this BatteryBank can receive
     * power from the given block.
     */
    public boolean canRecieveFrom(Block block) {
        if(!canInteractDirectly()) return false;
        for(InteractionPolicy p : interactions)
            if(p.canRecieveFrom(block)) return true;
        return false;
    }

    /***
     * @return True if this BatteryBank can send/recieve
     * directly to adjacent blocks
     */
    public boolean canInteractDirectly() {
        return interactions != null;
    }
    
    /***
     * @return True if this BatteryBank has no stored energy
     */
    public boolean isEmpty() {
        return battery.getEnergyStored() > 0;
    }

    /***
     * Compares the energy between this BatteryBank and a given
     * BatteryBank.
     * @param other BatteryBank to compare with.
     * @return True if this BatteryBank has more stored energy
     * than the given BatteryBank.
     */
    public boolean hasMoreEnergyThan(BatteryBank<?> other) {
        return this.battery.getEnergyStored() > other.battery.getEnergyStored();
    }

    /***
     * Called every time energy is added or removed from this BatteryBank.
     */
    @Override
    public void onEnergyUpdated() {
        target.setChanged();
        target.onEnergyUpdated();
        MechanoPackets.sendToAllClients(new EnergySyncS2CPacket(battery.getEnergyStored(), target.getBlockPos()));
    }

    /***
     * Overrides the energy amount in this 
     */
    @Override
    public void setEnergyStored(int energy) {
        battery.setEnergyStored(energy);
    }

    @Override
    public <R> @NotNull LazyOptional<R> getEnergyHandler() {
        return energyHandler.cast();
    }

    public <R> @NotNull LazyOptional<R> provideEnergyCapabilities(@NotNull Capability<R> cap, @Nullable Direction side) {
        return getCapabilityForSide(cap, side, getInteractionDirections());
    }

    /***
     * Initializes the energy capabilities of this BatteryBank
     */
    public void load() {
        energyHandler = LazyOptional.of(() -> battery);
    }

    /***
     * Invalidates the energy capabilities of this BatteryBank
     */
    public void invalidate() {
        energyHandler.invalidate();
    }

    public BatteryBank<T> reflectStateChange(BlockState state) {
        if(state == null) return this;
        CombinedOrientation target = DirectionTransformer.extract(state);
        for(InteractionPolicy interaction : interactions)
            interaction.rotateToFace(target);
        return this;
    }

    public CompoundTag writeTo(CompoundTag in) {
        return battery.writeTo(in);
    }

    public void readFrom(CompoundTag in) {
        battery.readFrom(in);
    }

    /***
     * Gets every direction that this NodeBank can interact with.
     * Directions are relative to the world, and will change
     * depending on the orientation of this NodeBank's parent block.
     * @return A list of Directions; empty if this NodeBank
     * has no interaction directions.
     */
    public Direction[] getInteractionDirections() {
        if(interactions == null || interactions.length == 0) return new Direction[0];

        Direction[] out = new Direction[interactions.length];
        for(int x = 0; x < interactions.length; x++) {
            InteractionPolicy p = interactions[x];
            if(p.isInput || p.isOutput)
                out[x] = interactions[x].getDirection();
        }
            
        return out;
    }

    /***
     * Differs from {@link #getInteractionDirections()} <p>
     * Returns InteractionPolicies directly, rather than 
     * summarizing the interactions as a list of Directions.
     * @return
     */
    public InteractionPolicy[] getRawInteractions() {
        return interactions;
    }
}
