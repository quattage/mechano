package com.quattage.mechano.core.electricity.node.base;

import com.quattage.mechano.content.item.spool.WireSpool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A class representing a node connected to a {@link IWireNode}.
 */
public class NodeConnection {

	public static final String CONNECTIONS = "connections";
	public static final String ID = "id";
	public static final String OTHER = "other";
	public static final String TYPE = "spoolType";
	public static final String X = "x";
	public static final String Y = "y";
	public static final String Z = "z";

	private final BlockEntity entity;

	private final int index;
	private final int otherIndex;
	private final WireSpool spoolType;
	private Vec3i relativePos;
	private boolean invalid = false;

	public NodeConnection(BlockEntity entity, int index, int other, WireSpool spoolType, BlockPos position) {
		this.entity = entity;
		this.index = index;
		this.otherIndex = other;
		this.spoolType = spoolType;
		this.relativePos = position.subtract(entity.getBlockPos());
	}

	public NodeConnection(BlockEntity entity, CompoundTag tag) {
		this.entity = entity;
		this.index = tag.getInt(ID);
		this.otherIndex = tag.getInt(OTHER);
		this.spoolType = WireSpool.get(tag.getString(TYPE));
		this.relativePos = new Vec3i(tag.getInt(X), tag.getInt(Y), tag.getInt(Z));
	}

	public void write(CompoundTag tag) {
		tag.putInt(ID, this.index);
		tag.putInt(OTHER, this.otherIndex);
		tag.putString(TYPE, this.spoolType.getId());
		tag.putInt(X, this.relativePos.getX());
		tag.putInt(Y, this.relativePos.getY());
		tag.putInt(Z, this.relativePos.getZ());
	}

	public void updateRelative(NodeRotation rotation) {
		this.relativePos = rotation.updateRelative(this.relativePos);
	}

	public int getIndex() {
		return index;
	}

	public int getOtherIndex() {
		return otherIndex;
	}

	public WireSpool getConnectionSpool() {
		return spoolType;
	}

	public Vec3i getRelativePos() {
		return this.relativePos;
	}

	public BlockPos getPos() {
		return entity.getBlockPos().offset(this.relativePos);
	}

	public boolean isInvalid() {
		return invalid;
	}

	public void invalid() {
		this.invalid = true;
	}
}
