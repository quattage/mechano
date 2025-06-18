package com.quattage.mechano.foundation.block.hitbox;

import javax.annotation.processing.Generated;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;

@SuppressWarnings("unused")
@Generated("com.quattage.mechano.infrastructure.datagen.HitboxDataProvider.Provider")
public class MechanoHitboxes {

	public void register(IEventBus modBus) {}

	
	public static final LazyRotatableHitbox TEST_AXIS = new LazyRotatableHitbox(
		VoxelShapeBuilder
		.start(1.0, 0.0, 1.0, 3.0, 5.0, 3.0)
		.addBox(13.0, 0.0, 13.0, 15.0, 2.0, 15.0)
		.addBox(7.0, 7.0, 7.0, 9.0, 9.0, 9.0)
		.addBox(1.0, 10.0, 13.0, 3.0, 12.0, 15.0)
		.addBox(13.0, 5.0, 1.0, 15.0, 7.0, 3.0)
		.optimize().make()
	);

	public static final LazyRotatableHitbox CONNECTOR_SINGLE = new LazyRotatableHitbox(
		VoxelShapeBuilder
		.start(2.0, 0.0, 3.0, 14.0, 4.0, 13.0)
		.addBox(5.5, 6.0, 5.5, 10.5, 16.0, 10.5)
		.addBox(4.0, 12.95, 4.0, 12.0, 14.95, 12.0)
		.addBox(4.0, 4.0, 4.0, 12.0, 9.0, 12.0)
		.addBox(4.0, 10.0, 4.0, 12.0, 12.0, 12.0)
		.optimize().make()
	);


}