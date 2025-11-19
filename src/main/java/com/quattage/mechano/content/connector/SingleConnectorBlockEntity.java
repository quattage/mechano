package com.quattage.mechano.content.connector;

import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.foundation.block.orientation.Relative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SingleConnectorBlockEntity extends ConnectorBlockEntity {

    public SingleConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void constructCircuit(CircuitFactory circuit) {
        Node passive = circuit.newJoint();
        circuit.wireJack("Wire Attachment")
            .attachedTo(passive)
            .x(0).y(17).z(0)
            .size(3.5f).make();
        circuit.blockJack("Bottom Face")
            .attachedTo(passive)
            .face(Relative.BOTTOM)
            .make();
    }
}

