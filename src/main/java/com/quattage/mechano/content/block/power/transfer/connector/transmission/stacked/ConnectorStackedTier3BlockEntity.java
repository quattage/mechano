package com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked;

import com.quattage.mechano.foundation.electricity.builder.BatteryBankBuilder;
import com.quattage.mechano.foundation.block.orientation.relative.Relative;
import com.quattage.mechano.foundation.electricity.ElectricBlockEntity;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
import com.quattage.mechano.foundation.electricity.builder.NodeBankBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ConnectorStackedTier3BlockEntity extends WireNodeBlockEntity {

    public ConnectorStackedTier3BlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

	@Override
	public void createWireNodeDefinition(NodeBankBuilder<WireNodeBlockEntity> builder) {
		builder.newNode()
            .at(20, 21, 8)
            .mode("B")
            .connections(3)
            .build()
        .newNode()
            .at(-4, 21, 8) 
            .mode("B")
            .connections(3)
            .build();
	}

    @Override
    public void createBatteryBankDefinition(BatteryBankBuilder<ElectricBlockEntity> builder) {
        builder
            .capacity(5000)
            .maxIO(2500)
            .newInteraction(Relative.BOTTOM)
                .sendsAndReceivesEnergy()
                .buildInteraction()
            .build();
    }

    @Override
    public boolean shouldMergeImplicitNodes() {
        return true;
    }
}
