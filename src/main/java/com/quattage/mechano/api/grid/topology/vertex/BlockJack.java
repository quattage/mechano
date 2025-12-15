package com.quattage.mechano.api.grid.topology.vertex;

import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;
import com.quattage.mechano.foundation.block.orientation.RelativeDirection;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes.DoubleLineConsumer;

/**
 * A voxel-voxel interaction. This mirrors NeoForge's sided capability system specifically
 * for Mechano's GridAPI. This class is designed specifically to be added to
 * Circuits directly via the {@link CircuitFactory factory.} Only circuits which
 * belong to BlockEntities can make use of this class.
 * 
 * TODO sided capability faking for forge-energy parity
 */
public class BlockJack extends AncillaryNode implements OrientationUpdatable {

    private static final float THICK = 2f / 16f;
    private RelativeDirection dir;
    
    public BlockJack(String name, boolean isVisible, RelativeDirection dir) {
        super(name, isVisible);
        this.dir = dir;
    }

    @Override
    public float getXO() {
        return dir.get().getStepX();
    }

    @Override
    public float getYO() {
        return dir.get().getStepY();
    }


    @Override
    public float getZO() {
        return dir.get().getStepZ();
    }

    @Override
    public float getSize() {
        return 1;
    }

    @Override
    public AABB makeAABB(Vector3d basis, float size) {
        float minX = 0, minY = 0, minZ = 0;
        float maxX = 1, maxY = 1, maxZ = 1;
        switch(dir.get()) {
            case UP -> {minY = 1 - BlockJack.THICK; }
            case DOWN -> { maxY = BlockJack.THICK; }
            case NORTH -> { maxZ = BlockJack.THICK; }
            case SOUTH -> { minZ = 1 - BlockJack.THICK; }
            case EAST -> { minX = 1 - BlockJack.THICK; }
            case WEST -> { maxX = BlockJack.THICK; }
        }
        return new AABB(basis.x + minX, basis.y + minY,  basis.z + minZ, 
            basis.x + maxX, basis.y + maxY, basis.z + maxZ);
    }
    
    @Override
    public void forAllEdges(DoubleLineConsumer action) {
        float nx = 0, ny = 0, nz = 0;
        float px = 1, py = 1, pz = 1;
        switch(dir.get()) {
            case UP -> { ny = 1 - BlockJack.THICK; }
            case DOWN -> { py = BlockJack.THICK; }
            case NORTH -> { pz = BlockJack.THICK; }
            case SOUTH -> { nz = 1 - BlockJack.THICK; }
            case EAST -> { nx = 1 - BlockJack.THICK; }
            case WEST -> { px = BlockJack.THICK; }
        }
        // bottom square
        action.consume(nx, ny, nz, nx, ny, pz);
        action.consume(nx, ny, pz, px, ny, pz);
        action.consume(px, ny, pz, px, ny, nz);
        action.consume(px, ny, nz, nx, ny, nz);
        // top square
        action.consume(nx, py, nz, nx, py, pz);
        action.consume(nx, py, pz, px, py, pz);
        action.consume(px, py, pz, px, py, nz);
        action.consume(px, py, nz, nx, py, nz);
        // vertical sections
        action.consume(nx, ny, nz, nx, py, nz);
        action.consume(nx, ny, pz, nx, py, pz);
        action.consume(px, ny, pz, px, py, pz);
        action.consume(px, ny, nz, px, py, nz);
    }

    @Override
    void translateStack(Vector3d basis, Vec3 cameraPos, PoseStack matrixStack) {
        matrixStack.translate(
            basis.x - cameraPos.x, 
            basis.y - cameraPos.y, 
            basis.z - cameraPos.z
        );
    }

    @Override
    public void updateOrientation(CombinedOrientation dir) {
        this.dir.updateOrientation(dir);
    }

    @Override
    public String describeState() {
        return "(facing " + dir + ") " + super.describeState();
    }
}
