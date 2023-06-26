package com.quattage.mechano.registry;

import com.jozufozu.flywheel.core.PartialModel;
import com.quattage.mechano.Mechano;

// This is where Partial Models get registered.
// eg. The needle on Create's Stressometer.
// CALLED BY LOGICAL CLIENT ONLY
public class MechanoPartials {
    public static final PartialModel DIAGONAL_GIRDER_SHORT_DOWN_FLAT = makePartialBlock("simple/diagonal_girder/partials/partial_short_down_flat");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_DOWN_VERT = makePartialBlock("simple/diagonal_girder/partials/partial_short_down_vert");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_UP_FLAT = makePartialBlock("simple/diagonal_girder/partials/partial_short_up_flat");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_UP_VERT = makePartialBlock("simple/diagonal_girder/partials/partial_short_up_vert");
    public static final PartialModel DIAGONAL_GIRDER_LONG_DOWN_FLAT = makePartialBlock("simple/diagonal_girder/partials/partial_long_down_flat");
    public static final PartialModel DIAGONAL_GIRDER_LONG_DOWN_VERT = makePartialBlock("simple/diagonal_girder/partials/partial_long_down_vert");
    public static final PartialModel DIAGONAL_GIRDER_LONG_UP_FLAT = makePartialBlock("simple/diagonal_girder/partials/partial_long_up_flat");
    public static final PartialModel DIAGONAL_GIRDER_LONG_UP_VERT = makePartialBlock("simple/diagonal_girder/partials/partial_long_up_vert");

    public static final PartialModel VOLTOMETER_NEEDLE = makePartialBlock("power/transfer/voltometer/voltometer_needle");

    private static PartialModel makePartialBlock(String path) {
        return new PartialModel(Mechano.asResource("block/" + path));
	}

    public static void register() {
        Mechano.logReg("partial models");
    }
}
