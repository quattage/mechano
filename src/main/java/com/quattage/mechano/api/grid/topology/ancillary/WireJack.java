
package com.quattage.mechano.api.grid.topology.ancillary;

import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An attachment point for catenaries. This class is designed specifically to be added to
 * Circuits directly via the {@link CircuitFactory factory.}
 * WireJacks are pushed to the selector so that their hitboxes can be highlighted when
 * players look at them.
 */
public class WireJack extends AncillaryJack implements OrientationUpdatable {

    private final long data;
    private final Vector3f offset;

    public WireJack(String name, boolean isVisible, long data) {
        super(name, isVisible);
        this.data = data;
        this.offset = makeOffsetVector();
    }

    private Vector3f makeOffsetVector() {
        return new Vector3f(toOffset(unpack(48)) / 32f, toOffset(unpack(32)) / 32f, toOffset(unpack(16)) / 32f);
    }

    @Override public float getXO() { return offset.x; }
    @Override public float getYO() { return offset.y; }
    @Override public float getZO() { return offset.z; }
    @Override public float getSize() { return toOffset(unpack(0)) / 32f; }
    
    private short unpack(int shift) { 
        return (short)(data >> shift); 
    }
    private float toOffset(short x) { 
        return ((((int)x) - Short.MIN_VALUE) * (48f / (Short.MAX_VALUE - Short.MIN_VALUE))) - 16f;
    }

    @Override
    public AABB makeAABB(Vector3d basis, float size) {
        return new AABB(
            (0.5d + basis.x + offset.x) - size,
            (0.5d + basis.y + offset.y) - size,
            (0.5d + basis.z + offset.z) - size,
            (0.5d + basis.x + offset.x) + size, 
            (0.5d + basis.y + offset.y) + size,
            (0.5d + basis.z + offset.z) + size
        );
    }

    @Override
    void translateStack(Vector3d basis, Vec3 cameraPos, PoseStack matrixStack) {
        matrixStack.translate(
            (0.5d + basis.x) - cameraPos.x, 
            (0.5d + basis.y) - cameraPos.y, 
            (0.5d + basis.z) - cameraPos.z
        );
    }

    @Override
    public void updateOrientation(CombinedOrientation dir) {
        // TODO FIX
        makeOffsetVector().rotate(dir.getLocalUp().getRotation(), offset);
    }

    @Override
    public String describeState() {
        return "(" + getXO() + ", " + getYO() + ", " + getZO() + ", " + getSize() + ") " + super.describeState();
    }
}
