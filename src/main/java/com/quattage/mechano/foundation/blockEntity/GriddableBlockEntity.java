package com.quattage.mechano.foundation.blockEntity;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.anchor.AnchorArray;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.VoxelUUID;
import com.quattage.mechano.foundation.catenary.CatenaryAccessor;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class GriddableBlockEntity extends ElectricBlockEntity implements Griddable<BlockEntity>, CatenaryAccessor {

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
        if(!surrogate.belongsToNetwork()) return;
        forEachCatenary(cat -> {
            cat.updateShapeFixed((ClientLevel)level, this);
        });
    }

    @Override
    public void onAnchorSynced(Level world, int index) {
        if(!world.isClientSide) return;
        invalidateRenderBoundingBox();
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if(!level.isClientSide || !surrogate.belongsToNetwork()) 
            return super.createRenderBoundingBox();
        return AABB.INFINITE;
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
    @OnlyIn(Dist.CLIENT)
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        if(!level.isClientSide()) return null;
        LinkDataStorable.Client storage = LinkDataStorable.getAsClient(this, false);
        if(storage == null) return null;
        return storage.getAll();
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