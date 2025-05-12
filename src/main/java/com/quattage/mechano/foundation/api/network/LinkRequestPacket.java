package com.quattage.mechano.foundation.api.network;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier;
import com.quattage.mechano.foundation.api.transmission.Transmitable.LinkResponse;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


public record LinkRequestPacket(NodeIdentifier.Key sideA, NodeIdentifier.Key sideB, TransmitterType<?> transmitter, Task task) implements ServerboundPacketPayload {

    public static final StreamCodec<ByteBuf, LinkRequestPacket> STREAM_CODEC = StreamCodec.composite(
        NodeIdentifier.Key.STREAM_CODEC, LinkRequestPacket::sideA, 
        NodeIdentifier.Key.STREAM_CODEC, LinkRequestPacket::sideB,
        TransmitterType.STREAM_CODEC, LinkRequestPacket::transmitter,
        Task.STREAM_CODEC, LinkRequestPacket::task,
        LinkRequestPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_C2S;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(ServerPlayer player) {
        SidedGridDispatcher.runOnServer(player, global -> {

            LinkResponse response = null;

            if(task == Task.CREATE) {
                response = global.createLink(sideA, sideB, transmitter);
                return;
            }

            if(task == Task.DESTROY) {
                return;
            }

            throw new UnsupportedOperationException("Task type '" + task + "' hasn't been implemented!");
        });
    }

    public static enum Task {
        CREATE,
        DESTROY,
        RESYNC,
        ;

        public static final StreamCodec<ByteBuf, Task> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Task decode(ByteBuf buffer) {
                return Task.values()[buffer.readByte()];
            }
            @Override
            public void encode(ByteBuf buffer, Task value) {
                buffer.writeByte(value.ordinal());
            }
        };
    }
}
