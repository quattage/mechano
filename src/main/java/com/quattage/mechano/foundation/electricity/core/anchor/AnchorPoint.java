package com.quattage.mechano.foundation.electricity.core.anchor;

import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.system.SVID;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AnchorPoint {

    private final SVID systemLocation;
    private final AnchorTransform transform;

    // private final AnchorCapability[] capabilities;

    private final float anchorSize;
    private AABB hitbox = null;

    public AnchorPoint(AnchorTransform worldLocation, SVID systemLocation) {
        this.systemLocation = systemLocation;
        this.transform = new AnchorTransform(8, 8, 8);
        this.anchorSize = 0.6f;
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
        if(hitbox == null) throw new IllegalStateException("Error obtaining AnchorPoint hitbox - AnchorPoint has not been initialized!");
        return hitbox;
    }

    /***
     * @return the size of this AnchorPoint
     */
    public float getSize() {
        return anchorSize;
    }

    public void refreshHitbox() {
        Vec3 realPos = getLocation();
        hitbox = new AABB(
            realPos.x - anchorSize, 
            realPos.y - anchorSize, 
            realPos.z - anchorSize, 
            anchorSize + realPos.x, 
            anchorSize + realPos.y, 
            anchorSize + realPos.z
        );
    }

    public Vec3 getLocation() {
        return transform.toRealPos(systemLocation.getPos());
    }

    /***
     * Compares AnchorPoints for equivalence. <p>
     * Note that "equivalence" in this case does not
     * compare the exact vector location.
     * @param other Object to compare
     * @return True if these AnchorPoints are equivalent. 
     * Two AnchorPoints are equivalent if both BlockPos
     * and indices are identical.
     */
    public boolean equals(Object other) {
        if(other instanceof AnchorPoint otherAnchor)
            return systemLocation.equals(otherAnchor.systemLocation);
        return false;
    }
}
