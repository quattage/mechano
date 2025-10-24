package com.quattage.mechano.foundation.item;

import static com.quattage.mechano.Mechano.lang;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoClientEvents;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.LinkDataStorage;
import com.quattage.mechano.api.SidedGridDispatcher;
import com.quattage.mechano.api.anchor.AnchorPoint;
import com.quattage.mechano.api.anchor.AnchorSelector;
import com.quattage.mechano.api.catenary.CatenaryAttributable;
import com.quattage.mechano.api.entity.GriddableEntityAttachment;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.identifier.EntityUUID;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.api.identifier.UUIDDiscriminator;
import com.quattage.mechano.api.landmark.GridCatenary;
import com.quattage.mechano.api.landmark.GridConnection;
import com.quattage.mechano.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.api.switchboard.GridResponse;
import com.quattage.mechano.api.switchboard.LinkRequestPacket;
import com.quattage.mechano.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.api.transmitter.Transmitable;
import com.quattage.mechano.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.mixin.client.accessor.PlayerInfoAccessor;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class SpoolItem<T extends Transmitter<?>> extends Item implements Transmitable<T>, LeftClickCapturable {

    private int startingDamage = -1;

    public SpoolItem(Properties properties) {
        super(properties);
    }

    /**
     * Removes all data from every spool in player's inventory. 
     */
    @OnlyIn(Dist.CLIENT)
    public static void wipeData(Player player, boolean cancelEarly) {
        ItemStack held = player.getMainHandItem();
        if(held != null && held.getItem() instanceof SpoolItem spool) {
            spool.cancelAwaitingConnection(null, null, held);
            if(cancelEarly) return;
        }
        held = player.getOffhandItem();
        if(held != null && held.getItem() instanceof SpoolItem spool) {
            spool.cancelAwaitingConnection(null, null, held);
            if(cancelEarly) return;
        }
        // normally the player is only ever permitted to have one bound
        // spool at a time, but just in case, we check the whole inventory
        for(ItemStack stack : player.getInventory().items) {
            if(stack != null && stack.getItem() instanceof SpoolItem spool) {
                spool.cancelAwaitingConnection(null, null, stack);
                if(cancelEarly) return;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasAwaiting(Player player) {
        for(ItemStack stack : player.getInventory().items) {
            if(stack != null && stack.getItem() instanceof SpoolItem && stack.has(UUIDDiscriminator.ATTACHMENT))
                return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide) return InteractionResultHolder.pass(Transmitable.getHolding(player).stack());
        if(usedHand != InteractionHand.MAIN_HAND || !AnchorSelector.INSTANCE.hasSelection()) 
            return InteractionResultHolder.fail(AnchorSelector.INSTANCE.getHeldStack());
        ItemStack stack = AnchorSelector.INSTANCE.getHeldStack();
        if(!stack.has(UUIDDiscriminator.ATTACHMENT)) 
            return handleFirstRightClick(player, stack, AnchorSelector.INSTANCE.getSelected());
        return handleSecondRightClick(player, stack, AnchorSelector.INSTANCE.getSelected());
    }

    /**
     * Called when the player initially selects an anchor, which creates a temporary link and catenary
     * between the selected anchor and the player.
     */
    @OnlyIn(Dist.CLIENT)
    private InteractionResultHolder<ItemStack> handleFirstRightClick(Player player, ItemStack stack, @Nullable AnchorPoint targetAnchor) {
        if(hasAwaiting(player)) return InteractionResultHolder.fail(stack);
        if(targetAnchor == null || !AnchorSelector.INSTANCE.isSelectedGood()) {
            cancelAwaitingConnection(targetAnchor.getAddress(), null, stack);
            return InteractionResultHolder.fail(stack);
        }
        AnchorPoint selfAnchor = AnchorPoint.getLocal(player);
        if(selfAnchor == null) {
            cancelAwaitingConnection(targetAnchor.getAddress(), null, stack);
            return InteractionResultHolder.fail(stack);
        }
        GridResponse result = SidedGridDispatcher.client(player).requestLinkCreation(selfAnchor, targetAnchor, getTransmitterType(), true);
        startingDamage = stack.getDamageValue();
        if(!result.indicatesCompletion()) {
            cancelAwaitingConnection(targetAnchor.getAddress(), null, stack);
            Mechano.LOGGER.warn("Link request returned failure state '" + result + "' (Requested by '" + player.getName() + "', from " + targetAnchor + " -> " + selfAnchor + ")");
            return InteractionResultHolder.fail(stack);
        }
        stack.set(UUIDDiscriminator.ATTACHMENT, targetAnchor.getAddress());
        return InteractionResultHolder.success(stack);
    }

    /**
     * Called when the player selects a second anchor, which finalizes the creation of a valid link and catenary
     */
    @OnlyIn(Dist.CLIENT)
    private InteractionResultHolder<ItemStack> handleSecondRightClick(Player player, ItemStack stack, @Nullable AnchorPoint endAnchor) {

        if(endAnchor == null) {
            cancelAwaitingConnection(null, null, stack);
            return InteractionResultHolder.fail(stack);
        }

        GridResponse initialResponse = AnchorSelector.INSTANCE.getSelectedResponse();
        if(!initialResponse.indicatesCompletion()) {
            if(initialResponse.shouldFailHard()) {
                onLeftClick(player, stack, InteractionHand.MAIN_HAND);
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        
        GridUUID startAddress = stack.get(UUIDDiscriminator.ATTACHMENT);
        AnchorPoint startAnchor = startAddress.getAnchor((ClientLevel)player.level());
        if(startAnchor == null) {
            cancelAwaitingConnection(null, endAnchor.getAddress(), stack);
            return InteractionResultHolder.fail(stack);
        }

        if(startAnchor.getAddress().equals(endAnchor.getAddress()))
            return InteractionResultHolder.pass(stack);

        ClientGrid grid = SidedGridDispatcher.client(player);
        GridResponse result = grid.requestLinkCreation(startAnchor, endAnchor, getTransmitterType(), true);
        if(!result.indicatesCompletion()) {
            if(result.shouldFailHard()) {
                onLeftClick(player, stack, InteractionHand.MAIN_HAND);
                return InteractionResultHolder.fail(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        Griddable<?> playerPoints = GriddableEntityAttachment.of(player, true);
        result = grid.requestLinkDestruction(startAnchor, playerPoints.getAnchors().get(0), true);
        applyDurability(player, stack, GridConnection.getEuclideanDistance(player.level(), startAddress, endAnchor.getAddress()));
        stack.remove(UUIDDiscriminator.ATTACHMENT);
        startingDamage = -1;
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean isSelected) {

        if(!world.isClientSide) return;
        GridUUID startAddress = stack.get(UUIDDiscriminator.ATTACHMENT);
        if(startAddress == null) return;

        EntityUUID playerAddress = new EntityUUID(entity.getUUID(), 0);
        AnchorPoint startAnchor = startAddress.getAnchor((ClientLevel)world);
        if(startAnchor == null || (startAnchor.getCurrentConnections() > startAnchor.getMaxConnections())) {
            Mechano.LOGGER.warn("Connection to " + startAnchor + " was cancelled prematurely.");
            cancelAwaitingConnection(startAnchor == null ? null : startAnchor.getAddress(), playerAddress, stack);
            return;
        }

        LinkDataStorage.Client storage = LinkDataStorage.getAsClient(entity, false);
        if(storage == null) return;
        GridCatenary cat = storage.get(world, new ConnectionKey(playerAddress, startAddress));
        if(cat == null) return;
        applyDurability(entity, stack, cat.calculateSpan());
        cat.adjustSpan(world, (stack.getMaxDamage() - stack.getDamageValue()) / 2f);
    }

    private void cancelAwaitingConnection(@Nullable GridUUID startAddress, @Nullable GridUUID endAddress, ItemStack stack) {
        stack.remove(UUIDDiscriminator.ATTACHMENT);
        if(startingDamage > -1) stack.setDamageValue(startingDamage);
        startingDamage = -1;
        if(startAddress == null || endAddress == null) return;
        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(startAddress, endAddress, MechanoTransmissionTypes.PERFECT_CONDUCTOR, GridResponse.TASK_DESTROY_LINK));
    }


    @Override
    public boolean onLeftClick(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        if(!stack.has(UUIDDiscriminator.ATTACHMENT)) return false;
        GridUUID addr = stack.get(UUIDDiscriminator.ATTACHMENT);
        if(addr == null) return false;

        AnchorPoint previous = addr.getAnchor((ClientLevel)player.level());
        if(previous == null) {
            AnchorPoint selfAnchor = AnchorPoint.getLocal(player);
            SidedGridDispatcher.client(player).requestLinkDestruction(selfAnchor, previous, true);
            return true;
        }

        Vec3 disp = previous.getPos(player.level()).subtract(player.getPosition(1)).normalize();
        float faceDot = (float)player.getViewVector(1).dot(disp);
        if(faceDot < CatenaryAttributable.DETACH_THRESHOLD) return true;
        AnchorPoint selfAnchor = AnchorPoint.getLocal(player);
        SidedGridDispatcher.client(player).requestLinkDestruction(selfAnchor, previous, true);
        cancelAwaitingConnection(addr, null, stack);
        if(hand != null) {
            player.swing(hand);
            player.swingTime = 1;
        }
        return true;
    }


    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
        return stack.has(UUIDDiscriminator.ATTACHMENT);
    }


    /**
     * A helper method for setting the durability of this spool
     * based on the proportional length of the currently awaiting catenary
     * @param stack
     * @param length
     * @param maxLength
     */
    private void applyDurability(Entity entity, ItemStack stack, float length) {
        if(startingDamage < 0) return;
        if(entity instanceof Player player) {
            GameType mode = ((PlayerInfoAccessor)player).mechano$getPlayerInfo().getGameMode();
            if(mode == null || mode == GameType.CREATIVE || mode == GameType.SPECTATOR)
                return;
        }            
        stack.setDamageValue(Math.min(stack.getMaxDamage(), Math.max(1, startingDamage + (int)Math.ceil((length * 2f)))));
    }

    /**
     * Minecraft's default "Durability: xx/xx" tooltip is added
     * to all damageable items automatically. If this method
     * returns <code>true</code>, that behaviour is skipped
     * by the {@link MechanoClientEvents#onTooltipGather tooltip overwriter.}
     * This method is designed to be used in conjunction with
     * an override to {@link #appendHoverText} to replace the 
     * durability indicator with one that makes more sense
     * for spools (by clarifying the unit as a meter)
     */
    public boolean hidesDefaultTooltip() { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        float max = (float)stack.getMaxDamage() / 2f;
        float current = max - ((float)stack.getDamageValue() / 2f);
        lang().text(String.format("%.1f", current) + "/" + String.format("%.1f", max) + "m").style(ChatFormatting.GRAY).forGoggles(tooltipComponents);
    }
}
