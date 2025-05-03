package com.quattage.mechano.foundation.api.grid.client;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Consumer;

import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.grid.landmarks.NodeIdentifier;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.quattage.mechano.foundation.mixin.client.RenderBuffersAccessor;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.grid.ProtocolTransferable;
import com.quattage.mechano.foundation.api.grid.ProtocolTransferable.AnchorResponse;
import com.quattage.mechano.foundation.api.grid.ProtocolTransferable.HoldingSummary;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Stores an array of {@link AnchorPoint} objects
 * and provides helpers for managing and accessing them.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber
public class AnchorPoints {

    /**
     * Wraps information about the anchor currently being targeted by the player
     */
    public static final Selector SELECTOR = new Selector();
    public static final AnchorPoints EMPTY = new AnchorPoints(new AnchorPoint[0]);
    
    private final AnchorPoint[] anchors;

    public static AnchorPoints.Builder begin(BlockEntity parent) {
        return new Builder(parent);
    }

    private AnchorPoints(AnchorPoint[] anchors) {
        this.anchors = anchors;
    }

    public void forEach(Consumer<AnchorPoint> action) {
        for(int x = 0; x < anchors.length; x++) {
            action.accept(anchors[x]);
        }
    }

    /**
     * @return The size of this AnchorPoints array
     */
    public int size() {
        return anchors.length;
    }

    /**
     * Updates the location and hitbox of all {@link AnchorPoint} objects
     * in this AnchorPoints array to reflect the data contained within the given 
     * BlockState
     * @param state state to extract orientation data from
     */
    public void updateOrientation(BlockState state) {
        CombinedOrientation dir = DirectionTransformer.extract(state);
        forEach(anchor -> {
            anchor.updateOrientation(dir);
        });
    }

    @Override
    public String toString() {
        if(anchors.length == 0) return "AnchorPoints[\n\tEMPTY\n]";
        String output = "AnchorPoints[\n";
        for(int x = 0; x < anchors.length; x++)
            output += anchors[x] == null ? "\tnull,\n" : ("\t" + anchors[x].toString() + ", \n");
        return output + "]";
    }
    

    public boolean contains(BlockPos pos, AnchorPoint anchor) {
        return anchor.isLocatedAt(pos) && anchor.getIndex() > 0 && anchor.getIndex() < size();
    }



    @SubscribeEvent // tick the selector at the game's frame rate
    public static void onFrame(RenderFrameEvent.Post evt) {
        AnchorPoints.SELECTOR.tick(Minecraft.getInstance().player, evt.getPartialTick());
    }


    @SubscribeEvent // Injects custom highlight box
    public static void onRenderStageComplete(RenderLevelStageEvent evt) {

        if(evt.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) 
            return;

        AnchorPoints.SELECTOR.drawTrackedAnchors(
            evt.getCamera(), evt.getPoseStack(), 
            ((RenderBuffersAccessor)evt.getLevelRenderer())
                .mechano$getRenderBuffers()
                .bufferSource()
                .getBuffer(RenderType.lines())
        );
    }

    @SubscribeEvent // Cancels the redundant select box if the player is looking at an AnchorPoint
    public static void onRenderHighlight(RenderHighlightEvent.Block evt) {
        evt.setCanceled(AnchorPoints.SELECTOR.hasSelection());
    }

    /**
     * Accumulates {@link AnchorPoint AnchorPoints} belonging to BlockEntities within the 
     * client's view frustum, and processes them at the end of the frame.
     */
    public static class Selector {

        // both may be null for a brief moment before the first tick is fired
        public @Nullable SelectedAnchor selected;
        public @Nullable VectorHelper.Ray lookingRay; // TODO probably just use instance.hitresult
        
        private ArrayList<Component> tooltip = new ArrayList<>();;
        public ProtocolTransferable.HoldingSummary playerHands = new HoldingSummary(null, null, null, null);
        private final Queue<SelectedAnchor> trackedEntries = new PriorityQueue<>();

        // Called by the event above
        protected void tick(LocalPlayer player, DeltaTracker delta) {

            if(player == null) return;

            invalidateStaleTarget();
            updateForCurrentFrame(player, delta);

            if(trackedEntries.isEmpty()) return;

            if(playerHands.isHoldingReleventItem()) {
                if(!playerHands.protocol().onRenderTick(player.level(), playerHands, delta)) return;
                findTargetAndRun((pgbe, anchor, distance) -> {
                    pgbe.collectTooltipInfo(tooltip, anchor, playerHands);
                    selected.response = playerHands.protocol().collectTooltipInfoAndResponse(player.level(), tooltip, pgbe, anchor, playerHands);
                });
            } else {
                findTargetAndRun((pgbe, anchor, distance) -> {
                    pgbe.collectTooltipInfo(tooltip, anchor, playerHands);
                    selected.response = AnchorResponse.NONE; 
                });
            }
        }

