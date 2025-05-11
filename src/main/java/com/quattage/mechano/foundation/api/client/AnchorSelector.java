package com.quattage.mechano.foundation.api.client;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.annotation.Nullable;

import org.apache.commons.lang3.function.TriConsumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.landmarks.GridNode.Tracker;
import com.quattage.mechano.foundation.api.transmission.Transmitable;
import com.quattage.mechano.foundation.api.transmission.Transmitable.AnchorResponse;
import com.quattage.mechano.foundation.api.transmission.Transmitable.HoldingSummary;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifiable;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.quattage.mechano.foundation.mixin.client.RenderBuffersAccessor;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * This class stores a queue containing {@link AnchorPoint AnchorPoints} 
 * sorted by their distance from the player. These AnchorPoints are accumulated
 * during the BlockEntity rendering phase and processed at the end of the frame.
 * Note that this class's singleton and all of its associated behaviours should 
 * only be accessed on the client.
 * <p>
 * When AnchorPoints are processed, this class will automatically render hitboxes
 * and tooltip overlays. For additional control over tooltip contents and hitbox appearance, see
 * {@link Transmitable#onRenderTick}, {@link Transmitable#collectTooltipInfoAndResponse}
 * , and {@link PowerGridBlockEntity#collectTooltipInfo}
 * <p>
 * Additionally, the {@link AnchorSelector#selected <code>selected</code>} field always contains up-to-date
 * information about the AnchorPoint being targeted by the player, and the {@link PowerGridBlockEntity}
 * that anchor point belongs to. If the player isn't looking at an AnchorPoint, this field will be null.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber
public class AnchorSelector {


    @SubscribeEvent // tick the selector at the game's frame rate
    public static void onFrame(RenderFrameEvent.Post evt) {
        AnchorSelector.INSTANCE.tick(Minecraft.getInstance().player, evt.getPartialTick());
    }

    @SubscribeEvent // Injects custom highlight box
    public static void onRenderStageComplete(RenderLevelStageEvent evt) {

        if(evt.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) 
            return;

        AnchorSelector.INSTANCE.drawTrackedAnchors(
            evt.getCamera(), evt.getPoseStack(), 
            ((RenderBuffersAccessor)evt.getLevelRenderer())
                .mechano$getRenderBuffers()
                .bufferSource()
                .getBuffer(RenderType.lines()),
            evt.getPartialTick()
        );
    }

    @SubscribeEvent // Cancels the redundant select box if the player is looking at an AnchorPoint
    public static void onRenderHighlight(RenderHighlightEvent.Block evt) {
        evt.setCanceled(AnchorSelector.INSTANCE.hasSelection());
    }


    /**
     * Singleton instance of {@link AnchorSelector}
     */
    public static AnchorSelector INSTANCE = new AnchorSelector();


    // both may be null for a brief moment before the first tick is fired
    public @Nullable Active selected;
    protected @Nullable VectorHelper.Ray lookingRay; // TODO probably just use instance.hitresult
    protected float selectedTicks = 0;

    protected boolean lookedThisFrame = false;
    protected ArrayList<Component> currentTooltip = new ArrayList<>();;
    public Transmitable.HoldingSummary playerHands = new HoldingSummary(null, null, null, null);
    private final Queue<Active> trackedEntries = new PriorityQueue<>();

    // called by the event above
    protected void tick(LocalPlayer player, DeltaTracker deltas) {

        invalidateStaleTarget();
        if(player == null) { 
            reset(); 
            return; 
        }
        updateForCurrentFrame(player, deltas);
        if(trackedEntries.isEmpty()) {
            reset();
            return;
        }

        if(playerHands.isHoldingReleventItem()) {
            if(!playerHands.implementingItem().onRenderTick(player.level(), playerHands, deltas)) {
                reset();
                return;
            }
            findTargetAndRun(deltas, (pgbe, sel, distance) -> {
                pgbe.collectTooltipInfo(currentTooltip, sel.anchor, playerHands);
                sel.response = playerHands.implementingItem().collectTooltipInfoAndResponse(player.level(), currentTooltip, pgbe, sel.anchor, playerHands);
            });
        } else {
            findTargetAndRun(deltas, (pgbe, sel, distance) -> {
                pgbe.collectTooltipInfo(currentTooltip, sel.anchor, playerHands);
                sel.response = AnchorResponse.NONE; 
            });
        }

        // gets rid of any transient entires that may not have been polled out by previous calls
        // TODO figure out why this leaks sometimes
        trackedEntries.clear(); 
    }

    // called by the event above
    protected void drawTrackedAnchors(Camera camera, PoseStack matrixStack, VertexConsumer buffer, DeltaTracker delta) {

        if(hasSelection() && lookedThisFrame) {
            if(!(selected.isDisabled() || selected.response.hidesAnchor())) {
                if(AnchorResponse.indicatesSpecialDrawing(selected.response)) {
                    if(selectedTicks < 1) selectedTicks += delta.getGameTimeDeltaTicks() / 2;
                    selectedTicks = Math.min(1, selectedTicks);
                    selected.renderComplexAABB(selectedTicks, false);
                } else if(selectedTicks > 0) {
                    selectedTicks -= delta.getGameTimeDeltaTicks() / 2;
                    selectedTicks = Math.max(0, selectedTicks);
                    selected.renderComplexAABB(selectedTicks, false);
                } else {
                    selected.injectSimpleVanillaOutline(camera.getPosition(), matrixStack, buffer);
                    selectedTicks = 0;
                }
            } else if(AnchorResponse.indicatesSpecialDrawing(selected.response) && selectedTicks > 0) {
                selectedTicks -= delta.getGameTimeDeltaTicks() / 2;
                selectedTicks = Math.max(0, selectedTicks);
                selected.renderComplexAABB(selectedTicks, false);
            }
        }

        if(!playerHands.isHoldingReleventItem()) return;
        for(Active anchor : trackedEntries) {
            if(anchor == null || anchor.equals(selected) || anchor.isInvalid() || anchor.isDisabled() || anchor.response.hidesAnchor()) 
                continue;
            if(AnchorResponse.indicatesSpecialDrawing(anchor.response)) anchor.renderComplexAABB(1, false);
            else anchor.injectSimpleVanillaOutline(camera.getPosition(), matrixStack, buffer);
        }
    }

    /**
     * Resets this selector to its default state and clears its 
     * currently queued entries for this frame
     */
    public void reset() {
        lookedThisFrame = false;
        selectedTicks = 0;
        lookingRay = null;
        selected = null;
        currentTooltip = new ArrayList<>();
        trackedEntries.clear();
    }

    /**
     * An AnchorPoint is considered "selected" if the player is looking directly at it
     * within a reasonable distance.
     * @return <code>true</code> if this selector's internal raycast is intersecting 
     * an AnchorPoint
     */
    public boolean hasSelection() {
        return selected != null && (!selected.isInvalid());
    }

    /**
     * Each BlockEntity that contains AnchorPoints submits their AnchorPoints
     * during the BlockEntity rendering phase. If the client is rendering any
     * BlockEntities with AnchorPoints within the view frustum, they will all 
     * be accumulated here by the end of the frame.
     * @return <code>true</code> if the tracked AnchorPoints queue in this selector is not empty.
     */
    public boolean hasAnchorsNearby() {
        return trackedEntries.size() > 0 && playerHands.isHoldingReleventItem();
    }

    /**
     * Tells this AnchorSelector to track the provided AnchorPoint information
     * for evaluation at the end of the current render tick. This anchor will be 
     * cached until the end of the current frame's render cycle and then forgotten.
     * @param owner {@link PowerGridBlockEntity} that the provided AnchorPoint belongs to
     * @param anchor Anchor to track
     * @param distance Distance from the player to the anchor. Used for priority sorting when pulling AnchorPoints from the queue, 
     * so this distance value doesn't necessarily have to be coherent - you can just submit an abitrary number (like, for example, 0, 
     * if you want this anchor to be evaluated with the highest priority
     */
    public void track(PowerGridBlockEntity owner, AnchorPoint anchor, float distance) {
        if(owner == null || anchor == null || distance <= 0) return;
        trackedEntries.add(new Active(owner, anchor, distance));
    }

    // updates the player's held item, raycast, and tooltip information for this frame
    private void updateForCurrentFrame(LocalPlayer player, DeltaTracker delta) {
        this.playerHands = Transmitable.getHolding(player);
        float distance = (float)player.blockInteractionRange();
        this.lookingRay = VectorHelper.getLookingRay(player, delta.getGameTimeDeltaPartialTick(false), distance);
        this.currentTooltip = new ArrayList<>();
    }

    // resets the target if its source was removed from the world
    private void invalidateStaleTarget() {
        if(selected != null && !selected.anchor.existsInWorld(playerHands.player().level()))
            reset();
    }

    /**
     * Searches through nearby AnchorPoints and executes the provided consumer on the most relevent one.
     * If the player is looking directly at a nearby AnchorPoint, that AnchorPoint, its parent PGBE,
     * and the distance from the player will be passed to the provided consumer. The provided consumer
     * may not fire at all if the player isn't near any AnchorPoints or isn't targeting one directly.
     * This is used internally to handle tooltip aggregation and some basic event stuff.
     * @param cons Consumer that is executed when this 
     */
    private void findTargetAndRun(DeltaTracker delta, TriConsumer<PowerGridBlockEntity, AnchorSelector.Active, Float> cons) { 
        // TODO public access may be useful
        lookedThisFrame = false;
        while(!trackedEntries.isEmpty()) {
            final Active sel = trackedEntries.poll();
            if(sel == null || sel.isInvalid()) continue;
            if(!sel.anchor.isIntersecting(lookingRay)) continue;
            lookedThisFrame = true;
            selected = sel;
            cons.accept(sel.be, sel, sel.distance);
            break;
        }
        if(!lookedThisFrame) {
            if(hasSelection() && selectedTicks > 0) {
                selectedTicks -= delta.getGameTimeDeltaTicks() / 2;
                selected.renderComplexAABB(selectedTicks, false);
            } else reset();
        }
    }

    public boolean hasTooltip() {
        return !currentTooltip.isEmpty();
    }

    public boolean isSelected(NodeIdentifiable<?> id) {
        if(!hasSelection()) return false;
        return selected.anchor.equals(id);
    }

    @Override
    public String toString() {
        return playerHands + (hasSelection() ? ", and is targeting " + selected : " and has no target");
    }


    /**
     * An {@link AnchorPoint} container for comparing based on distance to the LocalPlayer
     * and sorting in a PriorityQueue
     */
    public static class Active implements Comparable<Active>, NodeIdentifiable<AnchorPoint> {

        public final PowerGridBlockEntity be; 
        private final AnchorPoint anchor;
        public final float distance;
        public AnchorResponse response;
        private VoxelShape highlightShape;

        public Active(PowerGridBlockEntity be, AnchorPoint anchor, float distance) {
            this.be = be;
            this.anchor = anchor;
            this.distance = distance;
            this.response = AnchorResponse.NONE;
            float size = anchor.getSize();
            highlightShape = Shapes.create(-size, -size, -size, size, size, size);
        }

        public boolean isDisabled() {
            return !anchor.isEnabled(

            );
        }

        public boolean isInvalid() {
            return be == null || anchor == null || response == null || distance <= 0;
        }

        @Override
        public int compareTo(Active o) {
            if(this.distance < o.distance) return 1;
            if(this.distance > o.distance) return -1;
            return 0;
        }

        @Override
        public final boolean equals(Object o) {
            if(!(o instanceof Active that)) return false;
            return this.anchor.equals(that.anchor);
        }

        @Override
        public int hashCode() {
            return this.anchor.hashCode();
        }

        /**
         * Renders this wrapped AnchorPoint's VoxelShape to the given consumer and stack
         * @param basis The base transformation offset of the matrix - Usually the camera's position
         * @param matrix
         * @param buffer
         * @return <code>true</code> if the outline was successfully rendered.
         */
        public boolean injectSimpleVanillaOutline(Vec3 basis, PoseStack matrix, VertexConsumer buffer) {

            Minecraft mc = Minecraft.getInstance();
            if(mc != null && !mc.level.getWorldBorder().isWithinBounds(basis)) return false;
            Vec3 shapePos = anchor.getRealPosition();

            matrix.pushPose();
            matrix.translate(shapePos.x - basis.x, shapePos.y - basis.y, shapePos.z - basis.z);
            PoseStack.Pose transform = matrix.last();

            highlightShape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {

                float xD = (float)(x2 - x1), yD = (float)(y2 - y1), zD = (float)(z2 - z1);
                float len = Mth.sqrt(xD * xD + yD * yD + zD * zD);
                xD /= len; yD /= len; zD /= len;

                buffer.addVertex(transform.pose(), (float)x1, (float)y1, (float)z1)
                    .setColor(0, 0, 0, 0.4f)
                    .setNormal(transform.copy(), xD, yD, zD);
                buffer.addVertex(transform.pose(), (float)x2, (float)y2, (float)z2)
                    .setColor(0, 0, 0, 0.4f)
                    .setNormal(transform.copy(), xD, yD, zD);
            });
            matrix.popPose();
            return true;
        }

        /**
         * Renders this wrapped AnchorPoint's AABB hitbox to the Create Outliner
         */
        public void renderComplexAABB(float ticks, boolean unique) {
            AABB visual = anchor.makeHitbox(false).inflate(anchor.getSize() * ticks);
            Color col = Color.BLACK.mixWith(new Color(77, 253, 182), ticks);
            Outliner.getInstance().showAABB(unique ? anchor.hashCode() : 0xFFFFFFF, visual)
                .disableCull()
                .disableLineNormals()
                .colored(col)
                .lineWidth(0.010f * ticks);
        }


        @Override
        public String toString() {
            return "[" + be.getBlockPos() + ", " + anchor + ", " +  response + "]";
        }

        @Override
        public AnchorPoint getValue() {
            return anchor;
        }

        @Override
        public BlockPos getPos() {
            return anchor.getPos();
        }

        @Override
        public CompoundTag writeTo(CompoundTag in) {
            Mechano.LOGGER.warn("Potential bad access - " + this + " (selected) was serialized to NBT!");
            return anchor.writeTo(in);
        }

        @Override
        public CompoundTag writeOnlyAddress(CompoundTag in) {
            Mechano.LOGGER.warn("Potential bad access - " + this + " (selected) was serialized to NBT!");
            return anchor.writeOnlyAddress(in);
        }

        @Override
        public Tracker makeTrackable() {
            throw new UnsupportedOperationException("Active selections aren't trackable!");
        }

        @Override
        public int getIndex() {
            return anchor.getIndex();
        }
    }
}