package com.quattage.mechano.content.block.power.alternator.collector;

import com.quattage.mechano.content.block.power.alternator.rotor.RotorBlockEntity;
import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.electricity.ElectroKineticBlockEntity;
import com.quattage.mechano.foundation.electricity.IBatteryBank;
import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class CollectorBlockEntity extends ElectroKineticBlockEntity {

    public static final int MAX_ROTORS = 10;
    private final List<RotorBlockEntity> rotors=new ArrayList<>();
    private int statorCount;

    public CollectorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
    public void initialize() {
        super.initialize();
        updateRotorAndStatorCount();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        updateEnergyProduced();
    }

    private void updateEnergyProduced() {
        if(getSpeed()==0||statorCount==0||batteryBank.isFull())
            return;
        int energyProduced = (int) (statorCount *Math.abs(getSpeed()));
        batteryBank.battery.receiveEnergy(energyProduced,false);
        notifyUpdate();
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);
        if(clientPacket)
            updateRotorAndStatorCount();
    }

    @Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).inflate(1);
	}


    public void updateRotorAndStatorCount(){
        if(level == null)
            return;
        rotors.clear();
        Direction rotorDirection = getBlockState().getValue(CollectorBlock.FACING);
        for(int i = 1; i<MAX_ROTORS; i++){
            BlockPos rotorPos = worldPosition.relative(rotorDirection, i);
            if(level.getBlockEntity(rotorPos) instanceof RotorBlockEntity rotor){
                rotors.add(rotor);
                rotor.setCollector(this);
            }
            else
                break;
        }
        updateStatorCount();
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        //Debug info
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Lang.text("Rotor Count: "+rotors.size()).forGoggles(tooltip);
        Lang.text("Stator Count: "+statorCount).forGoggles(tooltip);
        Lang.text("Energy Produced: "+(statorCount *Math.abs(getSpeed()))+"FE/s").forGoggles(tooltip);
        Lang.text("Energy Stored: "+batteryBank.battery.getEnergyStored()).forGoggles(tooltip);
        Lang.text("Energy Capacity: "+batteryBank.battery.getMaxEnergyStored()).forGoggles(tooltip);
        return true;
    }

    private void updateStatorCount() {
        statorCount = rotors.stream().mapToInt(RotorBlockEntity::getStatorCount).sum();
    }

    @Override
    public void createBatteryBankDefinition(BatteryBankBuilder<? extends IBatteryBank> builder) {
        builder.capacity(10000)
                .newInteraction(Relative.BOTTOM).onlySendEnergy().buildInteraction();
    }
}
