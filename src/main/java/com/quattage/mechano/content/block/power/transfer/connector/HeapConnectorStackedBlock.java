package com.quattage.mechano.content.block.power.transfer.connector;

import java.util.Locale;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.ComplexDirectionalBlock;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.quattage.mechano.registry.MechanoBlocks;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class HeapConnectorStackedBlock extends ComplexDirectionalBlock implements IBE<HeapConnectorStackedBlockEntity> {

    public static final EnumProperty<StackedTier> TIER = EnumProperty.create("tier", StackedTier.class);

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
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.FAIL;
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

        float pitch = 1f;
        float pitchMod = state.getValue(TIER).ordinal() * 0.8f;
        
        Mechano.log("pitch: " + pitchMod);

        world.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.5f, pitch * pitchMod);
        world.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.2f, pitch * pitchMod * 1.1f);
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
