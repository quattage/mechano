package com.quattage.mechano.foundation.blockEntity;

import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.anchor.AnchorArray;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.VoxelUUID;

import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class GriddableBlockEntity extends ElectricBlockEntity implements Griddable<BlockEntity> {

    // always empty on the server
    private AnchorArray anchors = AnchorArray.EMPTY;
    private final SurrogateNode surrogate = new SurrogateNode(this);

    public GriddableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        AnchorArray.Builder unbuiltAnchors = AnchorArray.construct(this);
        constructAnchors(unbuiltAnchors);
        this.anchors = unbuiltAnchors.confirm(getBlockPos());
    }

    @Override
    public abstract void constructAnchors(AnchorArray.Builder anchors);

    @Override
    public void tick() {
        if(!getLevel().isClientSide) return;
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
    public void onRefresh(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onRefresh(world, pos, oldState, newState);
        anchors.updateOrientation(newState);
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockBroken(world, pos, oldState, newState);
        destroySurrogate();
    }

    @Override
    public AnchorArray getAnchors() {
        return anchors;
    }

    @Override
    public GridUUID getOrCreateAddress() {
        return new VoxelUUID(getBlockPos(), 0);
    }

    @Override
    public SurrogateNode getSurrogate() {
        return surrogate;
    }

    @Override
    public String describeState() {
        return "Block '" + getBlockState().getBlock().getName().getString() + "'";
    }

    @Override
    public Visual getVisual() {
        return (selected, tooltip, posX, posY, graphics) -> {
            GuiGameElement.of(getBlockState().getBlock().asItem())
			.at(posX + 10, posY - 16, 450)
			.render(graphics);
		};
    }

    @Override
    public BlockEntity getSource() {
        return this;
    }
}