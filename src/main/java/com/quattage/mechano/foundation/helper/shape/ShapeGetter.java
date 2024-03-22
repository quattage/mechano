package com.quattage.mechano.foundation.helper.shape;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.quattage.mechano.Mechano;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ShapeGetter {

	protected final Level world;
	protected final int radius;
	protected final Axis axis;
	protected final BlockPos centerPos;

	// TODO immutable members
	// TODO function pass to evaluate during iteration rather than after
	protected final Map<BlockPos, BlockState> shapeBlocks;

	public ShapeGetter(Level world, int radius, Axis axis, BlockPos centerPos) {
		this.world = world;
		this.radius = radius;
		this.axis = axis;
		this.centerPos = centerPos;
		shapeBlocks = new HashMap<BlockPos, BlockState>();
	}

	public abstract ShapeGetter compute();

	public Set<Map.Entry<BlockPos, BlockState>> getBlocks() {
		return shapeBlocks.entrySet();
	}

	public static ShapeGetterBuilder ofShape(Class<? extends ShapeGetter> shape) {
		return new ShapeGetterBuilder(shape);
	}

	@SuppressWarnings("unused")
	public static class ShapeGetterBuilder {

		final Class<? extends ShapeGetter> shape;
		BlockPos pos;
		Level world;
		int radius = 1;
		Axis axis = Axis.Y;

		public ShapeGetterBuilder(Class<? extends ShapeGetter> shape) {
			this.shape = shape;
		}

		public ShapeGetterBuilder at(Level world, BlockPos pos) {
			this.world = world;
			this.pos = pos;
			return this;
		}

		public ShapeGetterBuilder withRadius(int radius) {
			this.radius = radius;
			return this;
		}

		public ShapeGetterBuilder onAxis(Axis axis) {
			this.axis = axis;
			return this;
		}

		public ShapeGetter build() {

			if(world == null) throw new IllegalStateException("Error building ShapeGetter of type " + shape.getName() + " - " + " World was not initialized!");

			try {
				return shape.getDeclaredConstructor().newInstance(world, radius, axis, pos);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {

				// TODO this is dumb
				Mechano.log("oh no :(");
				return null;
			}
		}
	}
}
