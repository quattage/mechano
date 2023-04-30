package com.quattage.mechano.content.block.Inductor;

import com.quattage.mechano.Mechano;

import net.minecraft.util.Identifier;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class InductorBlockItemModel extends AnimatedGeoModel<InductorBlockItem> {

    private static final String NAME = "inductor";

	@Override
	public Identifier getAnimationResource(InductorBlockItem animatable) {
		return Mechano.newResource("animations/" + NAME + ".animation.json");
	}

	@Override
	public Identifier getModelResource(InductorBlockItem object) {
		return Mechano.newResource("geo/" + NAME + ".geo.json");
	}

	@Override
	public Identifier getTextureResource(InductorBlockItem object) {  
		return Mechano.newResource("textures/block/" + NAME + ".png");
	}
}
