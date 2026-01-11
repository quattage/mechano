package com.quattage.mechano.api.grid.topology.vertex;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.VoxelUUID;
import com.quattage.mechano.api.grid.topology.CircuitFactory;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;
import com.quattage.mechano.foundation.block.orientation.RelativeDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes.DoubleLineConsumer;

/**
 * A voxel-voxel interaction. This mirrors NeoForge's sided capability system specifically
 * for Mechano's GridAPI. This class is designed specifically to be added to
 * Circuits directly via the {@link CircuitFactory factory.} Only circuits which
 * belong to BlockEntities refer to a {@link VoxelUUID} can make use of this class.
 * 
 * TODO sided capability faking for forge-energy parity
 */
public class BlockJack extends AncillaryNode<VoxelUUID> implements OrientationUpdatable {

    private static final float THICK = 2f / 16f;
    private RelativeDirection dir;
    
    public BlockJack(String name, boolean isVisible, RelativeDirection dir) {
        super(name, isVisible);
        this.dir = dir;
    }

    public Vec3i getPos(Vec3i basis) {
        return basis.relative(dir.get());
    }

    public @Nullable BlockJack getOpposing(Level world, BlockPos basis) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(basis);
        BlockEntity be = world.getBlockEntity((BlockPos)getPos(basis));
        if(be == parent) {
            throw new IllegalStateException("Encountered an invalid blockentity traversal while getting opposing " 
                + "ancillaries - BlockJack " + this + " refers to itself! (at " + getPos(basis) + ")");
        }
        if(!(be instanceof Griddable gbe)) return null;
        GriddableTerminus terminus = gbe.getTerminus();
        if(terminus.isEmpty()) return null;
        for(int x = 0; x < terminus.size(); x++) {
            AncillaryNode<?> other = terminus.getAncillary(x);
            if(other == this) {
                throw new IllegalStateException("Encountered a leaked AncillaryNode instance (" 
                    + other + ") - This ancillary has two hosts: " + this + "  , and " + gbe);
            }
            if(!(other instanceof BlockJack bj)) continue;
            if(bj.isOpposing(this)) return bj;
        }
        return null;
    }

    public Direction getFacing() {
        return dir.get();
    }

    public boolean isOpposing(BlockJack other) {
        return other != null && other.dir.get().getOpposite().equals(this.dir.get());
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
    public <T extends ComponentUUID<T>> T bindUUID(T id) {
        throw new UnsupportedOperationException("Unimplemented method 'bindUUID'");
    }

    @Override
    public @Nullable CircuitComponent findSubComponent(ComponentUUID<?> id) {
        return this;
    }
}
