package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.core.DirectionalEnergyStorable;
import com.quattage.mechano.foundation.electricity.core.ForgeEnergyJunction;
import com.quattage.mechano.foundation.electricity.core.LocalEnergyStorage;
import com.quattage.mechano.foundation.network.EnergySyncS2CPacket;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/***
 * The BatteryBank object stores a list of ForgeEnergyJunctions.
 * It manages sending, receiving, and storing ForgeEnergy and handles
 * its own capability implementation.
 */
public class BatteryBank<T extends SmartBlockEntity & IBatteryBank> implements DirectionalEnergyStorable {

    
    @Nullable
    private final ForgeEnergyJunction[] interactions;
    public final LocalEnergyStorage<BatteryBank<T>> battery;    
    private final T target;
    public LazyOptional<IEnergyStorage> energyHandler = LazyOptional.empty();

    public BatteryBank(T target, ForgeEnergyJunction[] interactions, int capacity, int maxRecieve, int maxExtract, int energy) {
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
        for(ForgeEnergyJunction p : interactions)
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
        for(ForgeEnergyJunction p : interactions)
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
     * @return True if this BatteryBank is at max energy
     */
    public boolean isFull() {
        return battery.getEnergyStored() >= battery.getMaxEnergyStored();
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
     * Overrides the energy amount in this BatteryBank
     */
    @Override
    public void setEnergyStored(int energy) {
        battery.setEnergyStored(energy);
    }

    public int getEnergyStored() {
        return battery.getEnergyStored();
    }

    public int sendEnergyTo(BlockEntity other) {
        return 1;
        // other.getCapability
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
        for(ForgeEnergyJunction interaction : interactions)
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
     * Gets every direction that this BatteryBank can interact with.
     * Directions are relative to the world, and will change
     * depending on the orientation of this BatteryBank's parent block.
     * @return A list of Directions; empty if this BatteryBank
     * has no interaction directions.
     */
    public Direction[] getInteractionDirections() {
        if(interactions == null || interactions.length == 0) return new Direction[0];

        Direction[] out = new Direction[interactions.length];
        for(int x = 0; x < interactions.length; x++) {
            ForgeEnergyJunction p = interactions[x];
            if(p.isInput || p.isOutput)
                out[x] = interactions[x].getDirection();
        }
            
        return out;
    }

    /***
     * Checks whether this BatteryBank is directly connected to any BlockEntities that have ForgeEnergy
     * capabilites.
     * @return True if this BatteryBank is interacting with a ForgeEnergy BlockEntity
     */
    public boolean isConnectedExternally() {
        for(ForgeEnergyJunction pol : interactions) {
            Mechano.log("CHECKING DIR: " + pol.getDirection());
            if(pol.canSendOrReceive(target)) return true;
        }
        return false;
    }

    /***
     * Differs from {@link #getInteractionDirections()} <p>
     * Returns InteractionPolicies directly, rather than 
     * summarizing the interactions as a list of Directions.
     * @return
     */
    public ForgeEnergyJunction[] getRawInteractions() {
        return interactions;
    }
}
