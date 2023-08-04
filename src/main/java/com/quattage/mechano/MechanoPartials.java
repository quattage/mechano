package com.quattage.mechano;

import com.jozufozu.flywheel.core.PartialModel;

// This is where Partial Models get registered.
// eg. The needle on Create's Stressometer.
// CALLED BY LOGICAL CLIENT ONLY
public class MechanoPartials {
    public static final PartialModel DIAGONAL_GIRDER_SHORT_DOWN_FLAT = newPartial("simple/diagonal_girder/partials/partial_short_down_flat");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_DOWN_VERT = newPartial("simple/diagonal_girder/partials/partial_short_down_vert");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_UP_FLAT = newPartial("simple/diagonal_girder/partials/partial_short_up_flat");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_UP_VERT = newPartial("simple/diagonal_girder/partials/partial_short_up_vert");
    public static final PartialModel DIAGONAL_GIRDER_LONG_DOWN_FLAT = newPartial("simple/diagonal_girder/partials/partial_long_down_flat");
    public static final PartialModel DIAGONAL_GIRDER_LONG_DOWN_VERT = newPartial("simple/diagonal_girder/partials/partial_long_down_vert");
    public static final PartialModel DIAGONAL_GIRDER_LONG_UP_FLAT = newPartial("simple/diagonal_girder/partials/partial_long_up_flat");
    public static final PartialModel DIAGONAL_GIRDER_LONG_UP_VERT = newPartial("simple/diagonal_girder/partials/partial_long_up_vert");


    public static final PartialModel VOLTOMETER_NEEDLE = newPartial("power/transfer/voltometer/voltometer_needle");
    public static final PartialModel DEBUG_RED = newPartial("test/basic/cube");

    private static PartialModel newPartial(String path) {
        return new PartialModel(Mechano.asResource("block/" + path));
	}

    public static void register() {
        Mechano.logReg("partial models");
    }
}
