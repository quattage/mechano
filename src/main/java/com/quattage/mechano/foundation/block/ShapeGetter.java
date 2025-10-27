package com.quattage.mechano.foundation.block;

import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.function.Function;

import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;

public abstract class ShapeGetter {

	protected final int radius;
	protected Axis axis;
	protected BlockPos centerPos;
	protected boolean isInitialized = true;

	public ShapeGetter(Integer radius, Axis axis, BlockPos centerPos) {
		this.radius = radius;
		this.axis = axis;
		this.centerPos = centerPos;
	}

	/***
	 * Evaluates this ShapeGetter iteratively
	 * @param action Action to perform at the given BlockPos. Implementations should return a null PlacementOffset to indicate a skipped iteration.
	 * @return A PlacementOffset at this shape
	 */
	public PlacementOffset evaluatePlacement(Function<BlockPos, PlacementOffset> action) {
		if(!isInitialized) throw new IllegalStateException("Cannot evaluate ShapeGetter before it is initialized!");
		return evalSafe(action);
	}

	public void iteratePlacement(Consumer<BlockPos> action) {
		
	}

	protected abstract PlacementOffset evalSafe(Function<BlockPos, PlacementOffset> action);

	public static ShapeGetterBuilder ofShape(Class<? extends ShapeGetter> shape) {
		return new ShapeGetterBuilder(shape);
	}

	public ShapeGetter setAxis(Axis axis) {
		this.axis = axis;
		return this;
	}

	public ShapeGetter moveTo(BlockPos centerPos) {
		this.centerPos = centerPos;
		return this;
	}

	public static class ShapeGetterBuilder {

		final Class<? extends ShapeGetter> shape;
		BlockPos pos = BlockPos.ZERO;
		int radius = 1;
		Axis axis = Axis.Y;

		public ShapeGetterBuilder(Class<? extends ShapeGetter> shape) {
			this.shape = shape;
		}

		public ShapeGetterBuilder at(BlockPos pos) {
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
			try {
				Class<?>[] args = new Class[3];
				args[0] = Integer.class;
				args[1] = Axis.class;
				args[2] = BlockPos.class;

				return shape.getDeclaredConstructor(args).newInstance(radius, axis, pos);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {

				// TODO this is dumb
				e.printStackTrace();
				return null;
			}
		}
	}
}
