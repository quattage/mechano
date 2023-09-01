package com.quattage.mechano.core.electricity.battery;

import java.util.ArrayList;

import com.quattage.mechano.core.block.orientation.relative.Relative;
import com.quattage.mechano.core.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;

import net.minecraft.world.level.block.Block;

public class InteractionPolicyBuilder<T extends ElectricBlockEntity> {

    private boolean isInput = true;
    private boolean isOutput = true;
    

    private boolean denyOrAllow = false;
    private Block[] blocksList = null;

    private final BatteryBankBuilder<T> base;
    private final Relative relative;

    

    public InteractionPolicyBuilder(BatteryBankBuilder<T> base, Relative relative) {
        this.base = base;
        this.relative = relative;
    }

    /***
     * This interaction will only send energy to adjacent blocks
     * @return This InteractionPolicyBuilder for chaining
     */
    public InteractionPolicyBuilder<T> onlySendEnergy() {
        isInput = false;
        isOutput = true;
        return this;
    }

    /***
     * This interaction will only recieve energy from adjacent blocks
     * @return This InteractionPolicyBuilder for chaining
     */
    public InteractionPolicyBuilder<T> onlyRecieveEnergy() {
        isInput = true;
        isOutput = false;
        return this;
    }

    public InteractionPolicyBuilder<T> sendsAndReceivesEnergy() {
        isInput = true;
        isOutput = true;
        return this;
    }

    /***
     * Blocks that are defined within the returned fluent substructure will be whitelisted
     * to the resulting InteractionPolicy.
     * @return A new BlockListBuilder within this InteractionPolicyBuilder.
     */
    public BlockListBuilder<T> onlyInteractsWith() {
        denyOrAllow = true;
        return new BlockListBuilder<T>(this);
    }

    /***
     * Blocks that are defined within this fluent substructure will be blacklisted
     * from the resulting InteractionPolicy.
     * @return A new BlockListBuilder within this InteractionPolicyBuilder.
     */
    public BlockListBuilder<T> interactsWithAllExcept() {
        denyOrAllow = false;
        return new BlockListBuilder<T>(this);
    }

    

    public BatteryBankBuilder<T> buildInteraction() {
        base.addInteraction(new InteractionPolicy(new RelativeDirection(relative), isInput, isOutput, blocksList, denyOrAllow));
        return base;
    }

    public class BlockListBuilder<R extends ElectricBlockEntity> {

        private final InteractionPolicyBuilder<R> base;
        private final ArrayList<Block> blocksList = new ArrayList<Block>();
    
        public BlockListBuilder(InteractionPolicyBuilder<R> base) {
            this.base = base;
        }
    
        /***
         * Adds a block to this BlockListBuilder
         * @param block
         * @return This BlockListBuilder for chaining
         */
        public BlockListBuilder<R> block(Block block) {
            blocksList.add(block);
            return this;
        }
    
        public InteractionPolicyBuilder<R> confirm() {
            base.blocksList = blocksList.toArray(new Block[0]);
            return base;
        }
    }
}


