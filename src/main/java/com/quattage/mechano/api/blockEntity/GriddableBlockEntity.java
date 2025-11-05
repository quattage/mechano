package com.quattage.mechano.api.blockEntity;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.griddable.LazyJointHolder;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public abstract class GriddableBlockEntity extends ElectricBlockEntity implements Griddable<BlockEntity> {

    private @Nullable CircuitComponent circuit; // instantiated lazily
    private final LazyJointHolder joints = new LazyJointHolder();

    public GriddableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public final CircuitComponent getCircuit() {    
        if(circuit != null) return circuit;
        CircuitFactory builder = new CircuitFactory();
        constructCircuit(builder);
        this.circuit = builder.make(this);
        Mechano.LOGGER.warn("::\n" + circuit);
        return this.circuit;
    }

    /**
     * This method is invoked lazily when a call to {@link #getCircuit}
     * fails to provide a non-null value. Use this method and its 
     * {@link CircuitFactory factory} to define the initial configuration
     * of this GriddableBlockEntity's circuit.
     * @param circuit
     */
    public abstract void constructCircuit(CircuitFactory circuit);

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockBroken(world, pos, oldState, newState);
        // getSurrogate().destroy();
    }

    @Override
    public void tick() {
        
    }

    @Override
    protected AABB createRenderBoundingBox() {
        // if(!level.isClientSide || !surrogate.isSynced()) 
            return super.createRenderBoundingBox();
        // return AABB.INFINITE;
    }

    @Override
    public Level getWorld() {
        return getLevel();
    }

    @Override
    public BlockEntity getSource() {
        return this;
    }

    @Override
    public String describeState() {
        return "GriddableBE '" + getBlockState().getBlock().getName().getString() + "'";
    }

    @Override
    public String toString() {
        return describeState();
    }

    @Override
    public LazyJointHolder getExposedAncillaries() {
        return joints.updateAncillaries(circuit);
    }

    @Override
    public Vector3d getSourcePos() {
        Vec3i pos = getBlockPos();
        return new Vector3d(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Quaternionf getSourceRotation() {
        return DirectionTransformer.extract(this.getBlockState()).getRotation();
    }

    @Override
    public float getSourceMass() {
        return 99999f;
    }

    @Override
    protected void saveTo(CompoundTag tag, Provider registries) {

    }

    @Override
    protected void loadFrom(CompoundTag tag, Provider registries) {
        
    }
}