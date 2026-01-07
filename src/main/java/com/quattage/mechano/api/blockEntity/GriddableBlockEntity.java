package com.quattage.mechano.api.blockEntity;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.GridUUID.VoxelUUID;
import com.quattage.mechano.foundation.tracking.TrackedObject;
import com.quattage.mechano.foundation.tracking.UUIDSourceType;

import net.minecraft.client.renderer.culling.Frustum;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public abstract class GriddableBlockEntity extends SimpleBlockEntity implements Griddable<VoxelUUID>{

    private @Nullable CircuitComponent circuit; // instantiated lazily
    private final GriddableTerminus joints = new GriddableTerminus();
    private VoxelUUID addr;

    public GriddableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public final CircuitComponent getCircuit() {    
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
        GridUUID id = getUUID();
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
    public int getPriority() {
        return 0;
    }
    
    @Override
    public float getMass(LevelReader world) {
        return TrackedObject.DEFAULT_MASS;
    }

    @Override 
    protected void saveTo(CompoundTag tag, Provider registries) {

    }

    @Override 
    protected void loadFrom(CompoundTag tag, Provider registries) {

    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        return player != null && player.getChunkTrackingView() != null && 
            player.getChunkTrackingView().contains(getSectionX(), getSectionZ());
    }

    @Override
    public void sendLevelUpdates(Level world) {
        world.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        return view.isVisible(getRenderBoundingBox());
    }

    @Override
    public IAttachmentHolder getDataStorageHolder() {
        return this;
    }

    @Override

    public UUIDSourceType getSourceScope() {
        return UUIDSourceType.VOXEL;
    }

    @Override
    public boolean isDynamic() {
        return false;
    }

    @Override 
    public String toString() { 
        return "GriddableBE '" + getBlockState().getBlock().getName().getString() + "' ::\n" + circuit;     
    }
}