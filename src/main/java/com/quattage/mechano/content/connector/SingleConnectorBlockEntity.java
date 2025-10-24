package com.quattage.mechano.content.connector;

import com.quattage.mechano.api.anchor.AnchorCollection.DynamicAnchorArray;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class SingleConnectorBlockEntity extends ConnectorBlockEntity {

    public SingleConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void constructAnchors(DynamicAnchorArray builder) {
        builder.newAnchor()
            .at(8, 16, 8) 
            .connections(3)
            .radius(1.7f)
            .addTo(this);
    }
}

