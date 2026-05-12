package com.quattage.mechano.api.transmitter;

import com.quattage.mechano.grid.api.component.CircuitComponent;
import com.quattage.mechano.grid.topology.WireJack;
import com.tterrag.registrate.util.nullness.NonNullBiFunction;
import com.tterrag.registrate.util.nullness.NonnullType;

@FunctionalInterface
public interface TransmitterFactory<T extends CircuitComponent> extends NonNullBiFunction<WireJack<?>, WireJack<?>, T> {
    /**
     * Instantiates a single transmitter bound to the given <code>start</code> and <code>end</code>
     * @param start
     * @param end
     * @return
     */
    @Override
    @NonnullType T apply(@NonnullType WireJack<?> t, @NonnullType WireJack<?> u);
}
