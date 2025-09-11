package com.quattage.mechano.foundation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Allows implementing blocks to override the default behaviour when determining whether or not
 * a connector is allowed to be placed on a specific block.
 */
public interface ConnectorHostOverridable {
    
    /**
     * Called internally by {@link com.quattage.mechano.content.connector.BlockWithConnections#canSurvive ConnectorBlock.canSurvive()} <p>
     * Implement your own logic here to determine whether or not your block
     * can host a connector.
     * @param world World to operate within
     * @param connectorPos Position of the connector that called this method
     * @param connectorState BlockState of the connector that called this method
     * @param thisPos Position of this block
     * @param thisState BlockState of this block
     * @return
     */
    abstract boolean isConnectorAllowed(LevelReader world, BlockPos connectorPos, BlockState connectorState, BlockPos thisPos, BlockState thisState);

}
