package com.quattage.mechano.foundation.api;

import com.quattage.mechano.MechanoDataAttachments;
import com.quattage.mechano.foundation.api.landmark.client.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.client.AnchorSelector;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.transmitter.Transmitable;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.blockEntity.renderer.PowerGridBlockEntityRenderer;
import com.quattage.mechano.foundation.helper.VectorHelper;

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
            stack.set(MechanoDataAttachments.ADDRESS_COMPONENT, sel.strip());
            return InteractionResultHolder.success(stack);
        }

        AnchorPoint previous = AnchorPoint.retrieve(level, stack);
        if(previous == null || AnchorSelector.INSTANCE.isSelected(previous))
            return InteractionResultHolder.pass(stack);

        GlobalClientGrid client = SidedGridDispatcher.client(player);
        Response<?> result = client.requestLink(previous, AnchorSelector.INSTANCE.selected.anchor, getTransmitterType());
        if(Response.shouldBail(result)) stack.remove(MechanoDataAttachments.ADDRESS_COMPONENT);
        if(result.indicatesSuccess()) return InteractionResultHolder.success(stack);
        return InteractionResultHolder.fail(stack);
    }


    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, world, entity, slotId, isSelected);
        if(!isSelected) return;
        if(!world.isClientSide) return;

        PowerGridBlockEntityRenderer.endPos = VectorHelper.getLookingRay((Player)entity, 0, 10f).end;

        AnchorPoint previous = AnchorPoint.retrieve(world, stack);
        if(previous == null) 
            stack.remove(MechanoDataAttachments.ADDRESS_COMPONENT);
        else if(!previous.existsInWorld(world))
            stack.remove(MechanoDataAttachments.ADDRESS_COMPONENT);

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
