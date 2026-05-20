package com.quattage.mechano.switchboard;

import java.util.Objects;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.grid.ClientGrid;
import com.quattage.mechano.grid.Grid;
import com.quattage.mechano.switchboard.action.ActionTask;
import com.quattage.mechano.switchboard.action.ActionType.GridActionDecodeException;
import com.quattage.mechano.switchboard.action.ActionType.GridActionEncodeException;
import com.quattage.mechano.switchboard.action.GridAction;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record GridActionS2CPacket(GridAction action, Object[] args) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, GridActionS2CPacket> STREAM_CODEC = new StreamCodec<>() {

        @Override 
        public GridActionS2CPacket decode(ByteBuf buffer) { 
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
            catch (RuntimeException e) { throw new GridActionDecodeException(e, response); }
            if(decodedArgs == null) decodedArgs = new Object[0];
            return new GridActionS2CPacket(response, decodedArgs);
        }

        @Override 
        public void encode(ByteBuf buffer, GridActionS2CPacket value) { 
            buffer.writeByte(value.action.ordinal()); 
            ActionTask task = value.action.getTask();
            if(task == null) throw new IllegalArgumentException("Failed while encoding response task '" + value.action + "' - This response type didn't produce a task!");
            try { task.dynamicEncode(value.args, buffer); }
            catch (RuntimeException e) { throw new GridActionEncodeException(e, value.action); }
        }
    };

    public GridActionS2CPacket(GridAction action, Object[] args) {
        Objects.requireNonNull(action);
        this.action = action;
        this.args = args == null ? new Object[0] : args;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.GRID_ACTION_S2C;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ActionTask task = action.getTask();
        ClientGrid grid = Grid.client(player.level());
        GridAction result = task.executeAsClient(grid, args);
        if(GridAction.VERBOSE_LOGS) grid.debug("Handled " + action + " in " + grid.getDimensionName() + ":\n\n**Arguments: \n" + task.collectArgsAsString(args) + "\n\n** Result: \n(" + result.asResource() + ")");
    }
}

