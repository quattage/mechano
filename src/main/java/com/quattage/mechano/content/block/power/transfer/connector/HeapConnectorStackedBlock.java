package com.quattage.mechano.content.block.power.transfer.connector;

import java.util.Locale;

import com.mrh0.createaddition.shapes.CAShapes;
import com.quattage.mechano.core.block.ComplexDirectionalBlock;
import com.quattage.mechano.core.placement.ComplexDirection;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HeapConnectorStackedBlock extends ComplexDirectionalBlock implements IBE<HeapConnectorStackedBlockEntity> {

    public static final EnumProperty<StackedTier> TIER = EnumProperty.create("tier", StackedTier.class);
    public static final VoxelShaper ONE_SHAPE = CAShapes.shape(5.5, 0, 5.5, 10.5, 15, 10.5).forDirectional();
    public static final VoxelShaper TWO_SHAPE = CAShapes.shape(5.5, 0, 5.5, 10.5, 20, 10.5).forDirectional();
    public static final VoxelShaper THREE_SHAPE = CAShapes.shape(5.5, 0, 5.5, 10.5, 12, 10.5).forDirectional();
    public static final VoxelShaper FOUR_SHAPE = CAShapes.shape(5.5, 0, 5.5, 10.5, 19, 10.5).forDirectional();

    public enum StackedTier implements StringRepresentable {
        ONE, TWO, THREE, FOUR;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Override
        public String toString() {
            return getSerializedName();
        }

        public static StackedTier next(StackedTier tier) {
            int pos = tier.ordinal();
            pos += 1;

            if(pos >= StackedTier.values().length) {
                pos = 0;
            }

            return StackedTier.values()[pos];
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        StackedTier type = state.getValue(TIER);
        Direction stupid = state.getValue(ORIENTATION).getCardinal().getOpposite();
        if(type == StackedTier.ONE)
            return ONE_SHAPE.get(stupid);
        if(type == StackedTier.TWO)
            return TWO_SHAPE.get(stupid);
        if(type == StackedTier.THREE)
            return THREE_SHAPE.get(stupid);
        return FOUR_SHAPE.get(stupid);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
		ComplexDirection rotatedOrient = ComplexDirection.cycleOrient(state.getValue(ORIENTATION));
        BlockState rotated = state.setValue(ORIENTATION, rotatedOrient);
        KineticBlockEntity.switchToBlockState(world, context.getClickedPos(), updateAfterWrenched(rotated, context));

        if (world.getBlockState(context.getClickedPos()) != state)
			playRotateSound(world, context.getClickedPos());

		return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return new ItemStack(MechanoBlocks.HEAP_CONNECTOR.get().asItem());
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
        if (world instanceof ServerLevel) {
            if (player != null && !player.isCreative()) {
                StackedTier tier = state.getValue(TIER);
                for(int x = 0; x < tier.ordinal() + 1; x++) {
                    ItemStack itemStack = new ItemStack(MechanoBlocks.HEAP_CONNECTOR.get().asItem());
                    player.getInventory().placeItemBackInInventory(itemStack);
                }
            }
            world.destroyBlock(pos, false);
            playRemoveSound(world, pos);
        }
        return InteractionResult.SUCCESS;
    }


    public HeapConnectorStackedBlock(Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.defaultBlockState().setValue(TIER, StackedTier.ONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TIER);
    }

    public void upgradeConnector(BlockState state, Level world, BlockPos pos, BlockHitResult hit) {
        StackedTier newTier = StackedTier.next(state.getValue(TIER));
        world.setBlock(pos, state.setValue(TIER, newTier), Block.UPDATE_ALL);

        if (world.getBlockState(pos) != state)
			playUpgradeSound(world, pos, state);
    }

    private void playUpgradeSound(Level world, BlockPos pos, BlockState state) {
        float pitch = 1.2f;
        float pitchMod = state.getValue(TIER).ordinal() * 0.8f;

        world.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.5f, pitch * pitchMod);
        world.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.2f, 1);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {

        ItemStack heldItem = player.getItemInHand(hand);
        if(heldItem.getItem() == MechanoBlocks.HEAP_CONNECTOR.get().asItem()) {
            if(state.getValue(TIER).equals(StackedTier.FOUR)) return InteractionResult.CONSUME;
            if(!player.isCreative()) {
                ItemStack subtracted = heldItem.split(heldItem.getCount() - 1);
                player.setItemInHand(hand, subtracted);
            }
            upgradeConnector(state, world, pos, hit);
            return InteractionResult.SUCCESS;
        }

        return super.use(state, world, pos, player, hand, hit);
    }


    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(world, pos, state, player);
        if(!player.isCreative()) {
            dropConnectors(state, world, pos);
        }
    }


    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos,
            boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        
        Direction behind = state.getValue(ORIENTATION).getCardinal();
        if(world.getBlockState(pos.relative(behind)).getBlock() != MechanoBlocks.TRANSMISSION_NODE.get()) {
            world.destroyBlock(pos, true);
            dropConnectors(state, world, pos);
        }
    }

    // TODO drop wires as well
    public void dropConnectors(BlockState state, Level world, BlockPos pos) {
        StackedTier tier = state.getValue(TIER);
        if(tier != StackedTier.ONE) {
            for(int x = 0; x < tier.ordinal(); x++) {
                dropResources(state, world, pos);
            }
        }
    }


    @Override
    public Class<HeapConnectorStackedBlockEntity> getBlockEntityClass() {
        return HeapConnectorStackedBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HeapConnectorStackedBlockEntity> getBlockEntityType() {
        return MechanoBlockEntities.HEAP_CONNECTOR_STACKED.get();
    }
    
}
