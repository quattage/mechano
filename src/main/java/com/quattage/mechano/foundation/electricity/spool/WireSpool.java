package com.quattage.mechano.foundation.electricity.spool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.MechanoItems;
import com.quattage.mechano.foundation.electricity.AnchorPointBank;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.anchor.interaction.AnchorInteractType;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Triplet;

/***
 * A WireSpool object is both a Minecraft Item as well as a logical representation
 * of an electric connection's properties. 
 */
public abstract class WireSpool extends Item {

    protected final String PREFIX = "wire_";
    protected final int rate;
    protected final String name;
    protected final ItemStack emptySpoolDrop;
    protected final ItemStack rawDrop;

    private int useCooldown = 0;
    private Pair<AnchorInteractType, FakeNodeConnection> intermediary;
    private Player player; 
    private WireNodeBlockEntity target;

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
        WireSpoolManager.addType(this);
    }

    /***
     * The name of this Spool's wire. This is used as the ID of this spool. 
     * ex. a name of "hookup" results in an ID of "wire_hookup"
     * This is used to look up the wire's texture and transmission
     * properties.
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

    public final ResourceLocation asResource() {
        return MechanoClient.WIRE_TEXTURE_PROVIDER.get(this);
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
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {

        ItemStack handStack = player.getItemInHand(hand);

        Vec3 clickedLocation = VectorHelper.getLookingPos(player).getLocation();
        Triplet<ArrayList<AnchorPoint>, Integer, AnchorPointBank<?>> clickSummary = null;

        // ignore physical search if the player interacts directly with an EBE
        if(world.getBlockEntity(VectorHelper.toBlockPos(clickedLocation)) instanceof WireNodeBlockEntity ebe) {
            Pair<AnchorPoint[], Integer> direct = ebe.nodeBank.getAllNodes(clickedLocation);
            clickSummary = new Triplet<ArrayList<AnchorPoint>, Integer, AnchorPointBank<?>> (
                    new ArrayList<AnchorPoint>(Arrays.asList(direct.getFirst())), 
                    direct.getSecond(), ebe.nodeBank
                );
        } else {
            clickSummary = AnchorPointBank.findClosestNodeAlongRay(world, player.getEyePosition(), clickedLocation, 0);
        }

        // this sanity check prevents a crash, don't remove it
        if(clickSummary.getC() == null)
            return new InteractionResultHolder<ItemStack>(
                InteractionResult.PASS,
                handStack
            );

        this.target = (WireNodeBlockEntity)clickSummary.getC().target;
        target.reOrient();
        this.player = player;
        
        if(clickSummary.getB() == -1) 
            return new InteractionResultHolder<ItemStack>(
                InteractionResult.PASS,
                handStack
            );

        if(handStack.hasTag()) {
            if(handStack.getTag().contains("at") && handStack.getTag().contains("from"))
                return new InteractionResultHolder<ItemStack>(
                    handleTo(world, handStack, clickSummary, clickedLocation),
                    handStack
                );
        }

        return new InteractionResultHolder<ItemStack>(
            handleFrom(world, handStack, clickSummary, clickedLocation),
            handStack
        ); 
    }

    // @Override
    // public InteractionResult useOn(UseOnContext context) {
    //     Level world = context.getLevel();
    //     Vec3 clickedLoc = VectorHelper.getLookingPos(context.getPlayer()).getLocation();
    //     ItemStack handStack = context.getItemInHand();
        
    //     if(handStack.hasTag()) {
    //         if(handStack.getTag().contains("at") && handStack.getTag().contains("from"))
    //             return handleTo(world, handStack, clickedLoc, context.getPlayer());
    //     }
    //     return handleFrom(world, handStack, clickedLoc, context.getPlayer()); 
    // }

    /***
     * Implementation of NodeBank and ElectricNode tomfoolery that occurs when the player
     * first initiates the connection. Creates a FakeNodeConnection attached to the player.
     * @return InteractionResult indicating the success or failure of this interaction
     */
    private InteractionResult handleFrom(Level world, ItemStack wireStack, Triplet<ArrayList<AnchorPoint>, Integer, AnchorPointBank<?>> clickSummary, Vec3 clickedLoc) {

        AnchorPoint targetedNode = clickSummary.getA().get(clickSummary.getB());
        int index = clickSummary.getC().indexOf(targetedNode);
        this.intermediary = clickSummary.getC().makeFakeConnection(this, index, player);

        if(intermediary.getFirst().isSuccessful()) {
            CompoundTag nbt = wireStack.getOrCreateTag();   
            nbt.put("at", writePos(clickSummary.getC().pos));
            nbt.putInt("from", index);
            sendInfo(world, clickSummary.getC().pos, intermediary.getFirst());
            return InteractionResult.PASS;
        }

        if(!intermediary.getFirst().isSuccessful())
            revert(wireStack, false);

        sendInfo(world, clickSummary.getC().pos, intermediary.getFirst());
        return InteractionResult.FAIL;
    }

    /***
     * Follow-through from handleFrom, called when the connection is made real. 
     * Turns the previously established FakeNodeConnection into an ElectricNodeConnection.
     * @return InteractionResult indicating the success or failure of this interaction
     */
    private InteractionResult handleTo(Level world, ItemStack wireStack, Triplet<ArrayList<AnchorPoint>, Integer, AnchorPointBank<?>> clickSummary, Vec3 clickedLoc) {

        AnchorPoint targetedNode = clickSummary.getA().get(clickSummary.getB());
        int index = clickSummary.getC().indexOf(targetedNode);
        if(wireStack.getItem() instanceof WireSpool spool) {
            CompoundTag nbt = wireStack.getTag();
            if(world.getBlockEntity(getPos(nbt)) instanceof WireNodeBlockEntity ebeFrom) {

                if(intermediary == null && wireStack.hasTag()) { // on world load this item may have NBT but no valid intermediary
                    clearTag(wireStack);
                    return InteractionResult.PASS; // the connection is just ignored in this case, you'll have to click it again.
                }

                AnchorInteractType result = ebeFrom.nodeBank.connect(intermediary.getSecond(), clickSummary.getC(), index);
                // Mechano.log("Success? " + result.isSuccessful() + " Fatal? " + result.isFatal());

                if(!world.isClientSide() && !result.isSuccessful()) {
                    if(result.isFatal()) {
                        revert(wireStack, false);
                        if(wireStack.hasTag())
                            target = (WireNodeBlockEntity)world.getBlockEntity(getPos(wireStack.getTag()));
                    } else {
                        if(wireStack.hasTag()) {
                            target = (WireNodeBlockEntity)world.getBlockEntity(getPos(wireStack.getTag()));
                        }
                    }
                }
                sendInfo(world, clickSummary.getC().pos, result);
                clearTag(result, wireStack);
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

    /***
     * Grabs the bound position of the WireSpool from the provided
     * CompoundTag.
     * @param in CompoundTag to pull from
     * @return A composed BlockPos indicating the connection target.
     */
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

    /***
     * Cancels the connection if the player isn't holding the wire or punches with it
     */
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

        if(useCooldown > 0) useCooldown--;
    }

    private void revert(ItemStack stack, boolean clear) {
        clearTag(stack);
        if(clear && intermediary != null) cancelConnection(target, intermediary.getSecond().getSourceID());
    }

    private void revert(WireNodeBlockEntity ebe, ItemStack stack, boolean clear) {
        clearTag(stack);
        if(clear && intermediary != null) cancelConnection(ebe, intermediary.getSecond().getSourceID());
    }

    private void cancelConnection(WireNodeBlockEntity ebe, int sourceID) {
        if(ebe != null) ebe.nodeBank.cancelConnection(sourceID);
        sendInfo(ebe.getLevel(), ebe.getBlockPos(), AnchorInteractType.LINK_CANCELLED);
    }

    private void sendInfo(Level world, BlockPos pos, AnchorInteractType result) {
        player.displayClientMessage(result.getMessage(), true);
        result.playConnectSound(world, pos);
    }

    private void clearTag(ItemStack stack) {
        //if(!stack.hasTag()) return;
        stack.removeTagKey("at");
        stack.removeTagKey("from");
    }

    private void clearTag(AnchorInteractType result, ItemStack stack) {
        if(!result.isSuccessful() && !result.isFatal()) return;
        stack.removeTagKey("at");
        stack.removeTagKey("from");
    }
}
