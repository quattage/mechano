package com.quattage.mechano;

import com.jozufozu.flywheel.core.PartialModel;


public class MechanoModels {
    public static final PartialModel DIAGONAL_GIRDER_SHORT_DOWN_FLAT = newPartial("diagonal_girder/partials/short_down_flat");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_DOWN_VERT = newPartial("diagonal_girder/partials/short_down_vert");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_UP_FLAT = newPartial("diagonal_girder/partials/short_up_flat");
    public static final PartialModel DIAGONAL_GIRDER_SHORT_UP_VERT = newPartial("diagonal_girder/partials/short_up_vert");
    public static final PartialModel DIAGONAL_GIRDER_LONG_DOWN_FLAT = newPartial("diagonal_girder/partials/long_down_flat");
    public static final PartialModel DIAGONAL_GIRDER_LONG_DOWN_VERT = newPartial("diagonal_girder/partials/long_down_vert");
    public static final PartialModel DIAGONAL_GIRDER_LONG_UP_FLAT = newPartial("diagonal_girder/partials/long_up_flat");
    public static final PartialModel DIAGONAL_GIRDER_LONG_UP_VERT = newPartial("diagonal_girder/partials/long_up_vert");

    public static final PartialModel VOLTOMETER_NEEDLE = newPartial("voltometer/needle");

    private static PartialModel newPartial(String path) {
        return new PartialModel(Mechano.asResource("block/" + path));
	}

    public static void register() {
        Mechano.logReg("partial models");
    }
}
