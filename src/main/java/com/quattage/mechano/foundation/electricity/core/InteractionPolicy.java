package com.quattage.mechano.foundation.electricity.core;

import java.util.ArrayList;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.foundation.electricity.core.node.NodeMode;
import com.simibubi.create.foundation.utility.Color;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public class InteractionPolicy {
    
    private final RelativeDirection dir;
    public boolean isInput;
    public boolean isOutput;

    // when false, the interactions list is a blacklist. 
    // when true, the interactions list is a whitelist
    private final boolean denyOrAllow;
    private final Block[] interactions;

    /***
     * Create a new InteractionPolicy at the given RelativeDirection.
     * @param dir RelativeDirection 
     * @param isInput True if this InteractionPolicy can accept energy from external sources
     * @param isOutput True if this InteractionPolicy can send energy to external sources
     * @param interactions An array representing a list of blocks to consider
     * @param denyOrAllow True if the interactions list is a whitelist, or false if the interactions list is a blacklist.
     */
    public InteractionPolicy(RelativeDirection dir, boolean isInput, boolean isOutput, Block[] interactions, boolean denyOrAllow) {
        this.dir = dir;
        this.isInput = isInput;
        this.isOutput = isOutput;
        this.interactions = interactions;
        this.denyOrAllow = denyOrAllow;
    }

    /***
     * Create a new InteractionPolicy at the given RelativeDirection.
     * @param dir RelativeDirection
     * @param isInput True if this InteractionPolicy can accept energy from external sources
     * @param isOutput True if this InteractionPolicy can send energy to external sources
     */
    public InteractionPolicy(RelativeDirection dir, boolean isInput, boolean isOutput) {
        this.dir = dir;
        this.isInput = isInput;
        this.isOutput = isOutput;
        this.interactions = null;
        this.denyOrAllow = false;
    }

    /***
     * Create a new InteractionPolicy at the given RelativeDirection. <p>
     * This policy has no exceptions, and will always interact.
     * @param dir
     */
    public InteractionPolicy(RelativeDirection dir) {
        this.dir = dir;
        this.isInput = true;
        this.isOutput = true;
        this.interactions = null;
        this.denyOrAllow = false;
    }

    /***
     * Create a new InteractionPolicy at the given RelativeDirection. <p>
     * This policy has no exceptions, and will always interact.
     * @param dir
     */
    public InteractionPolicy(Relative rel) {
        this.dir = new RelativeDirection(rel);
        this.isInput = true;
        this.isOutput = true;
        this.interactions = null;
        this.denyOrAllow = false;
    }

    public Direction getDirection() {
        return dir.get();
    }

    public InteractionPolicy rotateToFace(CombinedOrientation orient) {
        dir.rotate(orient);
        return this;
    }

    public boolean canRecieveFrom(Block block) {
        if(!isInput) return false;
        return isBlockAllowed(block);
    }

    public boolean canSendTo(Block block) {
        if(!isOutput) return false;
        return isBlockAllowed(block);
    }

    public boolean isBlockAllowed(Block block) {

        if(interactions == null) return true;
        boolean hasBlock = false;
        for(Block b : interactions)
            if(b.equals(block)) hasBlock = true;
        
        return denyOrAllow ? hasBlock : !hasBlock;
    }

    /***
     * Determines whether or not this InteractionPolicy
     * is interacting with any ForgeEnergy capabilities 
     * in the world.
     * @param parent BlockEntity to use as a reference for getting real-world positions
     * @return True if this InteractionPolicy is facing towards
     * a block which provides ForgeEnergy capabilities in the opposing direction
     */
    public boolean canSendOrReceive(BlockEntity parent) {
        Level world = parent.getLevel();
        if((!isInput && !isOutput) || world == null) 
            return false;

        BlockPos offset = parent.getBlockPos().relative(getDirection());
        IEnergyStorage batteryAtPos = world.getBlockEntity(offset)
            .getCapability(ForgeCapabilities.ENERGY, getDirection().getOpposite())
            .orElse(null);
        return batteryAtPos != null;
    }

    public Color getColor() {
        if(isInput && isOutput) return NodeMode.BOTH.getHighlightColor();
        if(isInput) return NodeMode.INSERT.getHighlightColor();
        if(isOutput) return NodeMode.EXTRACT.getHighlightColor();
        return NodeMode.NONE.getHighlightColor();
    }


    public boolean equals(Object other) {
        if(other instanceof InteractionPolicy ip) 
            return dir.equals(ip.dir) && 
                this.isInput == ip.isInput && 
                this.isOutput == ip.isOutput;
        return false;
    }

    public int hashCode() {
        return dir.getRaw().ordinal() + (isInput ? 10 : 11) + (isOutput ? 10 : 11) * 31;
    }
}
