package com.quattage.mechano.foundation.api;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoDataAttachments;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifiable;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifier.Key;
import com.quattage.mechano.foundation.api.landmark.client.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.client.AnchorSelector;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.transmitter.Transmitable;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.blockEntity.renderer.PowerGridBlockEntityRenderer;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class WireSpoolItem<T extends Transmitter<?>> extends Item implements Transmitable<T> {

    public WireSpoolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if(!level.isClientSide) 
            return handleUseAsServer(player);
        if(!AnchorSelector.INSTANCE.hasSelection()) 
            return InteractionResultHolder.fail(AnchorSelector.INSTANCE.playerHands.stack());
        
        ItemStack stack = AnchorSelector.INSTANCE.playerHands.stack();
        AnchorSelector.Active sel = AnchorSelector.INSTANCE.selected;
        if(!stack.has(MechanoDataAttachments.ADDRESS_COMPONENT)) {
            if(!AnchorSelector.INSTANCE.selected.response.indicatesSuccess()) {
                // TODO send initial fail message to selector and GuiLayer
                return InteractionResultHolder.fail(stack);
            }
            Key addr = sel.strip();
            stack.set(MechanoDataAttachments.ADDRESS_COMPONENT, addr);
            PowerGridBlockEntityRenderer.selected = AnchorPoint.retrieve(level, addr);
            if(PowerGridBlockEntityRenderer.selected == null) cancelAwaitingConnection(sel, null, stack);
            return InteractionResultHolder.success(stack);
        }

        AnchorPoint previous = AnchorPoint.retrieve(level, stack);
        if(previous == null || AnchorSelector.INSTANCE.isSelected(previous))
            return InteractionResultHolder.pass(stack);

        GlobalClientGrid client = SidedGridDispatcher.client(player);
        Response<?> result = client.requestLink(previous, AnchorSelector.INSTANCE.selected.anchor, getTransmitterType());
        if(Response.shouldBail(result)) 
            cancelAwaitingConnection(sel, previous, stack);
        if(result.indicatesSuccess()) 
            return InteractionResultHolder.success(stack);

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, world, entity, slotId, isSelected);
        if(!world.isClientSide) return;
        NodeIdentifiable addr = stack.get(MechanoDataAttachments.ADDRESS_COMPONENT);
        if(addr == null) return;
        AnchorPoint previous = AnchorPoint.retrieve(world, addr);
        if(previous == null)
            cancelAwaitingConnection(addr, previous, stack);
        else if(!previous.existsInWorld(world))
            cancelAwaitingConnection(addr, previous, stack);
        else if(!previous.hasRoom())
            cancelAwaitingConnection(addr, previous, stack);
    }


    public void cancelAwaitingConnection(NodeIdentifiable addr, @Nullable AnchorPoint target, ItemStack stack) {
        Mechano.LOGGER.info("CANCELLED: " + addr);
        stack.remove(MechanoDataAttachments.ADDRESS_COMPONENT);
        PowerGridBlockEntityRenderer.selected = null;
    }

    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
        return stack.has(MechanoDataAttachments.ADDRESS_COMPONENT);
    }

    private InteractionResultHolder<ItemStack> handleUseAsServer(Player player) {
        HoldingSummary held = Transmitable.getHolding(player);
        return InteractionResultHolder.pass(held.stack());
    }
}
