package com.quattage.mechano.content.connector;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.landmark.client.AnchorArray.Builder;
import com.quattage.mechano.foundation.catenary.ParametricWireModel;
import com.quattage.mechano.foundation.catenary.SimulatedWireModel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SingleConnectorBlockEntity extends ConnectorBlockEntity {

    public static Vec3 endPos = null;
    
    private final SimulatedWireModel simWire = new SimulatedWireModel();
    private final ParametricWireModel paraWire = new ParametricWireModel();

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

        simWire.setOffset(anchors.getByIndex(0).getRealPosition(), endPos);
        if(!simWire.isInitialized()) simWire.initialize();
        simWire.update();
        simWire.drawDebug(anchors.getByIndex(0).getRealPosition());

        paraWire.setOffset(anchors.getByIndex(0).getRealPosition(), endPos);
        if(!paraWire.isInitialized()) paraWire.initialize();
        paraWire.update();
        paraWire.drawDebug(anchors.getByIndex(0).getRealPosition());
    }
}
