package com.quattage.mechano.foundation.electricity.core.anchor;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.rendering.WireAnchorBlockRenderer;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.foundation.utility.Color;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AnchorPoint {
    private final GID systemLocation;
    private final AnchorTransform transform;

    private final int maxConnections;

    private float anchorSize;
    private float targetFactor = 0;
    private AABB hitbox = null;

    // TODO breakout
    private static final Color rawColor = new Color(205, 240, 231);
    private static final Color selectedColor = new Color(0, 255, 189);

    public AnchorPoint(AnchorTransform transform, GID systemLocation, int maxConnections) {
        this.systemLocation = systemLocation;
        this.transform = transform;
        this.maxConnections = maxConnections;
        this.anchorSize = 0;
    }

    @Nullable
    public static Pair<AnchorPoint, WireAnchorBlockEntity> getAnchorAt(Level world, GID loc) {
        if(world == null) return null;
        if(loc == null) return null;
        BlockEntity be = world.getBlockEntity(loc.getPos());
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

    /***
     * Gets the hitbox of this AnchorPoint
     * @throws IllegalStateException 
     * @return an AABB representing the bounds of this AnchorPoint at its current location
     */
    public AABB getHitbox() {
        if(hitbox == null) return null;
        return hitbox.inflate(anchorSize * 0.005f);
    }

    /***
     * @return the size of this AnchorPoint
     */
    public float getSize() {
        return anchorSize;
    }

    /***
     * Sets the size of this AnchorPoint
     * @param anchorSize
     */
    public void setSize(int anchorSize) {
        this.anchorSize = anchorSize;
        if(anchorSize < 0) anchorSize = 0;
    }

    /***
     * Decreases the size of this AnchorPoint
     */
    public void deflate(float amt) {
        this.anchorSize -= amt;
        if(anchorSize < 0) anchorSize = 0;
    }

    /***
     * Increases the size of this AnchorPoint
     */
    public void inflate(float amt) {
        this.anchorSize += amt;
    }

    public void incTargetTick() {
        if(targetFactor < 1)
            targetFactor+= 0.05f;
    }

    public void decTargetTick() {
        if(targetFactor > 0) 
            targetFactor -= 0.05f;
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
    }

    public Vec3 getPos() {
        return transform.toRealPos(systemLocation.getPos());
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
        for(float x = 0f; x < 1; x += 0.1f) {
            Vec3 rayPos = start.lerp(end, x);
            double dist = rayPos.distanceTo(anchorPos);
            if(closestDist == -1 || dist < closestDist) 
                closestDist = dist;
        }
        return closestDist;
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
        return selectedColor.copy().mixWith(rawColor, anchorSize / (float)WireAnchorBlockRenderer.ANCHOR_SELECT_SIZE);
    }
}
