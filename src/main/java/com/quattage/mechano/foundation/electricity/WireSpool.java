
package com.quattage.mechano.foundation.electricity;

import java.util.ArrayList;

import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.MechanoItems;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.LinkResult;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.rendering.WireAnchorBlockRenderer;
import com.quattage.mechano.foundation.electricity.watt.unit.Voltage;
import com.quattage.mechano.foundation.network.AnchorSelectC2SPacket;

import net.createmod.catnip.data.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/***
 * A WireSpool object is both a Minecraft Item as well as a logical representation
 * of an electric connection's properties. These properties are registered as types
 * that can be accessed from a static ArrayList
 */
public abstract class WireSpool extends Item {


    // TODO factor this out into a generic class structure for wire types instead of loading it all in the item class
    private static final String PREFIX = "wire_";
    private static final ArrayList<WireSpool> SPOOL_TYPES = new ArrayList<>();

    private final String spoolName;
    private final int spoolID;

    private final float maxWatts;
    private final Voltage optimalVolts; // TODO make this a wrapper object to store optimal voltage

    private final ItemStack emptyDrop;
    private final ItemStack rawDrop;

    private GlobalTransferGrid network = null;
    private static GID selectedAnchorID = null;
    private Pair<AnchorPoint, WireAnchorBlockEntity> aP = null;

    public WireSpool(Properties properties) {
        super(properties);
        this.spoolName = setSpoolName();
        this.spoolID = SPOOL_TYPES.size();
        WireSpool.SPOOL_TYPES.add(this);
        this.maxWatts = setMaxWatts();
        this.optimalVolts = setOptimalVoltage();
        this.emptyDrop = new ItemStack(setEmptySpoolDrop());
        this.rawDrop = new ItemStack(setRawDrop());
    }

    /**
     * @return ArrayList of all registered spools
     */
    public static ArrayList<WireSpool> getAllTypes() {
        return SPOOL_TYPES;
    }

    /**
     * @param typeID Type of WireSpool to get
     * @return WireSpool at the given index
     */
    public static WireSpool ofType(int typeID) {
        return SPOOL_TYPES.get(typeID);
    }

    /**
     * Get the id (numerical index) of this WireSpool
     * @return
     */
    public int getSpoolID() {
        return spoolID;
    }

    public String getSpoolName() {
        return PREFIX + spoolName;
    }

    protected abstract String setSpoolName();

    /***
     * Define the maximum watt transfer rate of this WireSpool's associated wire type.
     */
    protected abstract float setMaxWatts();

    /***
     * Define the optimal voltage potential across this WireSpool's associated wire type;
     */
    protected abstract Voltage setOptimalVoltage(); 
    // TODO ^^ make this a wrapper object to store a bell curve function ^^

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

    public final ResourceLocation asResource() {
        return MechanoClient.WIRE_TEXTURE_PROVIDER.get(this);
    }

    public String getName() {
        return PREFIX + spoolName;
    }

    public final ItemStack getEmptySpool() {
        return emptyDrop;
    }
    
    public final ItemStack getRawDrop() {
        return rawDrop;
    }

    public Voltage getOptimalVoltage() {
        return optimalVolts;
    }

    public float getMaxWatts() {
        return maxWatts;
    }

