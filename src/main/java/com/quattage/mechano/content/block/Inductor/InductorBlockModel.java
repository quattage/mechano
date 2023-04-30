package com.quattage.mechano.content.block.Inductor;

import com.quattage.mechano.Mechano;

import net.minecraft.util.Identifier;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class InductorBlockModel extends AnimatedGeoModel<InductorBlockEntity> {

    private static final String NAME = "inductor";
	
	@Override
	public Identifier getAnimationResource(InductorBlockEntity animatable) {
		return Mechano.newResource("animations/" + NAME + ".animation.json");
	}

	@Override
	public Identifier getModelResource(InductorBlockEntity object) {
		return Mechano.newResource("geo/" + NAME + ".geo.json");
	}

	@Override
	public Identifier getTextureResource(InductorBlockEntity object) {  
		return Mechano.newResource("textures/block/" + NAME + ".png");
	}
}
