package com.quattage.mechano.foundation.api;

import java.util.List;

import com.quattage.mechano.foundation.api.landmark.DispatchedNode;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.client.AnchorArray;
import com.quattage.mechano.foundation.api.landmark.client.AnchorPoint;
import com.quattage.mechano.foundation.api.transmitter.Transmitable;
import com.quattage.mechano.foundation.blockEntity.ElectricBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class PowerGridBlockEntity extends ElectricBlockEntity {

    // always empty on the server
    public AnchorArray anchors = AnchorArray.EMPTY;
    public final DispatchedNode surrogate = new DispatchedNode(this);

    public PowerGridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        AnchorArray.Builder unbuiltAnchors = AnchorArray.construct(this);
        construct(unbuiltAnchors);
        this.anchors = unbuiltAnchors.confirm(getBlockPos());
        this.surrogate.nodeCount = this.anchors.size();
    }

    protected abstract void construct(AnchorArray.Builder anchors);

    /**
     * Called continuously on the client while the player is looking at an {@link AnchorPoint} belonging to this PGBE.
     * Can be overridden to define custom tooltip behaviour for individual block entities. 
     * @param tooltip The list of text components that will be appended to the displayed tooltip.
     * @param target The AnchorPoint that the player is looking at
     * @param held Container for information about the player, their held item stack, and the {@link Transmitable} - 
     *  Note that the contents of this HoldingSummary will be empty if the player is not holding a relevent item.
     * @return <code>true</code> (Reccomended) If you'd like to also include tooltip submitted by {@link Transmitable#collectTooltipInfoAndResponse}
     */
    public boolean collectTooltipInfo(List<Component> tooltip, AnchorPoint target, Transmitable.HoldingSummary held) {
        if(target == null) return true;
        if(!target.belongsTo(this)) return true;
        target.writeInfoToTooltip(tooltip);
        return true;
    }

    @Override
    public void onRefresh(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onRefresh(world, pos, oldState, newState);
        anchors.updateOrientation(newState);
    }

    @Override
    public void tick() {
        
    }

    @Override
    public void onLoad() {
        // the anchorpoint holder is set to empty on the server despite
        // being initially populated on both sides, this is stupid and dumb!!
        // who wrote this!?? (me, i did)
        if(!level.isClientSide) {
            this.anchors = AnchorArray.EMPTY;
        }
        super.onLoad();
        anchors.updateOrientation(getBlockState());
    }

    /**
     * Called when this PGBE is initially registered within a {@link PowerGrid}.
     * 
     * @param world World to operate within
     * @param grid The grid that this PGBE was added to
     */
    public void onAddedToGrid(Level world, PowerGrid grid) {
        
    }

    /**
     * Called whenever a connection is made to/from this PGBE
     * @param world World to operate within
     * @param connection The GridLink representing the connection that was added
     */
    public void onConnectionMade(Level world, GridLink connection) {
        
    }

    /**
     * Called whenever a connection is removed to/from this PGBE
     * @param world World to operate within
     * @param connection The connection that was destroyed. Note that this method is called AFTER the GridLink is removed from the network, so this connection's reference is stale and should't be stored.
     */
    public void onConnectionBroken(Level world, GridLink connection) {
        
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockBroken(world, pos, oldState, newState);
        surrogate.severAndForget();
    }
}