package com.quattage.mechano.foundation.helper.shape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CircleGetter extends ShapeGetter {

    public CircleGetter(Level world, int radius, Axis axis, BlockPos centerPos) {
        super(world, radius, axis, centerPos);
    }


    // all of this is completely untested and i wrote it at 3am good luck

    @Override
    public ShapeGetter compute() {
        // if(radius <= 1) {}
        makeCircle();
        return this;
    }

    private void makeCircle() {
        int iterX = this.radius;
        int iterY = 0;
        int step = 0;

		while(iterX >= iterY) {

            addBlock(iterX, iterY);
            addBlock(iterY, iterX);
            addBlock(-iterY, iterX);
            addBlock(-iterX, iterY);
            addBlock(-iterX, -iterY);
            addBlock(-iterY, -iterX);
            addBlock(iterY, -iterX);
            addBlock(iterX, -iterY);
			
			if(step <= 0) {
                iterY++;
                step += 2 * iterY + 1;
            } else {
                iterX--;
                step -= 2 * iterX + 1;
            }
		}
    }

    private void addBlock(int stepX, int stepY) {

		int x = this.centerPos.getX();
        int y = this.centerPos.getY();
        int z = this.centerPos.getZ();

        BlockPos checkPos;
        if(this.axis == Axis.X) checkPos = new BlockPos(x, y + stepX, z + stepY);
        else if(this.axis == Axis.Y) checkPos = new BlockPos(x + stepX, y, z + stepY); 
        else checkPos = new BlockPos(x + stepX, y + stepY, z);

        BlockState state = this.world.getBlockState(checkPos);
        shapeBlocks.put(checkPos, state);
	}
}
