package com.quattage.mechano.foundation.helper;

import org.apache.commons.lang3.mutable.MutableObject;

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

	VoxelShape shape;

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
	public VoxelShape getRotatedCopy(Vec3i rotation) {

		if ((!hasFeatures()) ||rotation.equals(Vec3i.ZERO))
			return this.shape;

		MutableObject<VoxelShape> result = new MutableObject<>(Shapes.empty());
		Vec3 center = new Vec3(8, 8, 8);

		this.shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
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

	public VoxelShape getShape() {
		return shape;
	}

}
