package com.quattage.mechano.foundation.gridapi.watt;

public interface WattStorable {
    
    /***
     * @return The amount of energy (in Joules)
     * that this energy store is currently holding
     */
    public Joule getStoredEnergy();

    /***
     * @return The capacity of this energy store
     * measured in miliamphours.
     */
    public long getEnergyCapacity();

    /***
     * @return The charge/discharge current (C Rating) of this energy store.
     */
    public float getCRating();

    /***
     * @return The voltage, represented by a decay function or 
     * constant value, that this energy store outputs throughout
     * its depletion cycle.
     */
    public VoltageReturnable getOutputVoltage();

    /***
     * @return The expected range of input voltages
     */
    public VoltageReturnable getInputVoltage();
}
