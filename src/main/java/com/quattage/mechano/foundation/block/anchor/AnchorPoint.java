package com.quattage.mechano.foundation.block.anchor;

import java.util.List;

import javax.annotation.Nullable;

import static com.quattage.mechano.Mechano.lang;

import com.quattage.mechano.MechanoClient;
import com.quattage.mechano.MechanoSettings;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/***
 * An AnchorPoint provides the framework required for placing GridVertices assigned to a particular BlockEntity
 * within the world. All connection-related logic is handled by the GridVertex, but the physicality of the vertex
 * itself is handled by the AnchorPoint.
 */
public class AnchorPoint {
    private final GID systemLocation;
    private final AnchorTransform transform;
    private final int maxConnections;

    private float hitboxSize = 0;
    @Nullable private AABB hitbox = null;
    @Nullable private AABB staticHitbox = null;

    @Nullable private GridVertex participant = null;

    // TODO breakout
    private static final Color rawColor = new Color(205, 240, 231);
    private static final Color selectedColor = new Color(0, 255, 189);

    public AnchorPoint(AnchorTransform transform, GID systemLocation, int maxConnections) {
        this.systemLocation = systemLocation;
        this.transform = transform;
        this.maxConnections = maxConnections;
    }

    @Nullable
    public static Pair<AnchorPoint, WireAnchorBlockEntity> getAnchorAt(Level world, GID loc) {
        if(world == null) return null;
        if(loc == null) return null;
        BlockEntity be = world.getBlockEntity(loc.getBlockPos());
        if(be instanceof WireAnchorBlockEntity wbe) 
            return Pair.of(wbe.getAnchorBank().get(loc.getSubIndex()), wbe);
        return null;
    }

    @Nullable
    public static Pair<AnchorPoint, WireAnchorBlockEntity> getAnchorAt(ClientLevel world, GID loc) {
        if(world == null) return null;
        if(loc == null) return null;
        BlockEntity be = world.getBlockEntity(loc.getBlockPos());
        if(be instanceof WireAnchorBlockEntity wbe) 
            return Pair.of(wbe.getAnchorBank().get(loc.getSubIndex()), wbe);
        return null;
    }   

    @Nullable
    public static Pair<AnchorPoint, WireAnchorBlockEntity> getAnchorAt(BlockAndTintGetter accessor, GID loc) {
        if(accessor == null) return null;
        if(loc == null) return null;
        BlockEntity be = accessor.getBlockEntity(loc.getBlockPos());
        if(be instanceof WireAnchorBlockEntity wbe) 
            return Pair.of(wbe.getAnchorBank().get(loc.getSubIndex()), wbe);
        return null;
    }   

    /***
     * Updates the location of this AnchorPoint
     */
    public void update(BlockEntity target) {
        if(target != null) update(target.getBlockState());
    }

    /***
     * Updates the location of this AnchorPoint
     */
    public void update(BlockState state) {
        if(state != null) update(DirectionTransformer.extract(state));
    }

    /***
     * Updates the location of this AnchorPoint
     */
    public void update(CombinedOrientation orient) {
        transform.rotateToFace(orient);
        refreshHitbox();
    }

    public void broadcastChunkChange(GlobalTransferGrid grid) {
        if(participant != null) 
            participant.refreshLinkedChunks();
        else {
            GridVertex vert = grid.getVertAt(this.getID());
            if(vert == null) return;
            vert.refreshLinkedChunks();
        }
    }

    /**
     * Gets the hitbox of this AnchorPoint
     * @return an AABB representing the bounds of this AnchorPoint at its current location
     */
    public AABB getHitbox() {
        if(hitbox == null) refreshHitbox();
        return hitbox.inflate(hitboxSize * 0.005f);
    }

    public AABB getStaticHitbox() {
        if(staticHitbox == null) refreshHitbox();
        return staticHitbox;
    }

    /**
     * @return the size of this AnchorPoint
     */
    public float getSize() {
        return hitboxSize;
    }

    public boolean hasSize() {
        return hitboxSize > 0;
    }

    public void increaseToSize(float sizeTarget, double delta) {
        if(this.hitboxSize < sizeTarget)
            this.hitboxSize += delta * 1.6;
        if(this.hitboxSize > sizeTarget)
            hitboxSize = sizeTarget;
    }

    public void decreaseToSize(float sizeTarget, double delta) {
        if(this.hitboxSize > sizeTarget)
            this.hitboxSize -= delta * 1.6;
        if(this.hitboxSize < sizeTarget)
            hitboxSize = sizeTarget;
    }

