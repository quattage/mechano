package com.quattage.mechano.content.creative;

import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.api.grid.component.impl.InfiniteVoltageSource;
import com.quattage.mechano.api.grid.topology.CircuitFactory;
import com.quattage.mechano.api.grid.topology.vertex.Node;
import com.quattage.mechano.foundation.block.orientation.Relative;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeVoltaplastBlockEntity extends GriddableBlockEntity {

    public CreativeVoltaplastBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void constructCircuit(CircuitFactory circuit) {
        Node positive = circuit.newNode("positive");
        Node negative = circuit.newNode("negative");
        InfiniteVoltageSource battery = circuit.supply(new InfiniteVoltageSource("creative_voltoplast", 12f));
        circuit.solder(positive, battery.positive());
        circuit.solder(negative, battery.negative());
        circuit.blockJack("positive")
            .attachedTo(positive)
            .face(Relative.BACK)
            .visibleByDefault()
            .make();
        circuit.blockJack("negative")
            .attachedTo(negative)
            .face(Relative.FRONT)
            .visibleByDefault()
            .make();
    }
}
