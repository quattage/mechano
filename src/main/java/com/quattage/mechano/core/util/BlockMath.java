package com.quattage.mechano.core.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BlockMath {

    public static double getSimpleDistance(BlockPos pos1, BlockPos pos2) {
        return Math.sqrt(Math.pow((pos1.getX() - pos2.getX()), 2) 
                        + Math.pow((pos1.getY() - pos2.getY()), 2) 
                        + Math.pow((pos1.getZ() - pos2.getZ()), 2));
    }

    public static BlockPos getNearestBlock(Vec3 root) {
        Vec3 pos = VectorHelper.getCenter(root);
        return VectorHelper.toBlockPos(pos);
    }

    @Nullable
    public static Direction getDirectionTo(Level world, BlockPos fromPos, BlockPos centerPos, Axis rotationAxis) {
        List<Direction> directions = getAxisDirections(rotationAxis);
		for (Direction dir : directions) {
            BlockPos checkPos = fromPos.relative(dir);
            if(checkPos.equals(centerPos)) return dir;
        }
        return null;
    }

    public static double average(BlockPos pos) {
        return (pos.getX() + pos.getY() + pos.getZ()) / 3;
    }


    public static boolean isNegative(Direction dir) {
        return dir == Direction.NORTH || 
            dir == Direction.WEST ||
            dir == Direction.DOWN;
    }

    public static List<Direction> getAxisDirections(Axis axis) {
        List<Direction> out = new ArrayList<Direction>();
        if(axis == Axis.Z) {
            out.add(Direction.DOWN);
            out.add(Direction.EAST);
            out.add(Direction.UP);
            out.add(Direction.WEST);
        } else if(axis == Axis.Y) {
            out.add(Direction.NORTH);
            out.add(Direction.EAST);
            out.add(Direction.SOUTH);
            out.add(Direction.WEST);
        } else {
            out.add(Direction.DOWN);
            out.add(Direction.NORTH);
            out.add(Direction.UP);
            out.add(Direction.SOUTH);
        }
        return out;
    }
}
