
package com.quattage.mechano.api.grid.topology.ancillary;

import org.joml.Vector3d;

import com.quattage.mechano.api.grid.CircuitFactory;

import net.minecraft.world.phys.AABB;

/**
 * An attachment point for catenaries. This class is designed specifically to be added to
 * Circuits directly via the {@link CircuitFactory factory.}
 * WireJacks are pushed to the selector so that their hitboxes can be highlighted when
 * players look at them.
 */
public class WireJack extends AncillaryJack {

    private final long data;

    public WireJack(String name, boolean isVisible, long data) {
        super(name, isVisible);
        this.data = data;
    }

    @Override public float getXO() { return this.toOffset(this.unpack(48)) / 16f; }
    @Override public float getYO() { return this.toOffset(this.unpack(32)) / 16f; }
    @Override public float getZO() { return this.toOffset(this.unpack(16)) / 16f; }
    @Override public float getSize() { return this.toOffset(this.unpack(0)) / 16f; }
    private int unpack(int shift) { return (int)((this.data >> shift) & 0xFFFF); }
    private float toOffset(int x) { return -16f + ((x + 32768f) / 65535f) * 48f; }

    @Override
    public AABB makeAABB(Vector3d basis, float size) {
        return new AABB(
            ((basis.x + getXO()) - size),
            ((basis.y + getYO()) - size),
            ((basis.z + getZO()) - size),
            ((basis.x + getXO()) + size),
            ((basis.y + getYO()) + size),
            ((basis.z + getZO()) + size)
        );
    }
}
