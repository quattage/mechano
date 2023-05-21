package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;

// This is where Partial Models get registered.
// eg. The needle on Create's Stressometer.
// CALLED BY LOGICAL CLIENT ONLY
public class MechanoPartials {
    public static void register() {
        Mechano.logReg("partial models");
    }
}
