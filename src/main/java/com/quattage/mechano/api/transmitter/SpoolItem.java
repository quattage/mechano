package com.quattage.mechano.api.transmitter;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoClientEvents;
import com.quattage.mechano.api.JackSelector;
import com.quattage.mechano.api.grid.topology.CircuitComponentProvider;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;
import com.quattage.mechano.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.LeftClickCapturable;
import com.quattage.mechano.foundation.mixin.client.accessor.PlayerInfoAccessor;
import com.quattage.mechano.foundation.tracking.DataSourceIdentifier;
import com.quattage.mechano.foundation.tracking.GridUUID;

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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class SpoolItem extends Item implements CircuitComponentProvider, LeftClickCapturable {

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

    @Override
    public GridResponse evaluateTarget(ClientLevel world, AncillaryJack target) {
        return GridResponse.TASK_SELECT_SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean hasAwaiting(Player player) {
        for(ItemStack stack : player.getInventory().items) {
            if(stack != null && stack.getItem() instanceof SpoolItem && stack.has(DataSourceIdentifier.ATTACHMENT))
                return true;
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide) return InteractionResultHolder.pass(JackSelector.getInstance().getHeldCircuitProvider(player));
        if(usedHand != InteractionHand.MAIN_HAND || !JackSelector.getInstance().hasSelection()) 
            return InteractionResultHolder.fail(JackSelector.getInstance().getHeldCircuitProvider(player));
        ItemStack stack = JackSelector.getInstance().getHeldCircuitProvider(player);
        if(!stack.has(DataSourceIdentifier.ATTACHMENT))
            return handleFirstRightClick(player, stack, JackSelector.getInstance().target());
        return handleSecondRightClick(player, stack, JackSelector.getInstance().target());
    }

    /**
     * Called when the player initially selects an {@link AncillaryJack}, which creates a temporary link and catenary
     * between the selected {@link AncillaryJack} and the player.
     */
    @OnlyIn(Dist.CLIENT)
    private InteractionResultHolder<ItemStack> handleFirstRightClick(Player player, ItemStack stack, @Nullable AncillaryJack initialTarget) {
        if(SpoolItem.hasAwaiting(player)) return InteractionResultHolder.fail(stack);
        return InteractionResultHolder.success(stack);
    }

    /**
     * Called when the player selects a second {@link AncillaryJack}, which finalizes the creation of a valid link and catenary
     */
    @OnlyIn(Dist.CLIENT)
    private InteractionResultHolder<ItemStack> handleSecondRightClick(Player player, ItemStack stack, @Nullable AncillaryJack subsequentTarget) {
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean isSelected) {

        if(!world.isClientSide) return;
        GridUUID startAddress = stack.get(DataSourceIdentifier.ATTACHMENT);
        if(startAddress == null) return;

    }

    private void cancelAwaitingConnection(@Nullable GridUUID startAddress, @Nullable GridUUID endAddress, ItemStack stack) {
        stack.remove(DataSourceIdentifier.ATTACHMENT);
        if(startingDamage > -1) stack.setDamageValue(startingDamage);
        startingDamage = -1;
        if(startAddress == null || endAddress == null) return;
    }


    @Override
    public boolean onLeftClick(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        if(!stack.has(DataSourceIdentifier.ATTACHMENT)) return false;
        GridUUID addr = stack.get(DataSourceIdentifier.ATTACHMENT);
        if(addr == null) return false;

        // WireJack previous = addr.getAnchor((ClientLevel)player.level());
        // if(previous == null) {
        //     AnchorPoint selfAnchor = AnchorPoint.getLocal(player);
        //     SidedGridDispatcher.client(player).requestLinkDestruction(selfAnchor, previous, true);
        //     return true;
        // }

        // Vec3 disp = previous.getPos(player.level()).subtract(player.getPosition(1)).normalize();
        // float faceDot = (float)player.getViewVector(1).dot(disp);
        // if(faceDot < 0.2) return true;
        // AnchorPoint selfAnchor = AnchorPoint.getLocal(player);
        // SidedGridDispatcher.client(player).requestLinkDestruction(selfAnchor, previous, true);
        // cancelAwaitingConnection(addr, null, stack);
        if(hand != null) {
            player.swing(hand);
            player.swingTime = 1;
        }
        return true;
    }


    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
        return stack.has(DataSourceIdentifier.ATTACHMENT);
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
        Mechano.lang().text(String.format("%.1f", current) + "/" + String.format("%.1f", max) + "m").style(ChatFormatting.GRAY).forGoggles(tooltipComponents);
    }
}
