package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


public record LinkRequestPacket(GridUUID start, GridUUID end, TransmitterType<?> transmitter, UpdateResponse task) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkRequestPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, LinkRequestPacket::start,
        UUIDDiscriminator.STREAM_CODEC, LinkRequestPacket::end,
        TransmitterType.STREAM_CODEC, LinkRequestPacket::transmitter,
        UpdateResponse.STREAM_CODEC, LinkRequestPacket::task,
        LinkRequestPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_C2S;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(ServerPlayer player) {
        ServerGrid global = SidedGridDispatcher.server(player);
        if(!task.indicatesCompletion()) return;
        switch(task) {
            case TASK_CREATE_LINK -> global.createLink(start, end, transmitter);
            case TASK_DESTROY_LINK -> global.destroyLink(start, end);
            case TASK_FREE_LINK -> {
                global.destroyLink(start, end);
            }
            case null, default -> Mechano.LOGGER.warn("No valid response could be provided for link task '" + task + "!'");
        }
    }
}
