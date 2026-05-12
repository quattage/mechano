package com.quattage.mechano.content.creative;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.block.orientation.Relative;
import com.quattage.mechano.grid.api.HeatingElement;
import com.quattage.mechano.grid.api.component.CircuitFactory;
import com.quattage.mechano.grid.topology.Node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeSinkBlockEntity extends GriddableBlockEntity {

    private @Nullable HeatingElement heater;

    public CreativeSinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if(getWorld().isClientSide) return;
        Mechano.LOGGER.warn("" + heater.getPowerState());
    }

    @Override
    public void constructCircuit(CircuitFactory circuit) {
        Node positive = circuit.newNode("positive");
        Node negative = circuit.newNode("negative");
        heater = circuit.supply(new HeatingElement(2));
        circuit.solder(positive, heater.positive());
        circuit.solder(negative, heater.negative());
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


    /**
     * -1.5 0.5 0.0 
        0.5 0.5 0.0 
        0.0 0.0 0.0 

        12.0 0.0 0.0
     */
}
