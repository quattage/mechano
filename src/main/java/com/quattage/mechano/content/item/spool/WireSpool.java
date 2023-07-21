package com.quattage.mechano.content.item.spool;

import java.util.HashMap;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.electricity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.node.NodeBank;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.electricity.node.connection.FakeNodeConnection;
import com.quattage.mechano.core.electricity.node.connection.NodeConnectResult;
import com.quattage.mechano.core.electricity.node.connection.NodeConnection;
import com.quattage.mechano.registry.MechanoItems;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public abstract class WireSpool extends Item {

    protected final String PREFIX = "wire_";
    protected final int rate;
    protected final String name;
    protected final ItemStack emptySpoolDrop;
    protected final ItemStack rawDrop;
    public static final HashMap<String, WireSpool> spoolTypes = new HashMap<String, WireSpool>();

    private Pair<NodeConnectResult, FakeNodeConnection> intermediary;
    private Player player;
    private ElectricBlockEntity target;

    /***
     * Create a new WireSpool object
     * @param properties ItemProperties
     * @param name String ID of this spool. Simple names (ex. "hookup")
     * @param rate Transfer rate (in FE per Tick) of this WireSpool's cooresponding wire type
     * @param base
     */
    public WireSpool(Properties properties) {
        super(properties);
        this.name = setName().toLowerCase();
        this.rate = setRate();
        this.emptySpoolDrop = new ItemStack(setEmptySpoolDrop());
        this.rawDrop = new ItemStack(setRawDrop());
        WireSpool.addType(this);
    }

    /***
     * Add a WireSpool object to the WireSpool cache. 
     * The stored WireSpool instance is then used by {@link #get() get()} 
     * to look up WireSpool types by ID when said ID retrieved from NBT.
     * <strong>Note: You don't have to do this yourself. It's done automatically
     * by the WireSpool's  {@link #WireSpool(Properties properties) constructor}.</strong>
     * @param spool WireSpool instance to cache.
     */
    public static final void addType(WireSpool spool) {
        spoolTypes.put(spool.getId(), spool);
    }

    /***
     * Retrieve a WireSpool object from the cache. This is used 
     * @param id ID to retrieve (ex. 'wire_hookup' or 'wire_transmission')
     */
    public static final WireSpool get(String id) {
        return spoolTypes.get(id);
    }

    /***
     * The name of this Spool's wire. This is used as the ID of this spool. 
     * ex. a name of "hookup" results in an ID of "wire_hookup"
     * This is used to look up the wire's texture.
     * @return
     */
    protected abstract String setName();


    /***
     * The energy transfer rate of this WireSpool's cooresponding wire type.
     * @return Int in FE per tick. 
     */
    protected abstract int setRate();

    /***
     * The Raw item that cooresponds to this WireSpool. This is usually just a basic wire item, which is
     * applied to the spool through some process. Note that you can set this drop to be the spool itself.
     * @return the Item instance of the desired raw item. Obtained through registry.
     */
    protected abstract Item setRawDrop();

    /***
     * The"Empty" version of this WireSpool, which is given to the player when they run out of wire.
     * Normally, this would just be an empty spool.
     * Override this if you want additional behavior or a custom EmptySpool object.
     * @return the Item instance of the desired empty spool. Obtained through registry.
     */
    protected Item setEmptySpoolDrop() {
        return MechanoItems.EMPTY_SPOOL.get();
    }

    public final String getId() {
        return PREFIX + name;
    }

    public final ItemStack getEmptySpool() {
        return emptySpoolDrop;
    }
    
    public final ItemStack getRawDrop() {
        return rawDrop;
    }

    public final int getRate() {
        return rate;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Vec3 clickedLoc = context.getClickLocation();
        BlockEntity be = world.getBlockEntity(clickedPos);
        player = context.getPlayer();

        ItemStack handStack = context.getItemInHand();
        
        if(be != null && be instanceof ElectricBlockEntity ebe) {

            target = ebe;
            if(handStack.hasTag()) {
                if(handStack.getTag().contains("at") && handStack.getTag().contains("from"))
                    return handleTo(world, handStack, clickedPos, clickedLoc);
            }
            return handleFrom(world, handStack, clickedPos, clickedLoc, context.getPlayer());
        } 
            
        return InteractionResult.PASS;
    }

    private InteractionResult handleFrom(Level world, ItemStack wireStack,
        BlockPos clickedPos, Vec3 clickedLoc, Entity placer) {

        Pair<ElectricNode, Double> clicked = target.nodes.getClosest(clickedLoc);
        if(clicked == null) return InteractionResult.PASS;

        double distance = clicked.getSecond().doubleValue();
        ElectricNode node = clicked.getFirst();

        if(distance > node.getHitSize() * 1.45) return InteractionResult.PASS;

        CompoundTag nbt = wireStack.getOrCreateTag();   
        nbt.put("at", writePos(clickedPos));
        nbt.putString("from", node.getId());

        intermediary = target.nodes.makeFakeConnection(this, node.getId(), placer);

        if(!intermediary.getFirst().isSuccessful())
            revert(wireStack, false);

        sendInfo(world, clickedPos, intermediary.getFirst());

        return InteractionResult.FAIL;
    }

    private InteractionResult handleTo(Level world, ItemStack wireStack,
        BlockPos clickedPos, Vec3 clickedLoc) {

        Pair<ElectricNode, Double> clicked = target.nodes.getClosest(clickedLoc);
        if(clicked == null) return InteractionResult.PASS;
    
        double distance = clicked.getSecond().doubleValue();
        ElectricNode toNode = clicked.getFirst();

        if(distance > toNode.getHitSize() * 1.47) return InteractionResult.PASS;

        if(wireStack.getItem() instanceof WireSpool spool) {
            CompoundTag nbt = wireStack.getTag();
            if(world.getBlockEntity(getPos(nbt)) instanceof ElectricBlockEntity ebeFrom) {

                if(intermediary == null && wireStack.hasTag()) { // on world load this item may have NBT but no valid intermediary
                    clearTag(wireStack);
                    return InteractionResult.PASS;
                }

                NodeConnectResult result = ebeFrom.nodes.connect(intermediary.getSecond(), target.nodes, toNode.getId());
                if(!result.isSuccessful())
                    revert(wireStack, false);

                sendInfo(world, clickedPos, result);
                clearTag(wireStack);
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.FAIL;
    }

    private CompoundTag writePos(BlockPos pos) {
        CompoundTag out = new CompoundTag();
        out.putInt("x", pos.getX());
        out.putInt("y", pos.getY());
        out.putInt("z", pos.getZ());
        return out;
    }

    @Nullable
    public BlockPos getPos(CompoundTag in) {

        if(!in.contains("at")) return null;

        CompoundTag at = in.getCompound("at");
        BlockPos out = new BlockPos(
            at.getInt("x"),
            at.getInt("y"),
            at.getInt("z")
        );

        return out;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isSelected) {
        super.inventoryTick(stack, world, entity, slot, isSelected);
        if(entity instanceof Player player && stack.getTag() != null) {
            if(stack.getTag().contains("at") || stack.getTag().contains("from")) {
                if(player.oAttackAnim != 0 || player.getMainHandItem().getItem() != stack.getItem()) {
                    if(!world.isClientSide) {
                        revert(stack, true);
                    }
                }
            }
        }
    }

    private void revert(ItemStack stack, boolean clear) {
        Mechano.log("REVERT");
        clearTag(stack);
        if(clear && intermediary != null) cancelConnection(target, intermediary.getSecond().getSourceID());
    }

    private void cancelConnection(ElectricBlockEntity ebe, String sourceID) {
        if(ebe != null) ebe.nodes.cancelConnection(sourceID);
        sendInfo(ebe.getLevel(), ebe.getBlockPos(), NodeConnectResult.LINK_CANCELLED);
    }

    private void sendInfo(Level world, BlockPos pos, NodeConnectResult result) {
        player.displayClientMessage(result.getMessage(), true);
        result.playConnectSound(world, pos);
    }

    private void clearTag(ItemStack stack) {
        //if(!stack.hasTag()) return;
        stack.removeTagKey("at");
        stack.removeTagKey("from");
    }
}
