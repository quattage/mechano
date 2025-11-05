package com.quattage.mechano.api.grid.topology.ancillary;

import org.joml.Vector3d;

import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;
import com.quattage.mechano.foundation.block.orientation.RelativeDirection;

import net.minecraft.world.phys.AABB;

/**
 * A voxel-voxel interaction. This mirrors NeoForge's sided capability system specifically
 * for Mechano's GridAPI. This class is designed specifically to be added to
 * Circuits directly via the {@link CircuitFactory factory.} Only circuits which
 * belong to BlockEntities can make use of this class.
 * 
 * TODO sided capability faking for forge-energy parity
 */
public class BlockJack extends AncillaryJack implements OrientationUpdatable {

    private RelativeDirection dir;
    
    public BlockJack(String name, boolean isVisible, RelativeDirection dir) {
        super(name, isVisible);
        this.dir = dir;
    }

    @Override
    public float getXO() {
        return 1;
    }

    @Override
    public float getYO() {
        return 1;
    }

    @Override
    public float getZO() {
        return 1;
    }

    @Override
    public float getSize() {
        return 1;
    }

    @Override
    public AABB makeAABB(Vector3d basis, float size) {
        return new AABB(0, 0, 0, 1, 1, 1);
    }

    @Override
    public void updateOrientation(CombinedOrientation dir) {
        this.dir.updateOrientation(dir);
    }
}
