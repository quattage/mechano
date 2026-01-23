
package com.quattage.mechano.api.transmitter;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoClientEvents;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridComponentTracker;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.switchboard.JackSelector;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.transmitter.TransmitterType.TransmitterProvider;
import com.quattage.mechano.foundation.LeftClickCapturable;
import com.quattage.mechano.foundation.MapLikeItemHoldable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class SpoolItem extends Item implements TransmitterProvider, LeftClickCapturable, MapLikeItemHoldable {

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

    /**
     * Checks whether or not the player currently has a SpoolItem with a binding in their inventory.
     * Due to the iterative nature of this method, this shouldn't be called often
     * @param player ServerPlayer or AbstractClientPlayer instance - this method is unsided
     * @returns <code>true</code> if the given <code>player</code>'s inventory contains a <code>SpoolItem</code> with
     * a {@link ComponentUUID} attachment
     */
    public static boolean hasAwaiting(Player player) {
        for(ItemStack stack : player.getInventory().items) {
            if(stack != null && stack.getItem() instanceof SpoolItem && stack.has(MechanoData.UUID))
                return true;
        }
        return false;
    }

    private int startingDamage = -1;

    public SpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide) return InteractionResultHolder.pass(JackSelector.getInstance().getHeldCircuitProvider(player));
        if(usedHand != InteractionHand.MAIN_HAND || !JackSelector.getInstance().hasSelection()) 
            return InteractionResultHolder.fail(JackSelector.getInstance().getHeldCircuitProvider(player));
        ItemStack stack = JackSelector.getInstance().getHeldCircuitProvider(player);
        ClientGrid grid = Grid.client(player);
        if(!stack.has(MechanoData.UUID))
            return handleFirstRightClick(grid, player, stack, JackSelector.getInstance().target());
        return handleSecondRightClick(grid, player, stack, JackSelector.getInstance().target());
    }

    /**
     * Called when the player initially selects an {@link AncillaryNode}, which creates a temporary link and catenary
     * between the selected {@link AncillaryNode} and the player.
     */
    @OnlyIn(Dist.CLIENT)
    private InteractionResultHolder<ItemStack> handleFirstRightClick(ClientGrid grid, Player player, ItemStack stack, @Nullable AncillaryNode<?> initialTarget) {
        if(SpoolItem.hasAwaiting(player) || initialTarget == null) 
            return InteractionResultHolder.fail(stack);
        Griddable<?> source = initialTarget.getProviderSource();
        if(source == null) {
            throw new NullPointerException("Failed while handling interaction with " 
                + initialTarget + " - This ancillary couldn't provide a non-null source!");
        }
        ComponentUUID<?> sourceID = GridComponentTracker.getAddress(source, initialTarget);
        CircuitComponent component = GridComponentTracker.findOrThrow(grid, sourceID);
        if(component == null || (component != initialTarget))
            return InteractionResultHolder.fail(stack);
        stack.set(MechanoData.UUID, sourceID);
        return InteractionResultHolder.success(stack);
    }

    /**
     * Called when the player selects a second {@link AncillaryNode}, which finalizes the creation of a valid link and catenary
     */
    @OnlyIn(Dist.CLIENT)
    private InteractionResultHolder<ItemStack> handleSecondRightClick(ClientGrid grid, Player player, ItemStack stack, @Nullable AncillaryNode<?> subsequentTarget) {
        if(subsequentTarget == null) 
            return InteractionResultHolder.fail(stack);
        ComponentUUID<?> initialTargetID = stack.get(MechanoData.UUID);
        AncillaryNode<?> initialTarget = (AncillaryNode<?>) GridComponentTracker.findOrThrow(grid, initialTargetID);
        Griddable<?> initialSource = initialTarget.getProviderSource();
        if(initialSource == null) {
            throw new NullPointerException("Failed while handling interaction with " 
                + initialTarget + " - The initial ancillary couldn't provide a non-null source!");
        }
        Griddable<?> subsequentSource = subsequentTarget.getProviderSource();
        if(subsequentSource == null) {
            throw new NullPointerException("Failed while handling interaction with " 
                + subsequentTarget + " - The subsequent ancillary couldn't provide a non-null source!");
        }
        if(!GridComponentTracker.isReachable(grid.getWorld(), initialSource) || !GridComponentTracker.isReachable(grid.getWorld(), subsequentSource)) 
            return InteractionResultHolder.fail(stack);
        ComponentUUID<?> subsequentTargetID = GridComponentTracker.getAddress(subsequentSource, subsequentTarget);
        GridAction request = grid.initiateTask(GridAction.TASK_LINK_CREATE)
            .from(initialSource, subsequentSource)
            .withArguments(initialTargetID, subsequentTargetID, getTransmitter(), player.getUUID())
            .requestRun();
        if(GridAction.VERBOSE_LOGS) grid.debug("Initiated link interaction from " + player);
        return request.getResultHolder(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean isSelected) {
        if(!world.isClientSide) return;
        ComponentUUID<?> startAddress = stack.get(MechanoData.UUID);
        if(startAddress == null) return;
        
    }

    private void cancelAwaitingConnection(@Nullable ComponentUUID<?> startAddress, @Nullable ComponentUUID<?> endAddress, ItemStack stack) {
        stack.remove(MechanoData.UUID);
        if(startingDamage > -1) stack.setDamageValue(startingDamage);
        startingDamage = -1;
        if(startAddress == null || endAddress == null) return;
    }


    @Override
    public boolean onLeftClick(Player player, ItemStack stack, @Nullable InteractionHand hand) {
        if(!stack.has(MechanoData.UUID)) return false;
        ComponentUUID<?> addr = stack.get(MechanoData.UUID);
        if(addr == null) return false;
        stack.remove(MechanoData.UUID);

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
        return stack.has(MechanoData.UUID);
    }

    // /**
    //  * A helper method for setting the durability of this spool
    //  * based on the proportional length of the currently awaiting catenary
    //  * @param stack
    //  * @param length
    //  * @param maxLength
    //  */
    // private void applyDurability(Entity entity, ItemStack stack, float length) {
    //     if(startingDamage < 0) return;
    //     if(entity instanceof Player player) {
    //         GameType mode = ((PlayerInfoAccessor)player).mechano$getPlayerInfo().getGameMode();
    //         if(mode == null || mode == GameType.CREATIVE || mode == GameType.SPECTATOR)
    //             return;
    //     }            
    //     stack.setDamageValue(Math.min(stack.getMaxDamage(), Math.max(1, startingDamage + (int)Math.ceil((length * 2f)))));
    // }

    /**
     * Minecraft's default "Durability: xx/xx" tooltip is added
     * to all damageable items automatically. If this method
     * returns <code>true</code>, that behaviour is skipped
     * by the {@link MechanoClientEvents#onTooltipGather tooltip event.}
     * Suppressing the tooltip is intended to be used in conjuction with
     * an override to {@link #appendHoverText} in order to replace the 
     * durability hint with one that makes more sense for spools 
     * by converting the durability value to meters.
     */
    public boolean hidesDefaultTooltip() { return true; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        float max = (float)stack.getMaxDamage() / 2f;
        float current = max - ((float)stack.getDamageValue() / 2f);
        Mechano.lang().text(String.format("%.1f", current) + "/" + String.format("%.1f", max) + "m").style(ChatFormatting.GRAY).forGoggles(tooltipComponents);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderSpecial(ItemStack stack) {
        return stack.has(MechanoData.UUID);
    }
}
