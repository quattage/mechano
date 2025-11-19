package com.quattage.mechano.api;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.MechanoClientEvents;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.CircuitComponentProvider;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;
import com.quattage.mechano.api.grid.topology.ancillary.WireJack;
import com.quattage.mechano.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.numeric.VectorOperations;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.foundation.gui.RemovedGuiUtils;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;

import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JackSelector {

    private static final JackSelector INSTANCE = new JackSelector();
    public static JackSelector getInstance() {
        return JackSelector.INSTANCE;
    }
    
    private final TargetAncillary selected = new TargetAncillary();
    private final Queue<TargetAncillary> nearbyJoints = new PriorityQueue<>();
    private @Nullable WireJack previousTarget = null;
    private ArrayList<Component> tooltip = new ArrayList<>();

    private boolean lookedThisFrame = false;
    private boolean shouldShowAllNearby = false;
    private float hoverTicks = 0;

    public void tick(LocalPlayer lp, DeltaTracker deltas) {
        if(lp == null || deltas == null || nearbyJoints.isEmpty()) {
            reset();
            return;
        }

        VectorOperations.Ray lookingRay = VectorOperations.getLookingRay(
            lp, deltas.getGameTimeDeltaPartialTick(false), (float)lp.blockInteractionRange());
        HoldingSummary hands = getHolding(lp);
        if(lookingRay == null || hands == null) {
            reset();
            return;
        }
        updateClosest((ClientLevel)lp.level(), lookingRay, hands.get(), deltas);
        if(!hands.isHoldingReleventItem()) {
            shouldShowAllNearby = false;
            nearbyJoints.clear();
            return;
        }
        shouldShowAllNearby = true;
        CircuitComponentProvider prov = hands.get();
        accumulateTooltip(lp, prov);
        accumulateTooltip(lp, selected.get());
        nearbyJoints.clear();
    }

    /**
     * Appends a {@link AncillaryJack} to this tracker's queue. Queued trackers
     * will be evaluated based on how close they are to the player's looking raycast.
     * @param tracker The current LocalPlayer (can be null)
     * @param source The griddable that owns <code>joint</code>
     * @param joint the joint to be added
     */
    public void trackForThisFrame(@Nullable LocalPlayer tracker, Griddable source, AncillaryJack joint) {
        if(tracker == null) {
            tracker = Minecraft.getInstance().player;
            if(tracker == null)
                throw new IllegalStateException("JackSelector track called outside of permissible context!");
        }
        Vector3d pos = source.getPositionOf(joint);
        float distance = (float)pos.distance(tracker.position().x, tracker.position().y, tracker.position().z);
        if(distance > tracker.getAttributes().getValue(Attributes.ENTITY_INTERACTION_RANGE) * 1.5f) return;
        TargetAncillary potentialTarget = new TargetAncillary(joint, distance);
        nearbyJoints.add(potentialTarget);
    }

    /**
     * Adds the pertinent information regarding the player's current target and held spool
     * to the tooltip for this frame
     * @param lp Local player
     * @param obj Object to pull tooltip info from (usually just the held spool)
     */
    public void accumulateTooltip(@Nullable LocalPlayer lp, @Nullable Object obj) {
        if(lp == null || obj == null) return;
        if(obj instanceof IHaveGoggleInformation tt && GogglesItem.isWearingGoggles(lp))
            tt.addToGoggleTooltip(tooltip, lp.isShiftKeyDown());
        if(obj instanceof IHaveHoveringInformation tt)
            tt.addToTooltip(tooltip, lp.isShiftKeyDown());
    }

    private void updateClosest(ClientLevel world, VectorOperations.Ray ray, CircuitComponentProvider prov, DeltaTracker deltas) { 
        lookedThisFrame = false;
        while(!nearbyJoints.isEmpty()) {
            final TargetAncillary sel = nearbyJoints.poll();
            if(sel == null || !sel.isVisible()) continue;
            if(!sel.get().isIntersecting(sel.target.getSource().getSourcePos(), ray)) continue;
            lookedThisFrame = true;
            if(prov == null) sel.updateResponse(GridResponse.NONE);
            else sel.updateResponse(prov.evaluateTarget(world, sel.get()));
            this.selected.setTo(sel);
            break;
        }
        if(!lookedThisFrame) {
            if(selected.exists() && hoverTicks > 0) {
                hoverTicks -= deltas.getGameTimeDeltaTicks() * 0.5;
                selected.get().drawToOutliner(selected.target.getSource().getSourcePos(), selected.getColor(), hoverTicks, deltas.getGameTimeDeltaPartialTick(false));
            } else reset();
        }
    }

    /**
     * Draws every {@link AncillaryJack} in the queue to either Create's {@link Outliner}
     * or to the provided <code>buffer</code> depending on the context.
     * @param camera The current rendering camera
     * @param matrixStack the {@link PoseStack} accessible from the current rendering context.
     * @param buffer the {@link VertexConsumer} that will receive vertices.
     * @param deltas delta time tracker for the local instance
     * @see #drawSelectedToOutliner
     * @see #drawSelectedToStack
     */
    public void draw(Camera camera, PoseStack matrixStack, VertexConsumer buffer, DeltaTracker deltas) {
        if(selected.exists() && lookedThisFrame) {
            if(selected.isVisible()) {
                if(selected.getResponse().getVisibility().isHighlighted()) {
                    if(hoverTicks < 1) hoverTicks += deltas.getGameTimeDeltaTicks() * 0.5;
                    hoverTicks = Math.min(1, hoverTicks);
                    selected.drawToOutliner(hoverTicks, deltas);
                } else if(hoverTicks > 0) {
                    hoverTicks -= deltas.getGameTimeDeltaTicks() * 0.5;
                    hoverTicks = Math.max(0, hoverTicks);
                    selected.drawToOutliner(hoverTicks, deltas);
                } else {
                    selected.drawToBuffer(camera, matrixStack, buffer, deltas);
                    hoverTicks = 0;
                }
            } else if(selected.getResponse().getVisibility().isHighlighted() && hoverTicks > 0) {
                hoverTicks -= deltas.getGameTimeDeltaTicks() * 0.5;
                hoverTicks = Math.max(0, hoverTicks);
                selected.drawToBuffer(camera, matrixStack, buffer, deltas);
            }
        }
        if(shouldShowAllNearby)
            drawAllNearby(camera, matrixStack, buffer, deltas);
    }

    private void drawAllNearby(Camera camera, PoseStack matrixStack, VertexConsumer buffer, DeltaTracker deltas) {
        for(TargetAncillary joint : nearbyJoints) {
            if(joint == null || joint.equals(selected)) continue;
            joint.drawToBuffer(camera, matrixStack, buffer, deltas);
        }
    }

    private HoldingSummary getHolding(Player player) {
        if(player == null) throw new NullPointerException("Couldn't instantiate a HoldingSummary - Player is null!");
        ItemStack stack = player.getMainHandItem();
        if(stack.getItem() instanceof CircuitComponentProvider transmitterItem)
            return new HoldingSummary(player, InteractionHand.MAIN_HAND, stack, transmitterItem);
        ItemStack offStack = player.getOffhandItem();
        if(offStack.getItem() instanceof CircuitComponentProvider transmitterItem)
            return new HoldingSummary(player, InteractionHand.OFF_HAND, offStack, transmitterItem);
        return new HoldingSummary(player, InteractionHand.MAIN_HAND, stack, null);
    }

    private void reset() {
        lookedThisFrame = false;
        shouldShowAllNearby = false;
        hoverTicks = 0;
        selected.reset();
        nearbyJoints.clear();
    }

    public ItemStack getHeldCircuitProvider(Player player) {
        HoldingSummary hands = getHolding(player);
        return hands.stack == null ? ItemStack.EMPTY : hands.stack;
    }


    public void renderOverlay(Minecraft mc, GuiGraphics graphics, DeltaTracker deltas) {
        if(!shouldRenderOverlay(mc) || !selected.exists()) return;

        float hoverTicks = this.hoverTicks * 100;
        PoseStack poseStack = graphics.pose();
		poseStack.pushPose();

		int tooltipTextWidth = 0; 
		for(FormattedText textLine : tooltip) {
			int textLineWidth = mc.font.width(textLine);
			if(textLineWidth > tooltipTextWidth)
				tooltipTextWidth = textLineWidth;
		}

		int tooltipHeight = 8;
		if(tooltip.size() > 1) {
			tooltipHeight += 2;
			tooltipHeight += (tooltip.size() - 1) * 10;
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

		if(fade < 1) {
			poseStack.translate(Math.pow(1 - fade, 3) * Math.signum(cfg.overlayOffsetX.get() + 0.5f) * 8f, 0f, 0f);
			colorBackground.scaleAlpha(fade);
			colorBorderTop.scaleAlpha(fade);
			colorBorderBot.scaleAlpha(fade);
		}

        selected.get().drawGUILabel(tooltip, posX, posY, graphics);
		poseStack.popPose();
		RemovedGuiUtils.drawHoveringText(graphics, tooltip, posX, posY, width, height, -1, colorBackground.getRGB(),
			colorBorderTop.getRGB(), colorBorderBot.getRGB(), mc.font);
    }

    private boolean shouldRenderOverlay(@Nullable Minecraft mc) {
        return mc != null &&
            MechanoClientEvents.shouldRenderOverlay(mc)
			&& (selected != null && selected.isVisible())
			&& (tooltip != null && tooltip.size() > 0)
			&& lookedThisFrame;
    }

    public boolean hasSelection() {
        return selected != null && selected.exists() && selected.isVisible();
    }

    public @Nullable AncillaryJack target() {
        return hasSelection() ? selected.get() : null;
    }

    protected static class TargetAncillary implements Comparable<TargetAncillary> {

        private AncillaryJack target = null;
        private GridResponse response = GridResponse.FAIL_GENERIC;
        private float distanceToPlayer = 0;

        protected TargetAncillary() {}

        protected TargetAncillary(AncillaryJack target, float distanceToPlayer) {
            this.target = target;
            this.distanceToPlayer = distanceToPlayer;
        }

        protected void setTo(TargetAncillary other) {
            this.target = other.target;
            this.response = other.response;
            this.distanceToPlayer = other.distanceToPlayer;
        }

        protected void updateResponse(GridResponse response) {
            this.response = response;
        }

        private void reset() {
            this.target = null;
            this.response = GridResponse.FAIL_GENERIC;
        }

        public boolean is(WireJack jack) {
            return target == jack;
        }

        public boolean exists() {
            return target != null;
        }

        public boolean isVisible() {
            return target != null && target.isVisible() && response.getVisibility().isVisible();
        }

        public Color getColor() {
            if(target == null || target.getSource() == null) return response.getVisibility().getColor();
            return response.getVisibility().getColor(target.getSource().getBlockPos());
        }

        public @Nullable AncillaryJack get() {
            return target;
        }

        public @Nullable GridResponse getResponse() {
            return response;
        }

        @Override
        public int compareTo(TargetAncillary that) {
            return this.distanceToPlayer > that.distanceToPlayer ? 1 : (this.distanceToPlayer < that.distanceToPlayer ? -1 : 0);
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == this) return true;
            if(!(obj instanceof TargetAncillary that)) return false;
            return this.target == that.target;
        }

        @Override
        public String toString() {
            if(target == null) return "Target(null)";
            return "Target(" + target + ", " + response + ")";
        }


    /**
     * Draws this{@link AncillaryJack} to Create's {@link Outliner}
     * @see #drawSelectedToBuffer
     */
    public void drawToOutliner(float hoverTicks, DeltaTracker deltas) {
        target.drawToOutliner(target.getSource().getSourcePos(), getColor(), hoverTicks, deltas.getGameTimeDeltaPartialTick(false));
    }

    /**
     * Draws this {@link AncillaryJack} to an arbitrary vertex buffer.
     * The outline drawn by this method will resemble Minecraft's
     * vanilla voxel selection box.
     * @see #drawSelectedToOutliner
     */
    public void drawToBuffer(Camera camera, PoseStack matrixStack, VertexConsumer buffer, DeltaTracker deltas) {
        target.drawToBuffer(target.getSource().getSourcePos(), camera.getPosition(), matrixStack, buffer, deltas.getGameTimeDeltaPartialTick(false));
    }
    }

    protected static record HoldingSummary(Player player, InteractionHand hand, ItemStack stack, CircuitComponentProvider obj) {
        public CircuitComponentProvider get() { return obj; }
        public boolean isHoldingReleventItem() { return player != null && hand != null && obj != null && stack != null; }
        @Override public final String toString() {
            return "'" + player.getName().getString() + "'' is holding '" + obj + "' in their (" + hand + ")";
        }
    }
}
