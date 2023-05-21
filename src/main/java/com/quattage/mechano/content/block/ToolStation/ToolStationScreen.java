package com.quattage.mechano.content.block.ToolStation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.container.AbstractSimiContainerScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ToolStationScreen extends AbstractSimiContainerScreen<ToolStationContainer> {

    public ToolStationScreen(ToolStationContainer container, Inventory inventory, Component title) {
        super(container, inventory, title);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        // nothing for now
    }

    
    
}
