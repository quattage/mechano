package com.quattage.mechano.api.switchboard;

import java.util.Objects;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.switchboard.action.ActionTask;
import com.quattage.mechano.api.switchboard.action.ActionType.GridActionDecodeException;
import com.quattage.mechano.api.switchboard.action.ActionType.GridActionEncodeException;
import com.quattage.mechano.api.switchboard.action.GridAction;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class GridActionC2SPacket implements ServerboundPacketPayload {

    private final GridAction action;
    private final Object[] args;
    
    public static final StreamCodec<ByteBuf, GridActionC2SPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override 
        public GridActionC2SPacket decode(ByteBuf buffer) { 
            byte idx = buffer.readByte();
            if(idx < 0 || idx >= GridAction.values().length) {
                throw new IndexOutOfBoundsException("Failed while decoding response task - response index " + idx 
                    + " is out of bounds for a response registry of " + GridAction.values().length + " members!");
            }
            GridAction response = GridAction.values()[idx];
            ActionTask task = response.getTask();
            if(task == null) throw new IllegalArgumentException("Failed while decoding response task '" + response + "' - This response type didn't produce a task!");
            Object[] decodedArgs = null;
            try { decodedArgs = task.dynamicDecode(buffer); }
            catch(RuntimeException e) { throw new GridActionDecodeException(e, response); }
            if(decodedArgs == null) decodedArgs = new Object[0];
            return new GridActionC2SPacket(response, decodedArgs);
        }

        @Override 
        public void encode(ByteBuf buffer, GridActionC2SPacket value) { 
            buffer.writeByte(value.action.ordinal()); 
            ActionTask task = value.action.getTask();
            if(task == null) throw new IllegalArgumentException("Failed while encoding response task '" + value.action + "' - This response type didn't produce a task!");
            try { task.dynamicEncode(value.args, buffer); }
            catch(RuntimeException e) { throw new GridActionEncodeException(e, value.action); }
        }
    };

    public GridActionC2SPacket(GridAction response, Object[] args) {
        Objects.requireNonNull(response);
        this.action = response;
        this.args = args == null ? new Object[0] : args;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.GRID_ACTION_C2S;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(ServerPlayer player) {
        ActionTask task = action.getTask();
        ServerGrid grid = Grid.server(player);
        GridAction result = task.executeAsServer(grid, args);
        if(GridAction.VERBOSE_LOGS) grid.debug("Handled " + action + " in " + grid.getDimensionName() + ":\n\n**Arguments: \n" + task.collectArgsAsString(args) + "\n\n** Result: \n(" + result.asResource() + ")");
    }
}
