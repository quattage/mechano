package com.quattage.mechano.content.block.Inductor;

import software.bernie.geckolib3.renderers.geo.GeoItemRenderer;

public class InductorItemRenderer extends GeoItemRenderer<InductorBlockItem> {
    public InductorItemRenderer() {
        super(new InductorBlockItemModel());
    }
}
