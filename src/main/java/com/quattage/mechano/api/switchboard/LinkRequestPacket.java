package com.quattage.mechano.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.SidedGridDispatcher;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.api.identifier.UUIDDiscriminator;
import com.quattage.mechano.api.transmitter.TransmitterType;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;


public record LinkRequestPacket(GridUUID start, GridUUID end, TransmitterType<?> transmitter, GridResponse task) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkRequestPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, LinkRequestPacket::start,
        UUIDDiscriminator.STREAM_CODEC, LinkRequestPacket::end,
        TransmitterType.STREAM_CODEC, LinkRequestPacket::transmitter,
        GridResponse.STREAM_CODEC, LinkRequestPacket::task,
        LinkRequestPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_C2S;
    }

    @Override
    public void handle(ServerPlayer player) {
        ServerGrid global = SidedGridDispatcher.server(player);
        if(!task.indicatesCompletion()) return;
        switch(task) {
            case TASK_CREATE_LINK -> global.createLink(start, end, transmitter);
            case TASK_DESTROY_LINK, TASK_FREE_LINK -> global.destroyLink(start, end);
            case null, default -> GridResponse.logUnhandled(task, this);
        }
    }
}
