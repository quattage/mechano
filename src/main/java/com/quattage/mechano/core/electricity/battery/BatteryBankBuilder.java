package com.quattage.mechano.core.electricity.battery;

import java.util.ArrayList;

import com.quattage.mechano.core.block.orientation.relative.Relative;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;

public class BatteryBankBuilder<T extends ElectricBlockEntity> {
    
    private int capacity = 10000;
    private int maxInput = 5000;
    private int maxOutput = 5000;
    private int startEnergy = 0;

    private T target = null;
    private ArrayList<InteractionPolicy> interactions = new ArrayList<InteractionPolicy>();

    public BatteryBankBuilder() {}

    /***
     * Bind this BatteryBank to a given BlockEntity.
     * @param target BlockEntity to bind this BatteryBank to
     * @return This BatteryBankBuilder, modified to reflect this change.
     */
    public BatteryBankBuilder<T> at(T target) {
        this.target = target;
        return this;
    }

    /***
     * Sets the maximum electircity capacity (in Forge Energy units)
     * of this BatteryBank <p> Defaults to 10000.
     * @param capacity
     * @return This BatteryBankBuilder, modified to reflect this change.
     */
    public BatteryBankBuilder<T> capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /***
     * Sets the maximum input and output electricity (in FE per tick)
     * of this BatteryBank. <p> Defaults to 5000.
     * @param io FE per Tick
     * @return This BatteryBankBuilder, modified to reflect this change.
     */
    public BatteryBankBuilder<T> maxIO(int io) {
        this.maxInput = io;
        this.maxOutput = io;
        return this;
    }

    /***
     * Sets the maximum input electricity (in FE per tick)
     * of this BatteryBank. <p> Defaults to 5000.
     * @param maxInput FE per Tick
     * @return This BatteryBankBuilder, modified to reflect this change.
     */
    public BatteryBankBuilder<T> maxInput(int maxInput) {
        this.maxInput = maxInput;
        return this;
    }

    /***
     * Sets the maximum output electricity (in FE per tick)
     * of this BatteryBank. <p> Defaults to 5000.
     * @param maxInput FE per Tick
     * @return This BatteryBankBuilder, modified to reflect this change.
     */
    public BatteryBankBuilder<T> maxOutput(int maxOutput) {
        this.maxOutput = maxOutput;
        return this;
    }

    public InteractionPolicyBuilder<T> newInteraction(Relative rel) {
        return new InteractionPolicyBuilder<T>(this, rel);
    }

    public BatteryBankBuilder<T> interactsWithAllSides() {
        for(Relative rel: Relative.values())
            interactions.add(new InteractionPolicy(rel));
        return this;
    }

    public BatteryBank<T> build() {
        if(interactions == null) new BatteryBank<T>(target, null, capacity, maxInput, maxOutput, startEnergy);
        return new BatteryBank<T>(target, interactions.toArray(new InteractionPolicy[0]), capacity, maxInput, maxOutput, startEnergy);
    }

    /***
     * Add an InteractionPolicy directly. This is designed to be used
     * by the builder, you don't have to call it yourself.
     */
    protected void addInteraction(InteractionPolicy p) {
        if(p == null) return;
        interactions.add(p);
    }
}
