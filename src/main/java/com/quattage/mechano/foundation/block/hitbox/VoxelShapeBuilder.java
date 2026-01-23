
package com.quattage.mechano.foundation.block.hitbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import com.google.gson.stream.MalformedJsonException;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/***
 * A fluent builder to aid in creating AABB boxes using Create's VoxelShape stuff.
 * Heavily influenced by Create Crafts & Additions' CAShapes. 
 * This class provides a series of methods that, when chained, resemble JSON model contents.
 * All measurements are in pixels, not meters.
 */ 
public class VoxelShapeBuilder {

	public static final VoxelShape CUBE = VoxelShapeBuilder.newBox(0, 0, 0, 16, 16, 16);

	private @Nullable VoxelShape cumulative;

	public static VoxelShapeBuilder start(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new VoxelShapeBuilder(VoxelShapeBuilder.newBox(x1, y1, z1, x2, y2, z2));
	}

	public static VoxelShape newBox(double x1, double y1, double z1, double x2, double y2, double z2) {
		return Block.box(x1, y1, z1, x2, y2, z2);
	}

	public static VoxelShape newBox(Vec3 v1, Vec3 v2) {
		return Block.box(
			Math.min(v1.x, v2.x), Math.min(v1.y, v2.y), Math.min(v1.z, v2.z),
			Math.max(v1.x, v2.x), Math.max(v1.y, v2.y), Math.max(v1.z, v2.z)
		);
	}

	public VoxelShapeBuilder() {
		this.cumulative = null;
	}

	public VoxelShapeBuilder(VoxelShape shape) {
		this.cumulative = shape;
	}

	public VoxelShapeBuilder addBox(double x1, double y1, double z1, double x2, double y2, double z2) {
		if(cumulative == null) this.cumulative = VoxelShapeBuilder.newBox(x1, y1, z1, x2, y2, z2);
		else this.cumulative = Shapes.join(this.cumulative, VoxelShapeBuilder.newBox(x1, y1, z1, x2, y2, z2), BooleanOp.OR);
		return this;
	}

	public VoxelShapeBuilder subtractBox(double x1, double y1, double z1, double x2, double y2, double z2) {
		if(cumulative == null) this.cumulative = VoxelShapeBuilder.newBox(x1, y1, z1, x2, y2, z2);
		else this.cumulative = Shapes.join(cumulative, VoxelShapeBuilder.newBox(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
		return this;
	}

	// copied from create (protected in voxelshaper)
	public static VoxelShape getRotatedCopy(VoxelShape shape, Vec3i rotation) {
		if(shape.isEmpty() || rotation.equals(Vec3i.ZERO))
			return shape;

		MutableObject<VoxelShape> result = new MutableObject<>(Shapes.empty());
		shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
			Vec3 v1 = new Vec3(x1, y1, z1).scale(16)
				.subtract(DirectionTransformer.MIDDLE);
			Vec3 v2 = new Vec3(x2, y2, z2).scale(16)
				.subtract(DirectionTransformer.MIDDLE);

			v1 = VecHelper.rotate(v1, (float) rotation.getX(), Axis.X);
			v1 = VecHelper.rotate(v1, (float) rotation.getY(), Axis.Y);
			v1 = VecHelper.rotate(v1, (float) rotation.getZ(), Axis.Z)
				.add(DirectionTransformer.MIDDLE);

			v2 = VecHelper.rotate(v2, (float) rotation.getX(), Axis.X);
			v2 = VecHelper.rotate(v2, (float) rotation.getY(), Axis.Y);
			v2 = VecHelper.rotate(v2, (float) rotation.getZ(), Axis.Z)
				.add(DirectionTransformer.MIDDLE);

			VoxelShape rotated = VoxelShapeBuilder.newBox(v1, v2);
			result.setValue(Shapes.join(result.getValue(), rotated, BooleanOp.OR));
		});

		return result.getValue();
	}

	public VoxelShapeBuilder optimize() {
		cumulative = cumulative.optimize();
		return this;
	}

	public boolean hasFeatures() {
		return cumulative != null && !cumulative.isEmpty();
	}

	public VoxelShape make() {
		return cumulative;
	}

	public static class ShapeAccumulator {

		private TemporaryShape[] shapes = new TemporaryShape[0];

		public void add(TemporaryShape shape) {
			if(!shape.isComplete()) {
				Mechano.LOGGER.error("Failed to add an unfinished TemporaryShape to a ShapeAccumulator!");
				return;
			}
			TemporaryShape[] copy = new TemporaryShape[shapes.length + 1];
			System.arraycopy(shapes, 0, copy, 0, shapes.length);
			copy[shapes.length] = shape;
			this.shapes = copy;
		}

		@Override
		public String toString() {
			if(isEmpty()) return "ShapeAccumulator[EMPTY]";
			String out = "";
			for(int x = 0; x < shapes.length; x++) {
				TemporaryShape shape = shapes[x];
				if(shape == null) out += "null";
				else out += shape.toString();
				if(x < shapes.length - 1) out += ", ";
			}
			return "ShapeAccumulator[" + out + "]";
		}

		public boolean isEmpty() {
			return shapes.length == 0;
		}

		public TemporaryShape getFirst() {
			return shapes[0];
		}

		public void forEachBox(BiConsumer<TemporaryShape, Boolean> cons) {
			for(int x = 0; x < shapes.length; x++) {
				TemporaryShape shape = shapes[x];
				if(shape == null || !shape.isComplete()) 
					continue;
				cons.accept(shape, x == 0);
			}
		}
	}

