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

public class SmallStatorBlock extends AbstractStatorBlock<com.quattage.mechano.content.block.power.alternator.stator.SmallStatorBlock.SmallStatorModelType> {

    public static final EnumProperty<SmallStatorModelType> MODEL_TYPE = EnumProperty.create("model", SmallStatorModelType.class);
    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper(1));
    private static Hitbox<SimpleOrientation> hitbox = new Hitbox<>();

    protected static enum SmallStatorModelType implements HitboxNameable, StringRepresentable, StatorTypeTransformable<SmallStatorModelType> {
        
        BASE_SINGLE(false),
        BASE_END_A(false),      
        BASE_END_B(false),      
        BASE_MIDDLE(false),     

        CORNER_SINGLE(true),
        CORNER_END_A(true),     
        CORNER_END_B(true),     
        CORNER_MIDDLE(true);

        final boolean isCorner;

        SmallStatorModelType(boolean isCorner) {
            this.isCorner = isCorner;
        }

        @Override
        public SmallStatorModelType get() {
            return this;
        }

        @Override
        public String getHitboxName() {
            return toString();
        }

        protected SmallStatorModelType toCorner() {
            return enumValues()[((get().ordinal() + 4) % (enumValues().length))];
        }

        public boolean isCorner() {
            return this.isCorner;
        }

        @Override
        public SmallStatorModelType[] enumValues() {
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
        public boolean shouldContnue(SmallStatorModelType other) {
            return this.isCorner() == other.isCorner();
        }

        
    }

    public SmallStatorBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(hitbox.needsBuilt()) hitbox = Mechano.HITBOXES.collectAllOfType(this);
        return hitbox.get(state.getValue(getTypeProperty())).getRotated(state.getValue(ORIENTATION));
    }

    @Override
    public BlockEntry<? extends AbstractStatorBlock<SmallStatorModelType>> getStatorEntry() {
        return MechanoBlocks.SMALL_STATOR;
    }

    @Override
    public BlockEntry<? extends BlockRotorable> getRotorEntry() {
        return MechanoBlocks.SMALL_ROTOR;
    }

    @Override
    public int getRadius() {
        return 1;
    }

    @Override
    protected int getPlacementHelperId() {
        return placementHelperId;
    }

    @Override
    protected EnumProperty<SmallStatorModelType> getTypeProperty() {
        return MODEL_TYPE;
    }

    @Override
    protected SmallStatorModelType getDefaultModelType() {
        return SmallStatorModelType.BASE_SINGLE;
    }

    @Override
    protected BlockState getAlignedModelState(Level world, Axis fromAxis, BlockPos centerPos, BlockState fromState, BlockState thisState, BlockPos[] plane) {

        BlockState aState = world.getBlockState(plane[0]);
        BlockState bState = world.getBlockState(plane[1]);
        if(isStator(aState) && isStator(bState)) {
            SimpleOrientation orient = bState.getValue(ORIENTATION);
            if(fromAxis == aState.getValue(ORIENTATION).getOrient() && fromAxis == orient.getOrient())  {
                thisState = thisState.setValue(ORIENTATION, bState.getValue(ORIENTATION));
                return thisState.setValue(MODEL_TYPE, thisState.getValue(MODEL_TYPE).toCorner());
            }
        }

        BlockState cState = world.getBlockState(plane[2]);
        if(isStator(bState) && isStator(cState)) {
            SimpleOrientation orient = cState.getValue(ORIENTATION);
            if(fromAxis == bState.getValue(ORIENTATION).getOrient() && fromAxis == orient.getOrient()) {
                thisState = thisState.setValue(ORIENTATION, orient);
                return thisState.setValue(MODEL_TYPE, thisState.getValue(MODEL_TYPE).toCorner());
            }
        }

        BlockState dState = world.getBlockState(plane[3]);
        if(isStator(cState) && isStator(dState)) {
            SimpleOrientation orient = dState.getValue(ORIENTATION);
            if(fromAxis == cState.getValue(ORIENTATION).getOrient() && fromAxis == orient.getOrient()) {
                thisState = thisState.setValue(ORIENTATION, orient);
                return thisState.setValue(MODEL_TYPE, thisState.getValue(MODEL_TYPE).toCorner());
            }
        }

        if(isStator(dState) && isStator(aState)) {
            SimpleOrientation orient = aState.getValue(ORIENTATION);
            if(fromAxis == dState.getValue(ORIENTATION).getOrient() && fromAxis == orient.getOrient()) {
                thisState = thisState.setValue(ORIENTATION, orient);
                return thisState.setValue(MODEL_TYPE, thisState.getValue(MODEL_TYPE).toCorner());
            }
        }
        

        return thisState;
    }

    @Override
    protected boolean areBlocksContinuous(SimpleOrientation thisOrient, SmallStatorModelType thisType,
            SimpleOrientation thatOrient, SmallStatorModelType thatType) {
        return thisType.isCorner() == thatType.isCorner() ? thisOrient == thatOrient : false;
    }

    @Override
    public BlockPos getAttachedRotorPos(Level world, BlockPos pos, BlockState state) {

        if(state.getValue(MODEL_TYPE).isCorner()) {
            SimpleOrientation orient = state.getValue(ORIENTATION);
            return pos.relative(orient.getCardinal())
                .relative(DirectionTransformer.getComplementingDirection(orient.getCardinal(), orient.getOrient()));
        }  
        
        return pos.relative(state.getValue(ORIENTATION).getCardinal());
    }

    @Override
    protected UpdateAlignment getAlignmentType() {
        return UpdateAlignment.ADJACENT;
    }

    //////////
    @MethodsReturnNonnullByDefault
	protected static class PlacementHelper extends StatorDirectionalHelper<SmallStatorModelType> {

		protected PlacementHelper(int radius) {
			super(radius, state -> state.getBlock() instanceof SmallStatorBlock, 
                state -> state.getValue(ORIENTATION), MODEL_TYPE);
		}

		@Override
		public Predicate<ItemStack> getItemPredicate() {
			return i -> i.getItem() instanceof BlockItem
				&& ((BlockItem) i.getItem()).getBlock() instanceof SmallStatorBlock;
		}

		@Override
		public Predicate<BlockState> getStatePredicate() {
			return Predicates.or(MechanoBlocks.SMALL_STATOR::has);
		}

		@Override
		public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos,
                                         BlockHitResult ray) {
			PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
			if (offset.isSuccessful()) {
				offset.withTransform(offset.getTransform());
            }
			return offset;
		}
	}
}