    public static ItemStack getHeldByPlayer(Player player) {
        if(player == null) return null;
        ItemStack stack = player.getMainHandItem();
        if(stack != null && stack.getItem() instanceof WireSpool) return stack;
        stack = player.getOffhandItem();
        if(stack != null && stack.getItem() instanceof WireSpool) return stack;
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        
        ItemStack handStack = player.getItemInHand(hand);
        Pair<AnchorPoint, WireAnchorBlockEntity> currentAnchor = null;

        if(world.isClientSide()) {
            AnchorPoint present = MechanoClient.ANCHOR_SELECTOR.getSelectedAnchor(world);
            if(present == null) return InteractionResultHolder.pass(handStack);
            network = GlobalTransferGrid.of(world);
            MechanoPackets.sendToServer(new AnchorSelectC2SPacket(present));
            return InteractionResultHolder.pass(handStack);
        } else {
            network = GlobalTransferGrid.of(world);
        }

        if(selectedAnchorID == null)
            return InteractionResultHolder.pass(handStack);

        currentAnchor = AnchorPoint.getAnchorAt(world, selectedAnchorID);
        if(currentAnchor == null || currentAnchor.getFirst() == null) return InteractionResultHolder.fail(handStack);

        if(!network.isVertAvailable(currentAnchor.getFirst().getID())) {
            player.displayClientMessage(LinkResult.DESTINATION_FULL.getMessage(), true);
            return InteractionResultHolder.fail(handStack);
        }
        
        CompoundTag nbt = handStack.getOrCreateTag();
        if(nbt.isEmpty()) {
            selectedAnchorID.writeTo(nbt);
            WireAnchorBlockRenderer.resetOldPos(player, currentAnchor.getFirst());
        } else {
            if(!GID.isValidTag(nbt)) { // validate just in case, this code may never be reached idk
                clearTag(handStack);
                player.displayClientMessage(LinkResult.GENERIC.getMessage(), true);
                return InteractionResultHolder.fail(handStack);
            }

            Pair<AnchorPoint, WireAnchorBlockEntity> previousAnchor = AnchorPoint.getAnchorAt(world, GID.of(nbt));
            if(previousAnchor == null ) {
                clearTag(handStack);
                return InteractionResultHolder.fail(handStack);
            }

            if(currentAnchor.getFirst().getID().getBlockPos().equals(previousAnchor.getFirst().getID().getBlockPos()))
                return InteractionResultHolder.fail(handStack);

            if(!previousAnchor.getFirst().equals(currentAnchor.getFirst())) {
                
                if(network.isVertAvailable(currentAnchor.getFirst().getID()) && network.isVertAvailable(previousAnchor.getFirst().getID())) {
                    LinkResult linkResult = network.link(player, previousAnchor.getFirst().getID(), selectedAnchorID, this.getSpoolID());
                    player.displayClientMessage(linkResult.getMessage(), true);
                    clearTag(handStack);
                    if(linkResult.isSuccessful()) return InteractionResultHolder.success(handStack);
                    return InteractionResultHolder.fail(handStack);
                }
                clearTag(handStack);
                return InteractionResultHolder.pass(handStack);
            }
            return InteractionResultHolder.pass(handStack);
        }
        return InteractionResultHolder.fail(handStack);
    }

    public void clearTag(ItemStack stack) {
        stack.setTag(new CompoundTag());
    }

    //                                        §    https://hypixel.net/attachments/colorcodes-png.2694223/
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isSelected) {
        if(entity instanceof Player player) {

            if(!isSelected) selectedAnchorID = null;

            CompoundTag nbt = stack.getOrCreateTag();
            if(GID.isValidTag(nbt)) {
                aP = AnchorPoint.getAnchorAt(world, GID.of(nbt));
                if(isSelected) {
                    MutableComponent message = Component.translatable("actionbar.mechano.connection.linking");
                    message.append(" §r§7[§r§l§a§l" + nbt.getInt("x") + "§r§2, §r§a§l" + nbt.getInt("y") + "§r§2, §r§a§l" + nbt.getInt("z") + "§r§7]");
                    player.displayClientMessage(message, true);
                } else {
                    player.displayClientMessage(LinkResult.USER_CANCEL.getMessage(), true);
                    stack.setTag(new CompoundTag());
                }
                if(aP != null && aP.getSecond() != null) 
                    aP.getSecond().getAnchorBank().setIsAwaitingConnection(world, true);
            } else {
                if(aP != null && aP.getSecond() != null) 
                    aP.getSecond().getAnchorBank().setIsAwaitingConnection(world, false);
                aP = null;
            }
        }
    }

    public void setSelectedAnchor(GID id) {
        selectedAnchorID = id;
    }
}