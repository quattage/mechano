package com.quattage.mechano.foundation.helper;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
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

    /***
     * When a block is clicked, this method will retieve the quadrant (triangular, offset by 45 degrees)
     * that the player clicked on and return that quadrant as a Direction relative to the orientation of
     * the face.
     * @param context BlockPlaceContext for the placement of the block
     * @param orient Direction if the clicked face
     * @param negateCenter Set to true if clicking near the middle of the block should be differentiated
     * (e.g. placing a block facing away when the middle is clicked)
     * @return
     */
    public static Direction getClickedQuadrant(BlockPlaceContext context, Direction orient, boolean negateCenter) {
        double x = 0;
        double y = 0;

        if(orient.getAxis() == Axis.Y) {
            y = context.getClickLocation().z - (double)context.getClickedPos().getZ();
            x = context.getClickLocation().x - (double)context.getClickedPos().getX();
        } else if(orient.getAxis() == Axis.Z) {
            y = context.getClickLocation().y - (double)context.getClickedPos().getY();
            x = context.getClickLocation().x - (double)context.getClickedPos().getX();
        } else if(orient.getAxis() == Axis.X) {
            y = context.getClickLocation().y - (double)context.getClickedPos().getY();
            x = context.getClickLocation().z - (double)context.getClickedPos().getZ();
        }

        double lineA = 1 * x + 0;
        double lineB = -1 * x + 1;

        if(negateCenter) {
            double cen = 0.3;
            if(x > cen && x < (1 - cen) && y > cen && y < (1 - cen))
                return orient;
        }

        // sorry
        if(orient == Direction.UP) {
            if(y <= lineA && y <= lineB) return Direction.NORTH;    // down
            if(y <= lineA && y >= lineB) return Direction.EAST;     // right
            if(y >= lineA && y >= lineB) return Direction.SOUTH;    // up
            if(y >= lineA && y <= lineB) return Direction.WEST;     // left
        } else if(orient == Direction.DOWN) {
            if(y <= lineA && y <= lineB) return Direction.NORTH;   
            if(y <= lineA && y >= lineB) return Direction.EAST; 
            if(y >= lineA && y >= lineB) return Direction.SOUTH;    
            if(y >= lineA && y <= lineB) return Direction.WEST;  
        } else if(orient == Direction.NORTH) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.EAST;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.WEST;
        } else if(orient == Direction.SOUTH) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.EAST;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.WEST;
        } else if(orient == Direction.EAST) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.SOUTH;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.NORTH;
        } else if(orient == Direction.WEST) {
            if(y <= lineA && y <= lineB) return Direction.DOWN;
            if(y <= lineA && y >= lineB) return Direction.SOUTH;
            if(y >= lineA && y >= lineB) return Direction.UP;
            if(y >= lineA && y <= lineB) return Direction.NORTH;
        }
        return Direction.UP;
    }
}