	/**
	 * A primitive AABB representation designed to store temporary coordinates
	 * before a VoxelShape is constructed. This class is used by the {@link HitboxProvider}
	 * to read from JSON.
	 */
	public static class TemporaryShape implements Disposable {

		private @Nullable double[] shape = new double[6];
		private Stage stage = Stage.EMPTY;

		public TemporaryShape() {}

		private TemporaryShape(double[] shape) {
			Objects.requireNonNull(shape);
			if(shape.length != 6) {
				throw new IllegalArgumentException("Couldn't instantiate TemporaryShape with bad array - " 
					+ Arrays.toString(shape) + " This array must be exactly 6 members long!");
			}
			this.stage = Stage.COMPLETE;
			this.shape = shape;
		}

		public boolean isComplete() {
			return this.stage == Stage.COMPLETE;
		}

		@SuppressWarnings("unchecked")
		public void collect(Map.Entry<String, Map<String, Object>> s) throws MalformedJsonException, IllegalStateException {
			assertNotDisposed();
			if("from".equals(s.getKey()) && this.stage == Stage.EMPTY) {
				read(0, (ArrayList<Object>)(s.getValue()));
				this.stage = Stage.HALF;
			} else if("to".equals(s.getKey()) && this.stage == Stage.HALF) {
				read(3, (ArrayList<Object>)(s.getValue()));
				this.stage = Stage.COMPLETE;
			} else if(this.stage == Stage.COMPLETE) throw new IllegalStateException(
				"This Temporary Shape has already been assembled! Additional calls to collect() aren't allowed!"
			);
		}

		public void collect(double x, double y, double z) {
			assertNotDisposed();
			if(this.stage == Stage.EMPTY) {
				this.shape[0] = x; this.shape[1] = y; this.shape[2] = z;
				this.stage = Stage.HALF;
			} else if(this.stage == Stage.HALF) {
				this.shape[3] = x; this.shape[4] = y; this.shape[5] = z;
				this.stage = Stage.COMPLETE;
			} else if(this.stage == Stage.COMPLETE) throw new IllegalStateException(
				"This TemporaryShape has already been assembled! Additional calls to collect() aren't allowed!"
			);
		}

		private void read(int adj, List<Object> list) throws MalformedJsonException {
			for(int x = 0; x < 3; x++) {
				Object o = list.get(x);
				double value = 0;
				if(o instanceof String s) {
					try {
						value = Double.valueOf(s);
					} catch (NumberFormatException e) {
						throw new MalformedJsonException("Token '" + value + "' is not a numerical value!");
					}
				} else {
					try{
						value = (double)o;
					} catch (ClassCastException e) {
						throw new MalformedJsonException("Token '" + value + "' is not a numerical value!");
					}
				}
				this.shape[x + adj]  = value;
			}
		}

		public void flushInto(VoxelShapeBuilder builder) {
			assertNotDisposed();
			if(this.stage != Stage.COMPLETE) return;
			builder.addBox(shape[0], shape[1], shape[2], shape[3], shape[4], shape[5]);
			clear();
		}

		public void flushInto(ShapeAccumulator accumulator) {
			assertNotDisposed();
			if(this.stage != Stage.COMPLETE) return;
			accumulator.add(new TemporaryShape(this.shape));
			clear();
		}

		@Override
		public void dispose() {
			this.stage = null;
			this.shape = null;
		}

		@Override
		public boolean hasBeenDisposed() {
			return this.stage == null && this.shape == null;
		}

		@Override
        public void assertNotDisposed() {
			if(this.stage == null || this.shape == null)
				throw new IllegalStateException("Couldn't perform an operation on a TemporaryShape that has already been destroyed!");
		}

		public void clear() {
			this.shape = new double[6];
			this.stage = Stage.EMPTY;
		}

		public double minX() {
			if(stage != Stage.COMPLETE)
				throw new IllegalStateException("Couldn't get minX coordinate for an unfinished shape!");
			return shape[0];
		}

		public double minY() {
			if(stage != Stage.COMPLETE)
				throw new IllegalStateException("Couldn't get minY coordinate for an unfinished shape!");
			return shape[1];
		}

		public double minZ() {
			if(stage != Stage.COMPLETE)
				throw new IllegalStateException("Couldn't get minZ coordinate for an unfinished shape!");
			return shape[2];
		}

		public double maxX() {
			if(stage != Stage.COMPLETE)
				throw new IllegalStateException("Couldn't get maxX coordinate for an unfinished shape!");
			return shape[3];
		}

		public double maxY() {
			if(stage != Stage.COMPLETE)
				throw new IllegalStateException("Couldn't get maxY coordinate for an unfinished shape!");
			return shape[4];
		}

		public double maxZ() {
			if(stage != Stage.COMPLETE)
				throw new IllegalStateException("Couldn't get maxZ coordinate for an unfinished shape!");
			return shape[5];
		}

		@Override
		public String toString() {
			if(stage == Stage.EMPTY) return "Empty ()";
			if(stage == Stage.HALF) return "Incomplete (" + shape[0] + ", " + shape[1] + ", " + shape[2] + ")";
			return "(" + shape[0] + ", " + shape[1] + ", " + shape[2] + ", " + shape[3] + ", " + shape[4] + ", " + shape[5] + ")";
		}

		private enum Stage {
			EMPTY,
			HALF,
			COMPLETE;
		}
	}
}