    public void refreshHitbox() {
        Vec3 realPos = getPos();
        this.hitbox = new AABB(
            realPos.x - 0.001, 
            realPos.y - 0.001, 
            realPos.z - 0.001, 
            0.001 + realPos.x, 
            0.001 + realPos.y, 
            0.001 + realPos.z
        );


        this.staticHitbox = hitbox.inflate(MechanoSettings.ANCHOR_NORMAL_SIZE * 0.005f);
    }

    public Vec3 getPos() {
        return transform.toRealPos(systemLocation.getBlockPos());
    }

    public Vec3 getLocalOffset() {
        return VectorHelper.toVec(transform.getRaw());
    }

    public GID getID() {
        return systemLocation;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public double getDistanceToRaycast(Vec3 start, Vec3 end) {
        double closestDist = -1;
        Vec3 anchorPos = getPos();
        for(float x = 0f; x < 1; x += MechanoSettings.ANCHOR_SELECT_RAYMARCH_RESOLUTION) {
            Vec3 rayPos = start.lerp(end, x);
            double dist = rayPos.distanceTo(anchorPos);
            
            if(closestDist == -1 || dist < closestDist) 
                closestDist = dist;
        }
        return closestDist;
    }

    /**
     * Syncs the participant of this AnchorPoint. 
     * An AnchorPoint's "participant" refers to the {@link GridVertex <code>GridVertex</code>}
     * that this AnchorPoint repersents in the grid.
     * @param world World to operate within. If null, syncing will only be performed if this AnchorPoint already has a participant.
     */
    public void syncParticipant(@Nullable ServerLevel world) {
        if(participant == null) {
            if(world == null) return;
            GlobalTransferGrid grid = GlobalTransferGrid.of(world);
            participant = grid.getVertAt(systemLocation);
            if(participant == null) return;
            participant.doFullSync(true);
            return;
        }

        participant.doFullSync(true);
    }

    /**
     * Sets the participant of this AnchorPoint to <code>null</code> <p>
     * An AnchorPoint's "participant" refers to the {@link GridVertex <code>GridVertex</code>}
     * that this AnchorPoint repersents in the grid.
     */
    public void nullifyParticipant() {
        this.participant = null;
    }

    public void addTooltip(List<Component> tooltip, WireAnchorBlockEntity parent, boolean isPlayerSneaking, boolean isWearingGoggles, ItemStack heldSpool) {
        AnchorVertexData serverData = MechanoClient.ANCHOR_SELECTOR.getAnchorData();
        boolean awaiting = serverData == null || (!getID().equals(serverData.getID()));
        lang().translate("gui.anchorpoint.status.title").forGoggles(tooltip);
        if(awaiting) {
            lang().text("...").style(ChatFormatting.RED).forGoggles(tooltip);
        } else {
            lang().text(serverData.getConnections() + "/" + maxConnections + " ").translate("gui.anchorpoint.status.connections").style(ChatFormatting.GRAY).forGoggles(tooltip);
        }
    }

    /**
     * Sets the participant of this AnchorPoint to the provided {@link GridVertex <code>GridVertex</code>} instance.
     * @param participant <code>GridVertex</code> to set this AnchorPoint's participant to
     * @throws IllegalArgumentException The provided GridVertex <strong>must</strong> have the same GID as this AnchorPoint.
     */
    public void setParticipant(GridVertex participant) {
        if(!participant.isAt(this.systemLocation))
            throw new IllegalArgumentException("Error setting participant - AnchorPoint at " + systemLocation + " does not coorespond to the supplied GridVertex at " + participant.getID() + "! (Block positions and indices must be equal!)");
        this.participant = participant;
    }

    @Nullable
    public GridVertex getParticipant() {
        return participant;
    }

    /**
     * Finds the {@link GridVertex <code>GridVertex</code>} participant that this AnchorPoint refers to within the given world.
     * Stores the result so subsequent calls have lower overhead.
     * @param world World to operate within
     * @throws NullPointerException if the provided world is null
     */
    public GridVertex getOrFindParticipantIn(Level world) {
        if(participant != null) return participant;
        if(world == null) throw new NullPointerException("Error finding participant - world is null!");
        participant = GlobalTransferGrid.of(world).getVertAt(getID());
        return participant;
    }

    public boolean equals(Object other) {
        if(other instanceof AnchorPoint otherAnchor)
            return systemLocation.equals(otherAnchor.systemLocation);
        return false;
    }

    public String toString() {
        return "{" + systemLocation + ", " + maxConnections + "}";
    }

    public int hashCode() {
        return systemLocation.hashCode();
    }

    public Color getColor() {
        return selectedColor.copy().mixWith(rawColor, hitboxSize / (float)MechanoSettings.ANCHOR_SELECT_SIZE);
    }
}