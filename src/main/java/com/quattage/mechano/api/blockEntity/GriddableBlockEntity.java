package com.quattage.mechano.api.blockEntity;

import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.grid.GridUUID.VoxelUUID;
import com.quattage.mechano.grid.Griddable;
import com.quattage.mechano.grid.GriddableTerminus;
import com.quattage.mechano.grid.HierarchicalConstruct;
import com.quattage.mechano.grid.api.component.Circuit;
import com.quattage.mechano.grid.api.component.CircuitComponent;
import com.quattage.mechano.grid.api.component.CircuitFactory;
import com.quattage.mechano.grid.topology.link.AncillaryPair;
import com.quattage.mechano.switchboard.action.GridAction;

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

    private @Nullable Circuit circuit; // instantiated lazily
    private final GriddableTerminus terminus = new GriddableTerminus();

    public GriddableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
        UUIDComposite.promptIfUnused(ComponentHierarchy.CIRCUIT, binding, this); 
        if(circuit != null) return circuit;
        CircuitFactory builder = new CircuitFactory();
        constructCircuit(builder);
        this.circuit = builder.make(this);
        return this.circuit;
    }

    /**
     * This method is invoked lazily when a call to {@link #getComponent}
     * fails to provide a non-null value. Use this method and its 
     * {@link CircuitFactory factory} to define the initial configuration
     * of this GriddableBlockEntity's circuit.
     * 
     * @param circuit A fresh CircuitFactory instance with no components
     */
    public abstract void constructCircuit(CircuitFactory circuit);

    @Override
    public VoxelUUID getUUID() {
        return new VoxelUUID(getBlockPos());
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        if(world.isClientSide || circuit == null) return;
        ServerGrid grid = Grid.server(world);
        grid.removeComponent(circuit, null);
        grid.initiateTask(GridAction.TASK_COMPONENT_DESTROY)
            .withArguments(GridTracking.getAddress(this, circuit))
            .executeImmediately();
        if(terminus != null) terminus.invalidate();
    }   

    @Override
    public void onRefresh(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        if(newState.isAir()) return;
        getTerminus().updateOrientation(newState);
        if(world.isClientSide()) return;
        Set<AncillaryPair> adjs = analyzeAdjacents();   
        ServerGrid grid = Grid.server(world);
        for(AncillaryPair pair : adjs) {
            pair.validateSelf();
            grid.addLinkDeferred(pair, null);
        }
    }

    @Override
    public void tick() {
        if(!getLevel().isClientSide()) return;
        forEachExternalLink(link -> {
            link.tick(getWorld());
        });
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
        return terminus;
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
    public GridTracking getTrackerScope() {
        return GridTracking.VOXEL;
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        return player != null && player.getChunkTrackingView() != null && 
            player.getChunkTrackingView().contains(getSectionX(), getSectionZ());
    }

    @Override 
    public String toString() { 
        return getBlockState().getBlock().getName().getString() + ":\n" + circuit;     
    }

    @Override
    public AABB getRenderBoundingBox() {
        if(getTerminus() != null && getTerminus().hasConnections()) 
            return AABB.INFINITE;
        return super.getRenderBoundingBox();
    }

    @Override
    public void onAddedToGrid(Grid grid) {
        invalidateRenderBoundingBox();
    }

    @Override
    public void onRemovedFromGrid(Grid grid) {
        invalidateRenderBoundingBox();
    }

    @Override
    public int getMergePriority() {
        return circuit instanceof HierarchicalConstruct gc ? gc.getMergePriority() : 5;
    }
}