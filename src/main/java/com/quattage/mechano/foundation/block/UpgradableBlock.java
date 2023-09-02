package com.quattage.mechano.foundation.block;

import java.util.ArrayList;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/***
 * Upgradable Blocks are blocks that hold an array of Upgradable block variants.
 * This list of upgraded/downgraded variants can be addressed to swap this block
 * in-place with its upgraded version. 
 */
public abstract class UpgradableBlock extends CombinedOrientedBlock {

    protected UpgradableBlock[] tiers;
    private Item upgradeItem;

    private final int iteration;
    private final boolean shouldDropItems;

    public UpgradableBlock(Properties properties) {
        super(properties);
        this.iteration = 0;
        this.tiers = new UpgradableBlock[0];
        this.upgradeItem = null;
        this.shouldDropItems = setShouldDropItems();
    }

    public UpgradableBlock(Properties properties, UpgradableBlock parent) {
        super(properties);
        
        this.tiers = parent.tiers;
        this.iteration = 0;
        this.upgradeItem = parent.upgradeItem;
        this.shouldDropItems = parent.shouldDropItems;
    }

    public UpgradableBlock(Properties properties, UpgradableBlock parent, int iteration) {
        super(properties);
        
        this.tiers = parent.tiers;
        this.iteration = iteration;
        this.upgradeItem = parent.upgradeItem;
        this.shouldDropItems = parent.shouldDropItems;
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        initTiers();
        super.onPlace(pState, pLevel, pPos, pOldState, pIsMoving);
    }

    public void initTiers(boolean force) {
        if(force || !hasTiers()) {
            ArrayList<UpgradableBlock> upgradesList = setUpgradeTiers(new ArrayList<UpgradableBlock>());
            this.tiers = upgradesList.toArray(new UpgradableBlock[upgradesList.size()]);
            verifyTiers();

            if(upgradeItem == null) 
                upgradeItem = setUpgradeItem();

            for(UpgradableBlock tier : tiers)
                tier.initTiers();
        }
    }

    public void initTiers() {
        initTiers(false);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState rotated = DirectionTransformer.rotate(state);

        if(!rotated.canSurvive(world, context.getClickedPos()))
			return InteractionResult.PASS;

        world.setBlock(clickedPos, rotated, 3);
        confirmWrench(state, context);

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {

        Level world = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();

        if(!downgrade(world, clickedPos, state)) 
            world.destroyBlock(clickedPos, false);

        if(!player.isCreative()) 
            player.getInventory().placeItemBackInInventory(new ItemStack(upgradeItem));
        playUpgradeSound(world, clickedPos);
        return InteractionResult.SUCCESS;
    }

    protected boolean hasTiers() {
        if(tiers == null) return false;
        if(tiers.length < 2) return false;
        return true;
    }

    /***
     * Gets the base Block of this UpgradableBlock
     */
    protected Block getBaseBlock() {
        return tiers[0];
    }

    /***
     * Gets the amount of tiers in this UpgradableBlock.
     * @return Int representing the size of the upgradeTiers array.
     */
    protected int getUpgradeCount() {
        return tiers.length;
    }

    /***
     * @return The Item required to upgrade this UpgradableBlock.
     */
    protected abstract Item setUpgradeItem();

    /***
     * @return True if this this UpgraableBlock should programatically 
     * drop all of its upgrade components when it is broken.
     */
    protected boolean setShouldDropItems() {
        return true;
    }

    /***
     * Attach Blocks to a provided ArrayList of blocks. This list represents the entire
     * hierarchy of upgradees. <p><strong> This block MUST be included in this list. </strong> <p>
     * Example:
     * <pre>
     * upgrades.add(block1);
     * upgrades.add(block2);
     * upgrades.add(block3);
     * upgrades.add(block4);
     * </pre>
     * This allows block1 to be upgraded into block2, block2 into block3, and block3 into block4.
     * This list must be stored identically within blocks 1, 2, 3, and 4.
     * @param upgrades A new ArrayList to add blocks to. Passed automatically by 
     * {@link #UpgradableBlock(Properties) UpgradableBlock's constructor}.
     * @return An ArrayList, usually just the one provided, but with UpgradableBlocks added to it.
     */
    protected abstract ArrayList<UpgradableBlock> setUpgradeTiers(ArrayList<UpgradableBlock> upgrades);
    
    protected final int getIteration() {
        for(int x = 0; x < tiers.length; x++) 
            if(tiers[x].equals(this)) return x;
        throw new IllegalArgumentException("Error instantiating UpgradableBlock '" + getName() 
            + "' - Upgrade Tiers must include this block!");
    }

    private void verifyTiers() {
        if(!hasTiers())
            throw new IllegalArgumentException("Error instantiating UpgradableBlock '" + getName() 
                + "' - Must have at least 2 upgrade tiers, but only found " + tiers.length + "!");
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        initTiers();

        if(world.isClientSide) return InteractionResult.PASS;

        ItemStack heldItem = player.getItemInHand(hand);
        if(heldItem.getItem() != upgradeItem) return InteractionResult.PASS;

        boolean upgraded = upgrade(world, pos, state);

        if(upgraded) {
            if(!player.isCreative()) subtractFromHand(player, hand, heldItem, 1);
            playUpgradeSound(world, pos);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        initTiers();
        return new ItemStack(upgradeItem);
    }

    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        initTiers();

        if(shouldDropItems && !player.isCreative()) {
            for(int x = 0; x < iteration + 1; x++)
                spawnItem(world, pos, new ItemStack(tiers[x].upgradeItem));
        }
        super.playerWillDestroy(world, pos, state, player);
    }


    public void spawnItem(Level world, BlockPos pos, ItemStack stack) {
        ItemEntity drop = new ItemEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack);
        drop.setDeltaMovement(VectorHelper.getRandomVector(0.3));
        world.addFreshEntity(drop);
    }

