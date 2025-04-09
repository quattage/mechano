package com.quattage.mechano.content.block.power.alternator.stator;

import java.util.Locale;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoBlocks;
import com.quattage.mechano.content.block.power.alternator.rotor.BlockRotorable;
import com.quattage.mechano.foundation.block.hitbox.Hitbox;
import com.quattage.mechano.foundation.block.hitbox.HitboxNameable;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.block.orientation.SimpleOrientation;
import com.quattage.mechano.foundation.helper.CreativeTabExcludable;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BigStatorBlock extends AbstractStatorBlock<com.quattage.mechano.content.block.power.alternator.stator.BigStatorBlock.BigStatorModelType> {

    public static final EnumProperty<BigStatorModelType> MODEL_TYPE = EnumProperty.create("model", BigStatorModelType.class);
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper(2));
    private static Hitbox<SimpleOrientation> hitbox = new Hitbox<>();

    protected static enum BigStatorModelType implements StringRepresentable, HitboxNameable, StatorTypeTransformable<BigStatorModelType>, CreativeTabExcludable {
        
        BASE_SINGLE(0, 0),
        BASE_END_A(0, 1),
        BASE_END_B(0, 2),
        BASE_MIDDLE(0, 3),

        CORNER_POSITIVE_SINGLE(1, 0),
        CORNER_POSITIVE_END_A(1, 1),
        CORNER_POSITIVE_END_B(1, 2),
        CORNER_POSITIVE_MIDDLE(1, 3),

        CORNER_NEGATIVE_SINGLE(2, 0),
        CORNER_NEGATIVE_END_A(2, 1),
        CORNER_NEGATIVE_END_B(2, 2),
        CORNER_NEGATIVE_MIDDLE(2, 3);

        final byte offset;
        final byte cornerType;

        BigStatorModelType(int cornerType, int offset) {
            this.cornerType = (byte)cornerType;
            this.offset = (byte)offset;
        }

        public byte getCornerType() {
            return this.cornerType;
        }

        @Override
        public BigStatorModelType get() {
            return this;
        }

        @Override
        public String getHitboxName() {
            return toString();
        }

        protected BigStatorModelType toBase() {
            return values()[this.offset];
        }

        protected BigStatorModelType toCorner(boolean isPositive) {
            return isPositive ? toPositiveCorner() : toNegativeCorner();
        }

        protected BigStatorModelType toPositiveCorner() {
            return values()[4 + this.offset];
        }

        protected BigStatorModelType toNegativeCorner() {
            return values()[8 + this.offset];
        }

        @Override
        public BigStatorModelType[] enumValues() {
            return values();
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String toString() {
            return getSerializedName();
        }

        @Override
        public boolean shouldContnue(BigStatorModelType other) {
            return true;
        }
    }

    public BigStatorBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(hitbox.needsBuilt()) hitbox = Mechano.HITBOXES.collectAllOfType(this);
        return hitbox.get(state.getValue(getTypeProperty())).getRotated(state.getValue(ORIENTATION));
    }

    @Override
    public BlockEntry<? extends AbstractStatorBlock<BigStatorModelType>> getStatorEntry() {
        return MechanoBlocks.BIG_STATOR;
    }

    @Override
    public BlockEntry<? extends BlockRotorable> getRotorEntry() {
        return MechanoBlocks.BIG_ROTOR_DUMMY;
    }

    @Override
    public int getRadius() {
        return 2;
    }

    @Override
    protected int getPlacementHelperId() {
        return placementHelperId;
    }

    @Override
    protected EnumProperty<BigStatorModelType> getTypeProperty() {
        return MODEL_TYPE;
    }

    @Override
    protected BigStatorModelType getDefaultModelType() {
        return BigStatorModelType.BASE_SINGLE;
    }

    @Override
    protected BlockState getAlignedModelState(Level world, Axis fromAxis, BlockPos centerPos, BlockState fromState, BlockState thisState, BlockPos[] plane) {

        if(world.isClientSide()) return thisState;

        for(BlockPos visitedPos : plane) {

            BlockState visitedState = world.getBlockState(visitedPos);            
            if(!isStator(visitedState)) continue;
            SimpleOrientation orient = visitedState.getValue(ORIENTATION);
            if(orient.getOrient() != fromAxis) continue;

            int cornerStatus = DirectionTransformer.getJoinedCornerStatus(
                fromState.getValue(ORIENTATION).getCardinal(), orient.getCardinal(), fromAxis);
            
            if(cornerStatus != 0) {
                BigStatorModelType type = thisState.getValue(MODEL_TYPE).toCorner(cornerStatus < 0 ? true : false);
                return thisState.setValue(MODEL_TYPE, type);
            }
        }

        return thisState;
    }

    @Override
    protected boolean areBlocksContinuous(SimpleOrientation thisOrient, BigStatorModelType thisType,
            SimpleOrientation thatOrient, BigStatorModelType thatType) {
        return thisType.getCornerType() == thatType.getCornerType() ? thisOrient == thatOrient : false;
    }

    @Override
    public BlockPos getAttachedRotorPos(Level world, BlockPos pos, BlockState state) {
        BlockPos facing = pos.relative(state.getValue(ORIENTATION).getCardinal());
        if(world.getBlockState(facing).getBlock() instanceof BlockRotorable br)
            return br.getParentPos(world, facing, state);
        return null;
    }

    @Override
    protected UpdateAlignment getAlignmentType() {
        return UpdateAlignment.CORNERS;
    }

    //////////
    @MethodsReturnNonnullByDefault
	protected static class PlacementHelper extends StatorDirectionalHelper<BigStatorModelType> {

		protected PlacementHelper(int radius) {
			super(radius, state -> state.getBlock() instanceof BigStatorBlock, 
                state -> state.getValue(ORIENTATION), MODEL_TYPE);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof BigStatorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.BIG_STATOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
                                         BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if(offset.isSuccessful()) {
				offset.withTransform(offset.getTransform());
            }
			return offset;
		}
	}
}