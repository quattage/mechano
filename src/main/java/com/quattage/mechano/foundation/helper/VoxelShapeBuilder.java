
package com.quattage.mechano.foundation.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import com.google.gson.stream.MalformedJsonException;

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
 */ 
public class VoxelShapeBuilder {

	private @Nullable VoxelShape shape;

	public static final VoxelShape CUBE = newBox(0, 0, 0, 16, 16, 16);

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
		this.shape = null;
	}

	public VoxelShapeBuilder(VoxelShape shape) {
		this.shape = shape;
	}


	public static VoxelShapeBuilder start(double x1, double y1, double z1, double x2, double y2, double z2) {
		return new VoxelShapeBuilder(newBox(x1, y1, z1, x2, y2, z2));
	}

	public VoxelShapeBuilder addBox(double x1, double y1, double z1, double x2, double y2, double z2) {
		if(shape == null) {
			this.shape = newBox(x1, y1, z1, x2, y2, z2);
		} else 
			this.shape = Shapes.join(this.shape, newBox(x1, y1, z1, x2, y2, z2), BooleanOp.OR);
		return this;
	}

	public VoxelShapeBuilder subtractBox(double x1, double y1, double z1, double x2, double y2, double z2) {
		this.shape = Shapes.join(shape, newBox(x1, y1, z1, x2, y2, z2), BooleanOp.ONLY_FIRST);
		return this;
	}

	// copied from create (protected in voxelshaper)
	public static VoxelShape getRotatedCopy(VoxelShape shape, Vec3i rotation) {

		if(shape.isEmpty() || rotation.equals(Vec3i.ZERO))
			return shape;

		MutableObject<VoxelShape> result = new MutableObject<>(Shapes.empty());
		Vec3 center = new Vec3(8, 8, 8);

		shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
			Vec3 v1 = new Vec3(x1, y1, z1).scale(16)
				.subtract(center);
			Vec3 v2 = new Vec3(x2, y2, z2).scale(16)
				.subtract(center);
			
			v1 = VecHelper.rotate(v1, (float) rotation.getX() * 90, Axis.X);
			v1 = VecHelper.rotate(v1, (float) rotation.getY() * 90, Axis.Y);
			v1 = VecHelper.rotate(v1, (float) rotation.getZ() * 90, Axis.Z)
				.add(center);

			v2 = VecHelper.rotate(v2, (float) rotation.getX() * 90, Axis.X);
			v2 = VecHelper.rotate(v2, (float) rotation.getY() * 90, Axis.Y);
			v2 = VecHelper.rotate(v2, (float) rotation.getZ() * 90, Axis.Z)
				.add(center);

			VoxelShape rotated = newBox(v1, v2);
			result.setValue(Shapes.join(result.getValue(), rotated, BooleanOp.OR));
		});

		return result.getValue();
	}

	public void optimize() {
		shape = shape.optimize();
	}

	public boolean hasFeatures() {
		return shape != null && !shape.isEmpty();
	}

	public VoxelShape make() {
		return shape;
	}

	public static class ShapeAccumulator {
		
		private TemporaryShape[] shapes = new TemporaryShape[0];

		public void add(TemporaryShape shape) {
			TemporaryShape[] copy = new TemporaryShape[shapes.length + 1];
			System.arraycopy(shapes, 0, copy, 0, shapes.length);
			copy[shapes.length] = shape;
			this.shapes = copy;
		}

		@Override
		public String toString() {
			if(shapes.length == 0) return "ShapeAccumulator[EMPTY]";
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
	}

	public static class TemporaryShape {

		private double[] shape = new double[6];
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
		public void collect(Map.Entry<String, Map<String, Object>> s) throws MalformedJsonException {
			if(s.getKey().equals("from") && this.stage == Stage.EMPTY) {
				read(0, (ArrayList<Object>)(s.getValue()));
				this.stage = Stage.HALF;
			} else if(s.getKey().equals("to") && this.stage == Stage.HALF) {
				read(3, (ArrayList<Object>)(s.getValue()));
				this.stage = Stage.COMPLETE;
			} else if(this.stage == Stage.COMPLETE) throw new IllegalStateException(
				"This Temporary Shape has already been assembled! Additional calls to accept() aren't allowed!"
			);
		}

		private void read(int adj, List<Object> list) throws MalformedJsonException {
			for(int x = 0; x < 3; x++) {
				Object o = list.get(x);
				double value = 0;
				if(o instanceof String s) {
					try {
						value = Double.valueOf(s);
					} catch(NumberFormatException e) {
						throw new MalformedJsonException("Token '" + value + "' is not a numerical value!");
					}
				} else {
					try{
						value = (double)o;
					} catch(ClassCastException e) {
						throw new MalformedJsonException("Token '" + value + "' is not a numerical value!");
					}
				}
				this.shape[x + adj]  = value;
			}
		}

		public void flushInto(VoxelShapeBuilder builder) {
			if(this.stage != Stage.COMPLETE) return;
			builder.addBox(shape[0], shape[1], shape[2], shape[3], shape[4], shape[5]);
			this.stage = Stage.EMPTY;
			this.shape = new double[6];
		}

		public void flushInto(ShapeAccumulator accumulator) {
			if(this.stage != Stage.COMPLETE) return;
			accumulator.add(new TemporaryShape(this.shape));
			this.stage = Stage.EMPTY;
			this.shape = new double[6];
		}


		@Override
		public String toString() {
			if(stage == Stage.EMPTY) return "Empty ()";
			if(stage == Stage.HALF) return "Incomplete (" + shape[0] + ", " + shape[1] + ", " + shape[2] + ")";
			return "(" + shape[0] + ", " + shape[1] + ", " + shape[2] + ", " + shape[3] + ", " + shape[4] + ", " + shape[5] + ")";
		}

		private static enum Stage {
			EMPTY,
			HALF,
			COMPLETE;
		}
	}
}
