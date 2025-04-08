package com.quattage.mechano.foundation.electricity.rendering;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.foundation.block.anchor.AnchorEntry;
import com.quattage.mechano.foundation.electricity.WireSpool;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

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
public class AnchorOverlayRenderer {

	public static final IGuiOverlay FUNC_INSTANCE = AnchorOverlayRenderer::renderOverlay;

	private static int hoverTicks = 0;
	private static GID targetedAnchor = null;

	public static void renderOverlay(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height) {

		Minecraft mc = Minecraft.getInstance();
		if(mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR)
			return;

		// no special behaviors are needed during the sanity check phase since we're already keeping 
		// the selected anchor up to date in the WireAnchorSelectionManager.
		AnchorEntry entry = MechanoClient.ANCHOR_SELECTOR.getSelectedEntry();

		if(!AnchorEntry.isValid(entry)) {
			hoverTicks = 0;
			return;
		}

		if(!entry.get().getID().equals(targetedAnchor)) {
			hoverTicks = 0;
			targetedAnchor = entry.get().getID();
		} else hoverTicks++;

		List<Component> tooltip = new ArrayList<>();
		entry.get().addTooltip(
			tooltip, 
			entry.getParent(), 
			mc.player.isCrouching(), 
			GogglesItem.isWearingGoggles(mc.player), 
			WireSpool.getHeldByPlayer(mc.player)
		);

		if(tooltip.isEmpty()) {
			hoverTicks = 0;			
			return;
		}

		PoseStack poseStack = graphics.pose();
		poseStack.pushPose();

		int tooltipTextWidth = 0;
		for (FormattedText textLine : tooltip) {
			int textLineWidth = mc.font.width(textLine);
			if (textLineWidth > tooltipTextWidth)
				tooltipTextWidth = textLineWidth;
		}

		int tooltipHeight = 8;
		if (tooltip.size() > 1) {
			tooltipHeight += 2;
			tooltipHeight += (tooltip.size() - 1) * 10;
		}


		// ------------------------------------------------------------------
		// !! EVERYTHING FROM HERE DOWN IS UNMODIFIED FROM CREATE'S SOURCE !!
		// ------------------------------------------------------------------

		CClient cfg = AllConfigs.client();
		int posX = width / 2 + cfg.overlayOffsetX.get();
		int posY = height / 2 + cfg.overlayOffsetY.get();

		posX = Math.min(posX, width - tooltipTextWidth - 20);
		posY = Math.min(posY, height - tooltipHeight - 20);

		float fade = Mth.clamp((hoverTicks + partialTicks) / 24f, 0, 1);
		Boolean useCustom = cfg.overlayCustomColor.get();
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

		RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
			colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);

		GuiGameElement.of(entry.getParent().getBlockState().getBlock().asItem())
			.at(posX + 10, posY - 16, 450)
			.render(graphics);
		poseStack.popPose();
	}
}