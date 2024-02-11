package com.quattage.mechano.content.block.power.transfer.connector.transmission;

import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;
import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.builder.AnchorBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TransmissionConnectorBlockEntity extends WireAnchorBlockEntity {


    public TransmissionConnectorBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(tileEntityTypeIn, pos, state);
    }

    @Override
    public void createWireNodeDefinition(AnchorBankBuilder<WireAnchorBlockEntity> builder) {
        builder.newNode()
            .at(8, 16, 8)
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
}
