package com.quattage.mechano.foundation.helper.shape;

import java.util.function.Function;

import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;

public class CircleGetter extends ShapeGetter {

    public CircleGetter(Integer radius, Axis axis, BlockPos centerPos) {
        super(radius, axis, centerPos);
    }

    @Override
    protected PlacementOffset evalSafe(Function<BlockPos, PlacementOffset> action) {
        return makeCircle(action);
    }

    private PlacementOffset makeCircle(Function<BlockPos, PlacementOffset> action) {
        int iterX = this.radius;
        int iterY = 0;
        int step = 1 - iterX;

		while(iterX >= iterY) {

            PlacementOffset current;
            
            // lol
            current = evalBlock(action, iterX, iterY);
            if(current != null) return current;

            current = evalBlock(action, -iterX, iterY);
            if(current != null) return current;

            current = evalBlock(action, iterX, -iterY);
            if(current != null) return current;

            current = evalBlock(action, -iterX, -iterY);
            if(current != null) return current;

            current = evalBlock(action, iterY, iterX);
            if(current != null) return current;

            current = evalBlock(action, -iterY, iterX);
            if(current != null) return current;

            current = evalBlock(action, iterY, -iterX);
            if(current != null) return current;

            current = evalBlock(action, -iterY, -iterX);
            if(current != null) return current;

			if(step <= 0) {
                iterY++;
                step += 2 * iterY + 1;
            } else {
                iterX--;
                step -= 2 * iterX + 1;
            }
		}

        return PlacementOffset.fail();
    }

    private PlacementOffset evalBlock(Function<BlockPos, PlacementOffset> action, int stepX, int stepY) {

		int x = this.centerPos.getX();
        int y = this.centerPos.getY();
        int z = this.centerPos.getZ();
        
        BlockPos checkPos;

        if(this.axis == Axis.X) checkPos = new BlockPos(x, y + stepX, z + stepY);
        else if(this.axis == Axis.Y) checkPos = new BlockPos(x + stepX, y, z + stepY);
        else checkPos = new BlockPos(x + stepX, y + stepY, z);

        return action.apply(checkPos);
	}
}
