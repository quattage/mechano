package com.quattage.mechano.api.grid.topology;



/**
 * Indicates that this class instantiates/caches/provides some kind of
 * {@link CircuitComponent} object for use with the Grid API.
 */
public interface CircuitProvider {
    CircuitComponent getCircuit();
}