        // called by the event above
        protected void drawTrackedAnchors(Camera camera, PoseStack matrixStack, VertexConsumer buffer) {

            if(!playerHands.isHoldingReleventItem()) {
                if(!hasSelection() || selected.isDisabled() || selected.response.hidesAnchor()) return;
                if(AnchorResponse.indicatesSpecialDrawing(selected.response)) selected.renderComplexAABB();
                else selected.injectSimpleVanillaOutline(camera.getPosition(), matrixStack, buffer);
                return;
            }

            for(SelectedAnchor anchor : trackedEntries) {
                if(anchor == null || anchor.isInvalid() || anchor.isDisabled() || anchor.response.hidesAnchor()) continue;
                if(AnchorResponse.indicatesSpecialDrawing(anchor.response)) anchor.renderComplexAABB();
                else anchor.injectSimpleVanillaOutline(camera.getPosition(), matrixStack, buffer);
            }
            
            Mechano.LOGGER.info("Selection: " + selected);
        }

        /**
         * Resets this selector to its default state and clears its 
         * currently queued entries for this frame
         */
        public void reset() {
            lookingRay = null;
            selected = null;
            tooltip = new ArrayList<>();
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
            trackedEntries.add(new SelectedAnchor(owner, anchor, distance));
        }

        // updates the player's held item, raycast, and tooltip information for this frame
        private void updateForCurrentFrame(LocalPlayer player, DeltaTracker delta) {
            this.playerHands = ProtocolTransferable.getHolding(player);
            this.lookingRay = VectorHelper.getLookingRay(player, delta.getGameTimeDeltaPartialTick(false), 20);
            this.tooltip = new ArrayList<>();
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
        private void findTargetAndRun(TriConsumer<PowerGridBlockEntity, AnchorPoint, Float> cons) { 
            // TODO public access may be useful
            boolean acquiredTarget = false;
            while(!trackedEntries.isEmpty()) {
                final SelectedAnchor sel = trackedEntries.poll();
                if(sel == null || sel.isInvalid()) continue;
                if(!sel.anchor.isIntersecting(lookingRay)) continue;
                acquiredTarget = true;
                selected = sel;
                cons.accept(sel.be, sel.anchor, sel.distance);
                break;
            }
            if(!acquiredTarget && selected != null)
                reset();
        }
    }











    /**
     * An {@link AnchorPoint} container for comparing based on distance to the LocalPlayer
     * and sorting in a PriorityQueue
     */
    private static class SelectedAnchor implements Comparable<SelectedAnchor> {

        public final PowerGridBlockEntity be; 
        public final AnchorPoint anchor;
        public final float distance;
        public AnchorResponse response;

        public SelectedAnchor(PowerGridBlockEntity be, AnchorPoint anchor, float distance) {
            this.be = be;
            this.anchor = anchor;
            this.distance = distance;
            this.response = AnchorResponse.NONE;
        }


        public boolean isDisabled() {
            return !anchor.isEnabled();
        }

        public boolean isInvalid() {
            return be == null || anchor == null || response == null || distance <= 0;
        }

        @Override
        public int compareTo(SelectedAnchor o) {
            if(this.distance < o.distance) return 1;
            if(this.distance > o.distance) return -1;
            return 0;
        }

        @Override
        public final boolean equals(Object o) {
            if(!(o instanceof SelectedAnchor that)) return false;
            return this.anchor.equals(that.anchor);
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
            if(anchor.shape == null || anchor.hitbox == null) anchor.rebuildHitbox();

            matrix.pushPose();
            matrix.translate(shapePos.x - basis.x, shapePos.y - basis.y, shapePos.z - basis.z);
            PoseStack.Pose transform = matrix.last();

            anchor.shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {

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
        public void renderComplexAABB() {
            Outliner.getInstance().showAABB(anchor.hashCode(), anchor.hitbox)
                .disableLineNormals()
                .colored(new Color(0, 255, 0))
                .lineWidth(0.003f);
        }


        @Override
        public String toString() {
            return "[" + be.getBlockPos() + ", " + anchor + ", " +  response + "]";
        }
    }























    /**
     * Fluentish builder for creating AnchorPoint arrays.
     */
    public static class Builder {
        
        private ObjectArrayList<AnchorPoint.Builder> anchors = new ObjectArrayList<>(NodeIdentifier.MAX_OCCUPANCY);
        private BlockEntity parent;

        public Builder(BlockEntity parent) {
            this.parent = parent;
        }

        protected void add(AnchorPoint.Builder newBuilder) {
            anchors.add(newBuilder);
        }
        
        public AnchorPoint.Builder add() {
            return new AnchorPoint.Builder(this);
        }

        public AnchorPoints confirm(BlockPos pos) {
            anchors.trim();
            if(anchors.isEmpty()) {
                Mechano.LOGGER.error("Error building AnchorPoints for " + parent + " - The resulting AnchorPoints is empty!");
                return AnchorPoints.EMPTY;
            }
            AnchorPoint[] builtAnchors = new AnchorPoint[anchors.size()];
            for(int x = 0; x < builtAnchors.length; x++) {
                if(x >= NodeIdentifier.MAX_OCCUPANCY) break;
                builtAnchors[x] = anchors.get(x).instantiate(pos, x);
            }
            return new AnchorPoints(builtAnchors);
        }
    }
}
