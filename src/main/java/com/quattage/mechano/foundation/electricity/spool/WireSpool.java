package com.quattage.mechano.foundation.electricity.spool;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.MechanoItems;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.anchor.interaction.AnchorInteractType;
import com.quattage.mechano.foundation.electricity.rendering.WireAnchorBlockRenderer;
import com.quattage.mechano.foundation.electricity.system.SVID;
import com.quattage.mechano.foundation.network.AnchorSelectC2SPacket;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
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
 * of an electric connection's properties. 
 */
public abstract class WireSpool extends Item {

    private final String PREFIX = "wire_";
    private final int rate;
    private final String name;
    private final ItemStack emptyDrop;
    private final ItemStack rawDrop;

    private SVID selectedAnchorID = null;

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
        this.emptyDrop = new ItemStack(setEmptySpoolDrop());
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
        return emptyDrop;
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

        if(world.isClientSide()) {
            MechanoPackets.sendToServer(new AnchorSelectC2SPacket(WireAnchorBlockRenderer.getSelectedAnchor()));
            return super.use(world, player, hand);
        }

        if(selectedAnchorID == null)
            return InteractionResultHolder.pass(handStack);

        AnchorPoint currentAnchor = AnchorPoint.getAnchorAt(world, selectedAnchorID);
        if(currentAnchor == null) return InteractionResultHolder.fail(handStack);

        if(!NETWORK.isVertAvailable(currentAnchor.getID())) {
            player.displayClientMessage(AnchorInteractType.ANCHOR_FULL.getMessage(), true);
            return InteractionResultHolder.fail(handStack);
        } 
        
        CompoundTag nbt = handStack.getOrCreateTag();
        if(nbt.isEmpty()) {
            selectedAnchorID.writeTo(nbt);
        } else {
            if(!SVID.isValidTag(nbt)) { // validate just in case, this code may never be reached idk
                clearTag(handStack);
                player.displayClientMessage(AnchorInteractType.GENERIC.getMessage(), true);
                return InteractionResultHolder.fail(handStack);
            }

            AnchorPoint previousAnchor = AnchorPoint.getAnchorAt(world, SVID.of(nbt));
            if(previousAnchor == null ) {
                clearTag(handStack);
                return InteractionResultHolder.fail(handStack);
            }

            if(!previousAnchor.equals(currentAnchor)) {
                if(NETWORK.isVertAvailable(currentAnchor.getID()) && NETWORK.isVertAvailable(previousAnchor.getID())) {
                    AnchorInteractType linkResult = NETWORK.link(previousAnchor.getID(), selectedAnchorID);
                    player.displayClientMessage(linkResult.getMessage(), true);
                    clearTag(handStack);
                    if(linkResult.isSuccessful())
                        return InteractionResultHolder.success(handStack);
                    return InteractionResultHolder.fail(handStack);
                }

                player.displayClientMessage(AnchorInteractType.ANCHOR_FULL.getMessage(), true);
                clearTag(handStack);
                return InteractionResultHolder.pass(handStack);
            }

            player.displayClientMessage(AnchorInteractType.LINK_CONFLICT.getMessage(), true);
            clearTag(handStack);
            return InteractionResultHolder.pass(handStack);
        }

        return InteractionResultHolder.fail(handStack);
    }


    @SuppressWarnings("unused")
    public void clearTag(ItemStack stack) {
        stack.setTag(new CompoundTag());
    }

    //                                        §    https://hypixel.net/attachments/colorcodes-png.2694223/
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isSelected) {
        if(entity instanceof Player player) {
            if(isSelected) {
                CompoundTag nbt = stack.getOrCreateTag();
                if(SVID.isValidTag(nbt)) {
                    MutableComponent message = Component.translatable("actionbar.mechano.connection.linking");
                    message.append(" §r§7[§r§l§a§l" + nbt.getInt("x") + "§r§2, §r§a§l" + nbt.getInt("y") + "§r§2, §r§a§l" + nbt.getInt("z") + "§r§7]");
                    player.displayClientMessage(message, true);
                }
            } else {
                CompoundTag nbt = stack.getTag();
                if(SVID.isValidTag(nbt)) {
                    player.displayClientMessage(AnchorInteractType.LINK_CANCELLED.getMessage(), true);
                    stack.setTag(new CompoundTag());
                } 
            }
        }
    }

    private void sendInfo(Level world, Player player, BlockPos pos, AnchorInteractType result) {
        player.displayClientMessage(result.getMessage(), true);
        result.playConnectSound(world, pos);
    }

    public void setSelectedAnchor(SVID selectedAnchorID) {
        this.selectedAnchorID = selectedAnchorID;
    }
}
