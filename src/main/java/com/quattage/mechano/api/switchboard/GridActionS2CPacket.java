package com.quattage.mechano.api.switchboard;

import java.util.Objects;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.api.switchboard.action.GridActionType.GridActionDecodeException;
import com.quattage.mechano.api.switchboard.action.GridActionType.GridActionEncodeException;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record GridActionS2CPacket(GridAction response, Object[] args) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, GridActionS2CPacket> STREAM_CODEC = new StreamCodec<>() {

        @Override 
        public GridActionS2CPacket decode(ByteBuf buffer) { 
            byte idx = buffer.readByte();
            if(idx < 0 || idx >= GridAction.values().length) {
                throw new IndexOutOfBoundsException("Failed while decoding response task - response index " + idx 
                    + " is out of bounds for a response registry of " + GridAction.values().length + " members!");
            }
            GridAction response = GridAction.values()[buffer.readByte()];
            GridActionTask task = response.getTask();
            if(task == null) throw new IllegalArgumentException("Failed while decoding response task '" + response + "' - This response type didn't produce a task!");
            Object[] decodedArgs = null;
            try { decodedArgs = task.dynamicDecode(buffer); }
            catch(RuntimeException e) { throw new GridActionDecodeException(e, response); }
            if(decodedArgs == null) decodedArgs = new Object[0];
            return new GridActionS2CPacket(response, decodedArgs);
        }

        @Override 
        public void encode(ByteBuf buffer, GridActionS2CPacket value) { 
            buffer.writeByte(value.response.ordinal()); 
            GridActionTask task = value.response.getTask();
            if(task == null) throw new IllegalArgumentException("Failed while encoding response task '" + value.response + "' - This response type didn't produce a task!");
            try { task.dynamicEncode(value.args, buffer); }
            catch(RuntimeException e) { throw new GridActionEncodeException(e, value.response); }
        }
    };

    public GridActionS2CPacket(GridAction response, Object[] args) {
        Objects.requireNonNull(response);
        this.response = response;
        this.args = args == null ? new Object[0] : args;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.GRID_ACTION_S2C;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        GridActionTask task = response.getTask();
        task.executeAsClient(Grid.client(player), args);
    }
}

