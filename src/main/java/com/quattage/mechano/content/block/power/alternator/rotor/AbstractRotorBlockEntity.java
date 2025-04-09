package com.quattage.mechano.content.block.power.alternator.rotor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlockEntity;
import com.quattage.mechano.content.block.power.alternator.stator.AbstractStatorBlock;
import com.quattage.mechano.foundation.helper.shape.CircleGetter;
import com.quattage.mechano.foundation.helper.shape.ShapeGetter;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractRotorBlockEntity extends KineticBlockEntity {

    private final ShapeGetter circle = ShapeGetter.ofShape(CircleGetter.class).withRadius(getStatorRadius()).build();
    private byte statorCount = 0;

    @Nullable
    private BlockPos controllerPos = null;

    public AbstractRotorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
        setLazyTickRate(20);
    }

    @Override
    protected void read(CompoundTag nbt, boolean clientPacket) {

        if(nbt.contains("cX")) {
            controllerPos = new BlockPos(
                nbt.getInt("cX"),
                nbt.getInt("cY"),
                nbt.getInt("cZ")
            );
        } else controllerPos = null;

        statorCount = nbt.getByte("sC");
        super.read(nbt, clientPacket);
    }

    @Override
    protected void write(CompoundTag nbt, boolean clientPacket) {

        if(controllerPos == null) {
            nbt.remove("cX");
            nbt.remove("cY");
            nbt.remove("cZ");
        } else {
            nbt.putInt("cX", controllerPos.getX());
            nbt.putInt("cY", controllerPos.getY());
            nbt.putInt("cZ", controllerPos.getZ());
        }

        nbt.putByte("sC", statorCount);

        super.write(nbt, clientPacket);
    }

    public void findConnectedStators(boolean notifyIfChanged) {

        final Set<BlockPos> visited = new HashSet<>();

        int oldCount = statorCount;
        statorCount = 0;
        circle.moveTo(getBlockPos()).setAxis(getBlockState().getValue(RotatedPillarBlock.AXIS)).evaluatePlacement(perimeterPos -> {

            if(visited.contains(perimeterPos)) return null;

            BlockState perimeterState = level.getBlockState(perimeterPos);

            if(perimeterState.getBlock() instanceof AbstractStatorBlock asb) {
                if(asb.hasRotor(level, perimeterPos, perimeterState))
                    statorCount++;
            }

            visited.add(perimeterPos);
            return null;
        });

        if(notifyIfChanged && (oldCount != statorCount)) {
            notifyUpdate();
        }
    }

    

    public void incStatorCount() {

        statorCount++;
        if(statorCount > getStatorCircumference()) {
            statorCount = (byte)getStatorCircumference();
            return;
        }

        getAndRefreshController();
        notifyUpdate();
    }

    public void decStatorCount() {
        statorCount--;
        if(statorCount < 0) {
            statorCount = 0;
            return;
        }

        getAndRefreshController();
        notifyUpdate();
    }

    private void getAndRefreshController() {

        if(hasNetwork()) {
            onSpeedChanged(speed);
            KineticNetwork parent = getOrCreateNetwork();
            parent.updateStressFor(this, calculateStressWithStators());
            parent.sync();
        }

        if(controllerPos != null && getLevel().getBlockEntity(controllerPos) instanceof SlipRingShaftBlockEntity srbe) {
            srbe.evaluateAlternatorStructure();
            srbe.onSpeedChanged(speed);
            return;
        }

        AbstractRotorBlock.searchExternally(level, worldPosition, getBlockState());
        if(controllerPos == null) return;
        if(getLevel().getBlockEntity(controllerPos) instanceof SlipRingShaftBlockEntity srbe)
            srbe.evaluateAlternatorStructure();
    }

    public abstract int getStatorCircumference();
    public abstract int getStatorRadius();
    protected abstract float getEfficiencyBonus();

    @Nullable
    protected SlipRingShaftBlockEntity getController() {
        if(controllerPos == null) return null;
        if(getLevel().getBlockEntity(controllerPos) instanceof SlipRingShaftBlockEntity srbe) 
            return srbe;
        return null;
    }

    @Nullable
    protected BlockPos getControllerPos() {
        return controllerPos;
    }

    /**
     * Sets the BlockPos of the connected SlipRingShaft. 
     * @param controllerPos BlockPos of the slip ring
     * @param update <code>TRUE</code> to automatically re-evaluate the slip ring at the given BlockPos.
     */
    public void setControllerPos(BlockPos controllerPos, boolean update) {
        this.controllerPos = controllerPos;

        if(controllerPos != null && update 
            && getLevel().getBlockEntity(controllerPos) instanceof SlipRingShaftBlockEntity srbe)
                srbe.evaluateAlternatorStructure();

        notifyUpdate();
    }

    @Override
    public float calculateStressApplied() {
		this.lastStressApplied = calculateStressWithStators();
		return lastStressApplied;
    }

    public float calculateStressWithStators() {
        return getRampedStress(getBaseImpact(), false);
    }

    protected float getBaseImpact() {
        return statorCount == 0 ? getNoStatorImpact() : getStatorImpact();
    }

    protected float getNoStatorImpact() {
        return 1.0f;
    }

    protected float getStatorImpact() {
        return 1.0f;
    }

    private float getRampedStress(float mul, boolean max) {
        float sP = (float)statorCount / (float)getStatorCircumference(); 
        if(max)
            return toNearest4((float)(0.00196f * Math.pow(262144, 1))) * mul;
        else return toNearest4((float)(0.00196f * Math.pow(262144, sP))) * mul;
    }

    private float toNearest4(float in) {
        float remainder = in % 4;
        return Math.max(1, remainder < 4 ? in - remainder : in + (4 - remainder));
    }

    public float getMaximumRotaryStress() {
        return toNearest4(calculateStressWithStators() * 256);
    }

    public float getMaximumPossibleStress() {
        return toNearest4(calculateStressWithStators() * 256);
    }

    @Override
    public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return false;
    }

    public byte getStatorCount() {
        return statorCount;
    }

    public int getMultiplier() {
        return 1;
    }

    @Nullable
    public static AbstractRotorBlockEntity get(Level world, @Nullable BlockPos pos) {

        if(pos == null) return null;
        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof AbstractRotorBlockEntity arbe) return arbe;
        if(be instanceof RotorReferable ref)
            return ref.getRotorBE();
        return null;
    }
}