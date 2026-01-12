package com.quattage.mechano.api.blockEntity;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.ComponentTracker;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.ComponentUUID.ComponentBinding;
import com.quattage.mechano.api.grid.component.ComponentUUID.VoxelUUID;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.CircuitFactory;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public abstract class GriddableBlockEntity extends SimpleBlockEntity implements Griddable<VoxelUUID>{

    private @Nullable CircuitComponent circuit; // instantiated lazily
    private final GriddableTerminus joints = new GriddableTerminus();
    private VoxelUUID addr;

    public GriddableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable CircuitComponent getComponent(ComponentBinding binding) {
        if(circuit != null) return circuit;
        CircuitFactory builder = new CircuitFactory();
        constructCircuit(builder);
        this.circuit = builder.make(this);
        return this.circuit;
    }

    /**
     * This method is invoked lazily when a call to {@link #getCircuit}
     * fails to provide a non-null value. Use this method and its 
     * {@link CircuitFactory factory} to define the initial configuration
     * of this GriddableBlockEntity's circuit.
     * @param circuit A fresh CircuitFactory instance with no components
     */
    public abstract void constructCircuit(CircuitFactory circuit);

    @Override
    public VoxelUUID getUUID() {
        if(this.addr == null) 
            this.addr = new VoxelUUID(getBlockPos());
        return this.addr;   
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        Grid grid = Grid.getUnsided(world);
        ComponentUUID<?> id = getUUID();
        List<AncillaryPair> links = grid.getLinksBelongingTo(id);
        if(links == null || links.isEmpty()) return;
        // convert to array to avoid concurrency issues
        AncillaryPair[] linksArray = links.toArray(new AncillaryPair[links.size()]);
        for(int x = 0; x < linksArray.length; x++)
            grid.removeLink(linksArray[x]);
        GriddableTerminus gt = provideTerminus();
        if(gt != null) gt.invalidate();
    }

    @Override
    public void onRefresh(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        GriddableTerminus gt = getTerminus();
        gt.updateOrientation(newState);
        Set<AncillaryPair> adjs = analyzeAdjacents();   
        if(adjs == null || adjs.isEmpty()) return;
        Grid grid = Grid.getUnsided(world);
        for(AncillaryPair pair : adjs) {
            pair.validateSelf();
            grid.addLink(pair);
            if(grid instanceof ServerGrid sg)
                sg.getNetlist().union(pair.getStartNode(), pair.getEndNode());
        }
    }

    @Override
    public void tick() {
        if(getLevel().isClientSide()) return;
        Grid.server(getLevel());
    }

    @Override
    public void initialize() {
        getTerminus().updateOrientation(getBlockState());
    }

    @Override
    protected AABB createRenderBoundingBox() {
        return super.createRenderBoundingBox();
    }

    @Override 
    public GriddableTerminus provideTerminus() { 
        return joints;
    }

    @Override 
    public Vector3d getSourcePos() { 
        Vec3i pos = getBlockPos(); 
        return new Vector3d(pos.getX(), pos.getY(), pos.getZ()); 
    }

    @Override
    public @Nullable Level getWorld() {
        return isRemoved() ? null : level;
    }

    @Override 
    public Quaternionf getSourceRotation() { 
        return DirectionTransformer.extract(this.getBlockState()).getRotation(); 
    }

    @Override 
    protected void saveTo(CompoundTag tag, Provider registries) {

    }

    @Override 
    protected void loadFrom(CompoundTag tag, Provider registries) {

    }

    @Override
    public ComponentTracker getTrackerScope() {
        return ComponentTracker.VOXEL;
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        return player != null && player.getChunkTrackingView() != null && 
            player.getChunkTrackingView().contains(getSectionX(), getSectionZ());
    }

    @Override 
    public String toString() { 
        return "GBE '" + getBlockState().getBlock().getName().getString() + "' ::\n" + circuit;     
    }
}