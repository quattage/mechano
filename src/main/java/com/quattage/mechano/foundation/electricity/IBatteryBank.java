package com.quattage.mechano.foundation.electricity;

import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;

public interface IBatteryBank {

    void createBatteryBankDefinition(BatteryBankBuilder<? extends IBatteryBank> builder);

    /***
     * Called whenever the Energy stored within this ElectricBlockEntity is
     * changed in any way. Sending block updates and packets is handled by
     * the BatteryBank object, so you won't have to do that here.
     */
    default void onEnergyUpdated() {

    }

}
