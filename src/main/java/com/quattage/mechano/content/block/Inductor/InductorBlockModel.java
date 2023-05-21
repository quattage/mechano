package com.quattage.mechano.content.block.Inductor;

import com.quattage.mechano.Mechano;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class InductorBlockModel extends AnimatedGeoModel<InductorBlockEntity> {

    private static final String NAME = "inductor";
	
	@Override
	public ResourceLocation getAnimationResource(InductorBlockEntity animatable) {
		return Mechano.asResource("animations/" + NAME + ".animation.json");
	}

	@Override
	public ResourceLocation getModelResource(InductorBlockEntity object) {
		return Mechano.asResource("geo/" + NAME + ".geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(InductorBlockEntity object) {  
		return Mechano.asResource("textures/block/" + NAME + ".png");
	}
}