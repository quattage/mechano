package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/***
 * ElectricBlockEntity provides a basic ForgeEnergy implementation with no
 * bells & whistles.
*/
public abstract class ElectroKineticBlockEntity extends KineticBlockEntity implements IBatteryBank{

    public final BatteryBank<ElectroKineticBlockEntity> batteryBank;

    public ElectroKineticBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        BatteryBankBuilder<ElectroKineticBlockEntity> init = new BatteryBankBuilder<ElectroKineticBlockEntity>().at(this);
        createBatteryBankDefinition(init);
        batteryBank = init.build();
    }

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

    public boolean isConnectedExternally() {
        return batteryBank.isConnectedExternally();
    }

    @Override
    public void onLoad() {
        reOrient();
        batteryBank.load();
        super.onLoad();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return batteryBank.provideEnergyCapabilities(cap, side);
    }

    @Override // runs on first tick
    public void initialize() {
        super.initialize();
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
