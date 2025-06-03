package com.quattage.mechano.content.connector;

import com.quattage.mechano.foundation.api.catenary.RealtimeWireModel;
import com.quattage.mechano.foundation.api.landmark.client.AnchorArray.Builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SingleConnectorBlockEntity extends ConnectorBlockEntity {

    public static Vec3 endPos = null;
    private final RealtimeWireModel wire = new RealtimeWireModel();

    public SingleConnectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void construct(Builder anchors) {
        anchors
            .add()
                .at(8, 16, 8)
                .connections(3)
                .radius(1.7f)
                .make();
    }
    

    @Override
    public void tick() {
        super.tick();

        if(!getLevel().isClientSide()) return;
        if(endPos == null) return;

        wire.setOffset(anchors.getByIndex(0).getRealPosition(), endPos);
        if(!wire.isInitialized()) wire.initialize();
        wire.simulate();
        wire.drawDebug(anchors.getByIndex(0).getRealPosition());
    }
}
