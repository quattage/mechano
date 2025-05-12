package com.quattage.mechano.foundation.api;

import com.quattage.mechano.MechanoDataAttachments;
import com.quattage.mechano.foundation.api.client.AnchorPoint;
import com.quattage.mechano.foundation.api.client.AnchorSelector;
import com.quattage.mechano.foundation.api.transmission.Transmitable;
import com.quattage.mechano.foundation.api.transmission.Transmitter;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public abstract class WireSpoolItem<T extends Transmitter> extends Item implements Transmitable<T> {

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
            if(!AnchorSelector.INSTANCE.selected.response.isSuccessful()) {
                // TODO send initial fail message to selector and GuiLayer
                return InteractionResultHolder.fail(stack);
            }
            stack.set(MechanoDataAttachments.ADDRESS_COMPONENT, sel.strip());
            return InteractionResultHolder.success(stack);
        }

        AnchorPoint previous = AnchorPoint.retrieve(level, stack);
        if(previous == null || AnchorSelector.INSTANCE.isSelected(previous))
            return InteractionResultHolder.pass(stack);

        GlobalClientGrid client = SidedGridDispatcher.client(player);
        LinkResponse result = client.requestLink(previous, AnchorSelector.INSTANCE.selected.getValue(), getTransmitterType());
        if(result.shouldBail()) stack.remove(MechanoDataAttachments.ADDRESS_COMPONENT);

        if(result.isSuccessful()) return InteractionResultHolder.success(stack);
        return InteractionResultHolder.fail(stack);
    }


    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
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
