package com.quattage.mechano.foundation.api.client;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.MechanoClientEvents;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifiable;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Renders AnchorPoint information to Create's familiar goggle overlay.
 * Invoked in a custom context because AnchorPoints are not voxels, and as such cannot be acquired
 * using traditional means (with HitResult)
 * 
 * A large portion of this code, particularly the rendering parts, were copied directly from Create's own
 * GoggleOverlayRenderer class.
 * Copying it here was just to simplify the process of manually invoking this code in a custom context
 * for my purposes, without having to write a very unfriendly and bloated mixin.
 */
@OnlyIn(Dist.CLIENT)
public class AnchorGuiLayer {

	private static int hoverTicks = 0;
	private static @Nullable NodeIdentifiable lastTarget = null;


	public static void renderOverlay(GuiGraphics graphics, DeltaTracker deltas) {

		Minecraft mc = Minecraft.getInstance();
		if(!shouldRenderOverlay(mc)) return;

		if(!AnchorSelector.INSTANCE.isSelected(lastTarget)) { 
			lastTarget = AnchorSelector.INSTANCE.selected;
			hoverTicks = 0; 
		}

		if(hoverTicks < 100) hoverTicks++;

		// -----------------------------------------------------------------------
		// !! EVERYTHING FROM HERE DOWN IS BARELY MODIFIED FROM CREATE'S SOURCE !!
		// -----------------------------------------------------------------------

		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();

		int tooltipTextWidth = 0; 
		for (FormattedText textLine : AnchorSelector.INSTANCE.currentTooltip) {
			int textLineWidth = mc.font.width(textLine);
			if (textLineWidth > tooltipTextWidth)
				tooltipTextWidth = textLineWidth;
		}

		int tooltipHeight = 8;
		if (AnchorSelector.INSTANCE.currentTooltip.size() > 1) {
			tooltipHeight += 2;
			tooltipHeight += (AnchorSelector.INSTANCE.currentTooltip.size() - 1) * 10;
		}

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		CClient cfg = AllConfigs.client();
		int posX = width / 2 + cfg.overlayOffsetX.get();
		int posY = height / 2 + cfg.overlayOffsetY.get();

		posX = Math.min(posX, width - tooltipTextWidth - 20);
		posY = Math.min(posY, height - tooltipHeight - 20);

		float fade = Mth.clamp((hoverTicks + deltas.getGameTimeDeltaPartialTick(false)) / 24f, 0, 1);
		Boolean useCustom = cfg.overlayCustomColor.get();
		// TODO contextual coloring
		Color colorBackground = useCustom ? new Color(cfg.overlayBackgroundColor.get())
				: BoxElement.COLOR_VANILLA_BACKGROUND
				.scaleAlpha(.75f);
		Color colorBorderTop = useCustom ? new Color(cfg.overlayBorderColorTop.get())
				: BoxElement.COLOR_VANILLA_BORDER
				.getFirst();
		Color colorBorderBot = useCustom ? new Color(cfg.overlayBorderColorBot.get())
				: BoxElement.COLOR_VANILLA_BORDER
				.getSecond();

		if (fade < 1) {
			poseStack.translate(Math.pow(1 - fade, 3) * Math.signum(cfg.overlayOffsetX.get() + .5f) * 8, 0, 0);
			colorBackground.scaleAlpha(fade);
			colorBorderTop.scaleAlpha(fade);
			colorBorderBot.scaleAlpha(fade);
		}

		GuiGameElement.of(AnchorSelector.INSTANCE.selected.be.getBlockState().getBlock().asItem())
			.at(posX + 10, posY - 16, 450)
			.render(graphics);
		poseStack.popPose();

		RemovedGuiUtils.drawHoveringText(graphics, AnchorSelector.INSTANCE.currentTooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
			colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);
	}


	public static boolean shouldRenderOverlay(Minecraft mc) {

		if(MechanoClientEvents.shouldRenderOverlay(mc) 
			&& AnchorSelector.INSTANCE.hasSelection()
			&& AnchorSelector.INSTANCE.hasTooltip() 
			&& !Response.hidesAnchor(AnchorSelector.INSTANCE.selected.response)
			&& AnchorSelector.INSTANCE.lookedThisFrame)
				return true;

		hoverTicks = 0;
		lastTarget = null;
		return false;
	}
}