    /***
     * Upgrades this UpgradableBlock to its next version. If there is no
     * next version, it does nothing and returns false.
     * @param world World to operate within
     * @param pos BlockPos of this UpgradableBlock
     * @return True if the BlockState at the given position
     * was successfully changed.
     */
    protected boolean upgrade(Level world, BlockPos pos, BlockState baseState) {
        initTiers();

        UpgradableBlock destination = getUpgrade();
        if(destination == null) return false;
        destination.initTiers();

        CombinedOrientation orient = baseState.getValue(CombinedOrientedBlock.ORIENTATION);
        BlockState destinationState = destination.defaultBlockState()
            .setValue(CombinedOrientedBlock.ORIENTATION, orient);

        world.destroyBlock(pos, false);
        world.setBlock(pos, destinationState, 3);
        world.setBlocksDirty(pos, baseState, destinationState);
        

        playUpgradeSound(world, pos);

        return true;
    }

    /***
     * Downgrades this UpgradableBlock to its previous version. If there is no
     * previous version, it does nothing and returns false.
     * @param world World to operate within
     * @param pos BlockPos of this UpgradableBlock
     * @return True if the BlockState at the given position
     * was successfully changed.
     */
    protected boolean downgrade(Level world, BlockPos pos, BlockState baseState) {
        initTiers();

        UpgradableBlock destination = getDowngrade();
        if(destination == null) return false;
        destination.initTiers();

        CombinedOrientation orient = baseState.getValue(CombinedOrientedBlock.ORIENTATION);
        BlockState destinationState = destination.defaultBlockState()
            .setValue(CombinedOrientedBlock.ORIENTATION, orient);

        world.destroyBlock(pos, false);
        world.setBlock(pos, destinationState, 3);
        world.setBlocksDirty(pos, baseState, destinationState);

        playUpgradeSound(world, pos);

        return true;
    }

    /***
     * Gets the next UpgradableBlock in the chain of upgrades.
     * @return UpgradableBlock in the next index, or null if one does not exist.
     */
    @Nullable
    protected UpgradableBlock getUpgrade() {
        if(iteration >= tiers.length - 1) return null;
        return tiers[iteration + 1];
    }

    /***
     * Gets the previous UpgradableBlock in the chain of upgrades.
     * @return UpgradableBlock in the previous index, or null if one does not exist.
     */
    @Nullable
    protected UpgradableBlock getDowngrade() {
        if(iteration <= 0) return null;
        return tiers[iteration - 1];
    }

    private void subtractFromHand(Player player, InteractionHand hand, ItemStack heldItem, int count) {
        ItemStack subtracted = heldItem.split(heldItem.getCount() - count);
        player.setItemInHand(hand, subtracted);
    }


    /***
     * Pitch shifts a standard upgrade sound depending on the current 
     * UpgradableBlock iteration and plays it in the world.
     * @param world World to operate within.
     * @param pos BlockPos origin of the sound.
     */
    protected void playUpgradeSound(Level world, BlockPos pos) {
        float pitchMod = (((float)iteration + 2f) / (float)tiers.length);        
        world.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.5f, 1.2f * pitchMod);
        world.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.2f, 1);
    }

    public String getTierId() {
        return "tier" + iteration;
    }

    public Item getUpgradeItem() {
        return upgradeItem;
    }
}
