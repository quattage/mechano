package com.quattage.mechano.api.switchboard;

import java.util.Objects;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.switchboard.action.GridActionTask;
import com.quattage.mechano.api.switchboard.action.GridActionType.GridActionDecodeException;
import com.quattage.mechano.api.switchboard.action.GridActionType.GridActionEncodeException;
import com.quattage.mechano.api.switchboard.action.GridActions;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record GridActionC2SPacket(GridActions response, Object[] args) implements ServerboundPacketPayload {
    public static final StreamCodec<ByteBuf, GridActionC2SPacket> STREAM_CODEC = new StreamCodec<>() {

        @Override 
        public GridActionC2SPacket decode(ByteBuf buffer) { 
            byte idx = buffer.readByte();
            if(idx < 0 || idx >= GridActions.values().length) {
                throw new IndexOutOfBoundsException("Failed while decoding response task - response index " + idx 
                    + " is out of bounds for a response registry of " + GridActions.values().length + " members!");
            }
            GridActions response = GridActions.values()[buffer.readByte()];
            GridActionTask task = response.getTask();
            if(task == null) throw new IllegalArgumentException("Failed while decoding response task '" + response + "' - This response type didn't produce a task!");
            Object[] decodedArgs = null;
            try { decodedArgs = task.decode(buffer); }
            catch(RuntimeException e) { throw new GridActionDecodeException(e, response); }
            if(decodedArgs == null) decodedArgs = new Object[0];
            return new GridActionC2SPacket(response, decodedArgs);
        }

        @Override 
        public void encode(ByteBuf buffer, GridActionC2SPacket value) { 
            buffer.writeByte(value.response.ordinal()); 
            GridActionTask task = value.response.getTask();
            if(task == null) throw new IllegalArgumentException("Failed while encoding response task '" + value.response + "' - This response type didn't produce a task!");
            try { task.encode(value.args, buffer); }
            catch(RuntimeException e) { throw new GridActionEncodeException(e, value.response); }
        }
    };

    public GridActionC2SPacket(GridActions response, Object[] args) {
        Objects.requireNonNull(response);
        this.response = response;
        this.args = args == null ? new Object[0] : args;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(ServerPlayer player) {
        GridActionTask task = response.getTask();
        task.executeAsServer(Grid.server(player), args);
    }
}
