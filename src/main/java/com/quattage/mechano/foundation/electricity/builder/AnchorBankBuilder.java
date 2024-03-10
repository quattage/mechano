package com.quattage.mechano.foundation.electricity.builder;

import java.util.ArrayList;
import java.util.HashSet;

import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.AnchorPointBank;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorTransform;
import com.quattage.mechano.foundation.electricity.power.features.GID;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * A fluent builder for AnchorPointBanks
 */
public class AnchorBankBuilder<T extends BlockEntity> {
    
    private int capacity = 10000;
    private int maxInput = 5000;
    private int maxOutput = 5000;
    private int startEnergy = 0;

    private T target = null;
    
    private final ArrayList<AnchorPoint> nodesToAdd = new ArrayList<AnchorPoint>();

    public AnchorBankBuilder() {};

    /***
     * Bind this NodeBank to a given BlockEntity.
     * @param target BlockEntity to bind this NodeBank to
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public AnchorBankBuilder<T> at(T target) {
        this.target = target;
        return this;
    }

    /***
     * Sets the maximum electircity capacity (in Forge Energy units)
     * of this NodeBank <p> Defaults to 10000.
     * @param capacity
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public AnchorBankBuilder<T> capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /***
     * Sets the maximum input and output electricity (in FE per tick)
     * of this NodeBank. <p> Defaults to 5000.
     * @param io FE per Tick
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public AnchorBankBuilder<T> maxIO(int io) {
        this.maxInput = io;
        this.maxOutput = io;
        return this;
    }

    /***
     * Sets the maximum input electricity (in FE per tick)
     * of this NodeBank. <p> Defaults to 5000.
     * @param maxInput FE per Tick
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public AnchorBankBuilder<T> maxInput(int maxInput) {
        this.maxInput = maxInput;
        return this;
    }

    /***
     * Sets the maximum output electricity (in FE per tick)
     * of this NodeBank. <p> Defaults to 5000.
     * @param maxInput FE per Tick
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public AnchorBankBuilder<T> maxOutput(int maxOutput) {
        this.maxOutput = maxOutput;
        return this;
    }

    /***
     * Sets the amount of energy that this NodeBank will hold when it is populated.
     * This is normally 0. Changing this basically just means that the EnergyStorage
     * in this NodeBank will start with some free energy when the player places it.
     * @param startEnergy FE to start with
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public AnchorBankBuilder<T> withStartingEnergy(int startEnergy) {
        if(startEnergy > capacity) startEnergy = capacity;
        this.startEnergy = startEnergy;
        return this;
    }

    /***
     * Adds a new ElectricNode to this NodeBank
     * @return
     */
    public AnchorPointBuilder newNode() {
        return new AnchorPointBuilder(this);
    }

    protected AnchorBankBuilder<T> add(AnchorTransform transform, int maxConnections) {
        return add(new AnchorPoint(transform, new GID(target.getBlockPos(), nodesToAdd.size()), maxConnections));
    }

    private AnchorBankBuilder<T> add(AnchorPoint node) {
        nodesToAdd.add(node);
        return this;
    }

    private void doCompleteCheck() {
        if(target == null) throw new IllegalStateException("NodeBank cannot be built - BlockEntity target is null. (Use .at() during construction)");
        if(nodesToAdd.isEmpty()) throw new IllegalStateException("NodeBank cannot be built - Must contain at least 1 ElectricNode (Use .newNode to add a node)");
    }

    /***
     * Finalizes all changes made to this AnchorPointBankBuilder and returns
     * a new AnchorPointBank. Use this after you've set up all your values.
     * @throws IllegalStateException If you haven't yet configured a BlockEntity target
     * (see {@link #at(BlockEntity) at()})
     * @return a NodeBank instance.
     */
    public AnchorPointBank<T> build() {
        doCompleteCheck();
        return new AnchorPointBank<T>(target, nodesToAdd);
    }
}
