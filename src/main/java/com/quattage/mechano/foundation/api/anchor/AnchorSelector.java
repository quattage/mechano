package com.quattage.mechano.foundation.api.anchor;

import java.util.ArrayList;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;

import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.Griddable.Visual;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.UpdateResponse;
import com.quattage.mechano.foundation.api.transmitter.Transmitable;
import com.quattage.mechano.foundation.api.transmitter.Transmitable.HoldingSummary;
import com.quattage.mechano.foundation.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

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
 * , and {@link GriddableBlockEntity#collectTooltipInfo}
 * <p>
 * Additionally, the {@link AnchorSelector#selected <code>selected</code>} field always contains up-to-date
 * information about the AnchorPoint being targeted by the player, and the {@link GriddableBlockEntity}
 * that anchor point belongs to. If the player isn't looking at an AnchorPoint, this field will be null.
 */
@OnlyIn(Dist.CLIENT)
public class AnchorSelector {

    /**
     * Singleton instance of {@link AnchorSelector}
     */
    public static AnchorSelector INSTANCE = new AnchorSelector();

    // both may be null for a brief moment before the first tick is fired
    @Nullable public Active selected;
    @Nullable private VectorHelper.Ray lookingRay; // TODO instance.hitresult with additional clipping?
    protected float selectedTicks = 0;

    private boolean lookedThisFrame = false;
    private ArrayList<Component> currentTooltip = new ArrayList<>();;
    private Transmitable.HoldingSummary playerHands = new HoldingSummary(null, null, null, null);
    private final Queue<Active> trackedEntries = new PriorityQueue<>();

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

    public AnchorPoint getSelected() {
        return selected == null ? null : selected.anchor;
    }

    public boolean hasSelection() {
        return getSelected() != null;
    }

    public boolean isSelected(GridUUID id) {
        if(id == null || !hasSelection()) return false;
        return selected.anchor.getAddress().equals(id);
    }

    public boolean isSelected(AnchorPoint anchor) {
        if(anchor == null || !hasSelection()) return false;
        return selected.anchor.getAddress().equals(anchor.getAddress());
    }

    protected boolean isSelected(Active active) {
        if(active == null || !hasSelection()) return false;
        return selected.anchor.getAddress().equals(active.anchor.getAddress());
    }

    public boolean isSelectedGood() {
        return hasSelection() && selected.response.indicatesCompletion();
    }

    public UpdateResponse getSelectedResponse() {
        return hasSelection() ? selected.response : UpdateResponse.NONE;
    }

    public boolean hasTooltip() {
        return currentTooltip != null && !currentTooltip.isEmpty();
    }

    public @Nullable Visual getSelectedVisual() {
        return hasSelection() ? selected.points.getVisual() : null;
    }

    public ArrayList<Component> getTooltip() {
        return currentTooltip;
    }

    public float getSelectedTicks() {
        return selectedTicks;
    }

    public boolean hasLookedThisFrame() {
        return lookedThisFrame;
    }

    public @Nullable VectorHelper.Ray getLookingRay() {
        return lookingRay;
    }

    public Transmitable.HoldingSummary getPlayerHandContents() {
        return playerHands;
    }

    public ItemStack getHeldStack() {
        return playerHands == null ? null : playerHands.stack();
    }

    /**
     * Tells this AnchorSelector to track the provided AnchorPoint information
     * for evaluation at the end of the current render tick. This anchor will be 
     * cached until the end of the current frame's render cycle and then forgotten.
     * @param owner {@link GriddableBlockEntity} that the provided AnchorPoint belongs to
     * @param anchor Anchor to track
     * @param distance Distance from the player to the anchor. Used for priority sorting when pulling AnchorPoints from the queue, 
     * so this distance value doesn't necessarily have to be coherent - you can just submit an abitrary number (like, for example, 0, 
     * if you want this anchor to be evaluated with the highest priority
     */
    public void trackForThisFrame(Griddable<?> points, AnchorPoint anchor, float distance) {
        if(points == null || anchor == null || distance <= 0) return;
        trackedEntries.add(new Active(points, anchor, anchor.getAddress(), distance));
    }




    public void tick(LocalPlayer player, DeltaTracker deltas) {
        if(selected != null && !selected.anchor.existsIn(playerHands.player().level()))
            resetCompletely();
        if(player == null) { 
            resetCompletely(); 
            return; 
        }
        this.lookingRay = VectorHelper.getLookingRay(player, deltas.getGameTimeDeltaPartialTick(false), (float)player.blockInteractionRange());
        this.playerHands = Transmitable.getHolding(player);
        this.currentTooltip = new ArrayList<>();
        if(trackedEntries.isEmpty()) {
            resetCompletely();
            return;
        }
        if(playerHands.isHoldingReleventItem()) {
            if(!playerHands.implementingItem().onRenderTick(player.level(), playerHands, deltas)) {
                resetCompletely();
                return;
            }
            findTargetAndRun(player.level(), deltas, (points, sel, distance) -> {
                points.writeTooltip(currentTooltip, playerHands, sel.anchor);
                sel.response = playerHands.implementingItem().collectTooltipInfoAndResponse((ClientLevel)player.level(), currentTooltip, points, sel.anchor, playerHands);
            });
        } else {
            findTargetAndRun(player.level(), deltas, (points, sel, distance) -> {
                points.writeTooltip(currentTooltip, playerHands, sel.anchor);
                sel.response = UpdateResponse.NONE;
            });
        }
        trackedEntries.clear(); 
    }

    public void drawTrackedAnchors(Camera camera, PoseStack matrixStack, VertexConsumer buffer, DeltaTracker delta) {

        if(hasSelection() && lookedThisFrame) {
            if(!selected.anchor.isEnabled()) {
                if(selected.response.showsTarget()) {
                    if(selected.response.indicatesCompletion()) {
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
                }
            } else if(selectedTicks > 0) {
                selectedTicks -= delta.getGameTimeDeltaTicks() / 2;
                selectedTicks = Math.max(0, selectedTicks);
                selected.renderComplexAABB(selectedTicks, false);
            }
        }

        if(!playerHands.isHoldingReleventItem()) return;
        for(Active entry : trackedEntries) {
            if(entry == null || entry.equals(selected) 
                || entry.anchor.isEnabled() || !entry.response.showsTarget()) 
                    continue;
            entry.injectSimpleVanillaOutline(camera.getPosition(), matrixStack, buffer);
        }
    }

    /**
     * Resets this selector to its default state and clears its 
     * currently queued entries for this frame
     */
    public void resetCompletely() {
        lookedThisFrame = false;
        selectedTicks = 0;
        selected = null;
        currentTooltip = new ArrayList<>();
        trackedEntries.clear();
    }




    /**
     * Searches through nearby AnchorPoints and executes the provided consumer on the most relevent one.
     * If the player is looking directly at a nearby AnchorPoint, that AnchorPoint, its parent points,
     * and the distance from the player will be passed to the provided consumer. The provided consumer
     * may not fire at all if the player isn't near any AnchorPoints or isn't targeting one directly.
     * This is used internally to handle tooltip aggregation and some basic event stuff.
     * @param cons Consumer that is executed when this 
     */
    private void findTargetAndRun(LevelReader world, DeltaTracker delta, TriConsumer<Griddable<?>, AnchorSelector.Active, Float> cons) { 
        // TODO public access may be useful
        lookedThisFrame = false;
        while(!trackedEntries.isEmpty()) {
            final Active sel = trackedEntries.poll();
            if(sel == null || !sel.points.isInteractable()) continue;
            if(!sel.anchor.isIntersecting(world, lookingRay)) continue;
            lookedThisFrame = true;
            selected = sel;
            cons.accept(sel.points, sel, sel.distance);
            break;
        }
        if(!lookedThisFrame) {
            if(hasSelection() && selectedTicks > 0) {
                selectedTicks -= delta.getGameTimeDeltaTicks() / 2;
                selected.renderComplexAABB(selectedTicks, false);
            } else resetCompletely();
        }
    }



    

    @Override
    public String toString() {
        return playerHands + (hasSelection() ? ", and is targeting " + selected : " and has no target");
    }


    /**
     * An {@link AnchorPoint} container for comparing based on distance to the LocalPlayer
     * and sorting in a PriorityQueue
     */
    protected static class Active implements Comparable<Active> {

        private final Griddable<?> points; 
        private final AnchorPoint anchor;
        private final float distance;
        private UpdateResponse response;
        private VoxelShape highlightShape;

        public Active(Griddable<?> points, AnchorPoint anchor, GridUUID address, float distance) {
            Objects.requireNonNull(points);
            Objects.requireNonNull(anchor);
            Objects.requireNonNull(address);
            if(distance < 0.00001) 
                throw new IllegalArgumentException("Can't instantiate a new active selection with less than 0 distance!");
            this.points = points;
            this.anchor = anchor;
            this.distance = distance;
            this.response = UpdateResponse.NONE;
            float size = anchor.getSize();
            highlightShape = Shapes.create(-size, -size, -size, size, size, size);
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
            Vec3 shapePos = anchor.getPos(points.getWorld());

            // yoinked from vanilla
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
            AABB visual = anchor.makeHitbox(points.getWorld(), false).inflate(anchor.getSize() * ticks);
            Color col = Color.BLACK.mixWith(new Color(77, 253, 182), ticks);
            Outliner.getInstance().showAABB(unique ? anchor.hashCode() : 0xFFFFFFF, visual)
                .disableCull()
                .disableLineNormals()
                .colored(col)
                .lineWidth(0.010f * ticks);
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

        @Override
        public String toString() {
            return "[" + points + ", " + anchor + ", " +  response + "]";
        }
    }
}