package com.quattage.mechano.foundation.api;

import com.quattage.mechano.foundation.api.grid.PowerGrid;
import com.quattage.mechano.foundation.api.grid.landmarks.GridLink;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class PowerGridBlockEntity extends ElectricBlockEntity {

    public PowerGridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * Called whenever this PGBE is registered within a {@link PowerGrid}
     * @param world World to operate within
     * @param grid The grid that this PGBE was added to
     */
    public abstract void onAddedToGrid(Level world, PowerGrid grid);

    /**
     * Called whenever this PGBE is removed from a {@link PowerGrid}
     * @param world World to operate within
     * @param grid The grid that this PGBE was removed from
     */
    public abstract void onRemovedFromGrid(Level world, PowerGrid grid);

    /**
     * Called whenever a connection is made to/from this PGBE
     * @param world World to operate within
     * @param connection The GridLink representing the connection that was added
     */
    public abstract void onConnectionMade(Level world, GridLink connection);

    /**
     * Called whenever a connection is removed to/from this PGBE
     * @param world World to operate within
     * @param connection The connection that was destroyed. Note that this method is called AFTER the GridLink is removed from the network, so this connection's reference is stale and should't be stored.
     */
    public abstract void onConnectionBroken(Level world, GridLink connection);
}