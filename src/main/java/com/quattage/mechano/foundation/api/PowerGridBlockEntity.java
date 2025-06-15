package com.quattage.mechano.foundation.api;

import com.quattage.mechano.foundation.api.anchor.AnchorArray;
import com.quattage.mechano.foundation.api.anchor.AnchorPointHoldable;
import com.quattage.mechano.foundation.api.anchor.DispatchedAnchorNode;
import com.quattage.mechano.foundation.api.landmark.uuid.GridUUID;
import com.quattage.mechano.foundation.api.landmark.uuid.impl.VoxelUUID;
import com.quattage.mechano.foundation.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.foundation.blockEntity.renderer.PowerGridBlockEntityRenderer;
import com.quattage.mechano.foundation.catenary.Catenary;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class PowerGridBlockEntity extends ElectricBlockEntity implements AnchorPointHoldable {

    // always empty on the server
    private AnchorArray anchors = AnchorArray.EMPTY;
    public final DispatchedAnchorNode surrogate = new DispatchedAnchorNode(this);

    public PowerGridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        AnchorArray.Builder unbuiltAnchors = AnchorArray.construct(this);
        constructAnchors(unbuiltAnchors);
        this.anchors = unbuiltAnchors.confirm(getBlockPos());
        this.surrogate.nodeCount = this.anchors.size();
    }

    public abstract void constructAnchors(AnchorArray.Builder anchors);

    @Override
    public void onRefresh(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onRefresh(world, pos, oldState, newState);
        anchors.updateOrientation(newState);
    }

    @Override
    public void tick() {
        if(!getLevel().isClientSide) return;
        Catenary.tryUpdateOrElse(this, PowerGridBlockEntityRenderer.catenary, 1, (catenary) -> {});
    }

    @Override
    public void onLoad() {
        // the anchorpoint holder is set to empty on the server despite
        // being initially populated on both sides, this is stupid and dumb!!
        // who wrote this!?? (me, i did)
        if(!level.isClientSide)
            this.anchors = AnchorArray.EMPTY;
        super.onLoad();
        anchors.updateOrientation(getBlockState());
    }

    @Override
    public Level getWorld() {
        return getLevel();
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockBroken(world, pos, oldState, newState);
        surrogate.severAndForget();
    }

    @Override
    public AnchorArray getAnchors() {
        return anchors;
    }

    @Override
    public GridUUID createAddress() {
        return new VoxelUUID(getBlockPos(), 0);
    }

    @Override
    public DispatchedAnchorNode getSurrogate() {
        return surrogate;
    }

    @Override
    public String describeState() {
        return getBlockState().getBlock().getName().toString();
    }
}