package com.quattage.mechano.foundation.api;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorSelector;
import com.quattage.mechano.foundation.api.landmark.uuid.GridUUID;
import com.quattage.mechano.foundation.api.landmark.uuid.GridUUIDData;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.transmitter.Transmitable;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.blockEntity.renderer.PowerGridBlockEntityRenderer;

import net.minecraft.client.multiplayer.ClientLevel;
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
        if(!stack.has(GridUUIDData.ATTACHMENT)) {
            if(!AnchorSelector.INSTANCE.selected.response.indicatesSuccess()) {
                // TODO send initial fail message to selector and GuiLayer
                return InteractionResultHolder.fail(stack);
            }
            stack.set(GridUUIDData.ATTACHMENT, sel.address);
            PowerGridBlockEntityRenderer.selected = sel.anchor;
            if(PowerGridBlockEntityRenderer.selected == null) cancelAwaitingConnection(sel.address, sel.anchor, stack);
            return InteractionResultHolder.success(stack);
        }

        GridUUID lastAddress = stack.get(GridUUIDData.ATTACHMENT);
        AnchorPoint lastAnchor = lastAddress.getAnchor((ClientLevel)level);
        if(lastAnchor == null || AnchorSelector.INSTANCE.isSelected(lastAddress))
            return InteractionResultHolder.pass(stack);

        GlobalClientGrid client = SidedGridDispatcher.client(player);
        Response<?> result = client.requestLink(lastAnchor, AnchorSelector.INSTANCE.selected.anchor, getTransmitterType());
        if(Response.shouldBail(result)) 
            cancelAwaitingConnection(lastAddress, sel.anchor, stack);
        if(result.indicatesSuccess()) 
            return InteractionResultHolder.success(stack);

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, world, entity, slotId, isSelected);
        if(!world.isClientSide) return;
        GridUUID addr = stack.get(GridUUIDData.ATTACHMENT);
        if(addr == null) return;
        AnchorPoint previous = addr.getAnchor((ClientLevel)world);
        if(previous == null)
            cancelAwaitingConnection(addr, previous, stack);
        else if(!previous.hasRoom())
            cancelAwaitingConnection(addr, previous, stack);
    }


    public void cancelAwaitingConnection(GridUUID addr, @Nullable AnchorPoint target, ItemStack stack) {
        stack.remove(GridUUIDData.ATTACHMENT);
        PowerGridBlockEntityRenderer.selected = null;
    }

    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
        return stack.has(GridUUIDData.ATTACHMENT);
    }

    private InteractionResultHolder<ItemStack> handleUseAsServer(Player player) {
        HoldingSummary held = Transmitable.getHolding(player);
        return InteractionResultHolder.pass(held.stack());
    }
}
