package com.quattage.mechano.content.block.power.transfer.connector.transmission;

import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;
import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
import com.quattage.mechano.foundation.electricity.builder.NodeBankBuilder;
import com.quattage.mechano.foundation.electricity.core.anchor.NodeMode;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TransmissionConnectorBlockEntity extends WireNodeBlockEntity {


    public TransmissionConnectorBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    @Override
    public void createWireNodeDefinition(NodeBankBuilder<WireNodeBlockEntity> builder) {
        builder.newNode()
            .at(8, 16, 8)
            .mode(NodeMode.BOTH)
            .connections(2)
            .build();
    }

    @Override
    public void createBatteryBankDefinition(BatteryBankBuilder<ElectricBlockEntity> builder) {
        builder
            .capacity(5000)
            .maxIO(2500)
            .newInteraction(Relative.BOTTOM)
            .buildInteraction()
        .build();
    }

    @Override
    public boolean shouldMergeImplicitNodes() {
        return true;
    }
}
