package com.quattage.mechano.foundation.api.network;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifiable;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier;
import com.quattage.mechano.foundation.api.transmission.Transmitter;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


public record LinkRequestPacket(NodeIdentifier.Key sideA, NodeIdentifier.Key sideB, Transmitter transmitter) implements ServerboundPacketPayload {

    public static final StreamCodec<ByteBuf, LinkRequestPacket> STREAM_CODEC = StreamCodec.composite(
        NodeIdentifier.Key.STREAM_CODEC, LinkRequestPacket::sideA, 
        NodeIdentifier.Key.STREAM_CODEC, LinkRequestPacket::sideB,
        TransmitterType.STREAM_CODEC, LinkRequestPacket::transmitter,
        LinkRequestPacket::new
    );

    public static void send(NodeIdentifiable<?> sideA, NodeIdentifiable<?> sideB, Transmitter transmitter) {
        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(sideA.strip(), sideB.strip(), transmitter));
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_C2S;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(ServerPlayer player) {
        SidedGridDispatcher.runOnServer(player, grid -> {
            grid.log("HELLO! " + transmitter);
        });
    }
}
