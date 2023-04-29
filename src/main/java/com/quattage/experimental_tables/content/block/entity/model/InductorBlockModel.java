package com.quattage.experimental_tables.content.block.entity.model;

import com.quattage.experimental_tables.ExperimentalTables;
import com.quattage.experimental_tables.content.block.entity.InductorBlockEntity;

import net.minecraft.util.Identifier;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class InductorBlockModel extends AnimatedGeoModel<InductorBlockEntity> {

    private static final String NAME = "inductor";

	@Override
	public Identifier getAnimationResource(InductorBlockEntity animatable) {
		return new Identifier(ExperimentalTables.MOD_ID, "animations/" + NAME + ".animation.json");
	}

	@Override
	public Identifier getModelResource(InductorBlockEntity object) {
		return new Identifier(ExperimentalTables.MOD_ID, "geo/" + NAME + ".geo.json");
	}

	@Override
	public Identifier getTextureResource(InductorBlockEntity object) {  
        return new Identifier(ExperimentalTables.MOD_ID, "textures/" + NAME + ".png");
	}
	
}
