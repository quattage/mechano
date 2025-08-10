package com.quattage.mechano.infrastructure.manifest;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.gridapi.SidedGridDispatcher;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public record ManifestResponsePacket(String message) implements ServerboundPacketPayload {

    public static final StreamCodec<ByteBuf, ManifestResponsePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ManifestResponsePacket::message,
        ManifestResponsePacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.MANIFEST_C2S;
    }

    @Override
    public void handle(ServerPlayer player) {
        SidedGridDispatcher.MANIFEST.handleResponse(message);
    }
}
