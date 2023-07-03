package com.quattage.mechano.core.block;

import com.quattage.mechano.core.block.orientation.VerticalOrientation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;

public class VerticallyOrientedBlock extends Block implements IWrenchable{
    public static final EnumProperty<VerticalOrientation> ORIENTATION = EnumProperty.create("orientation", VerticalOrientation.class); 
    
    public VerticallyOrientedBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ORIENTATION, VerticalOrientation.WEST_UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction localFacing = context.getClickedFace();
        Direction localVertical = getVerticalHalf(context.getClickedPos(), context.getClickLocation());
        if(localFacing.getAxis() == Direction.Axis.Y)
            localFacing = context.getHorizontalDirection();
        if(context.getPlayer().isCrouching()) localFacing = localFacing.getOpposite();

        return this.defaultBlockState().setValue(ORIENTATION, VerticalOrientation.combine(localFacing, localVertical));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();

        BlockState rotated = state.setValue(ORIENTATION, VerticalOrientation.cycle(state.getValue(ORIENTATION)));

        if (!rotated.canSurvive(world, context.getClickedPos()))
			return InteractionResult.PASS;
        
        KineticBlockEntity.switchToBlockState(world, context.getClickedPos(), updateAfterWrenched(rotated, context));

        if (world.getBlockState(context.getClickedPos()) != state)
			playRotateSound(world, context.getClickedPos());

		return InteractionResult.SUCCESS;
    }

    private Direction getVerticalHalf(BlockPos absolutePos, Vec3 clickedPos) {
        double deviation = absolutePos.getY() - clickedPos.y;
        if(deviation >= 0.5) return Direction.UP;
        return Direction.DOWN;
    }
}
