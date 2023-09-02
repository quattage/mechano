package com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked;

import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;
import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
import com.quattage.mechano.foundation.electricity.builder.NodeBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectorStackedTier1BlockEntity extends WireNodeBlockEntity {

    public ConnectorStackedTier1BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

	@Override
	public void createWireNodeDefinition(NodeBankBuilder<WireNodeBlockEntity> builder) {
		builder.newNode()
            .id("conn1")
            .at(8, 20, 8) 
            .mode("B")
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
