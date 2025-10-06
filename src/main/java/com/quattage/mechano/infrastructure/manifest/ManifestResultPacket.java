package com.quattage.mechano.infrastructure.manifest;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ManifestResultPacket(String message) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, ManifestResultPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ManifestResultPacket::message,
        ManifestResultPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.MANIFEST_RESULT_S2C;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        SidedGridDispatcher.MANIFEST.handleComplete(player, message);
    }
}
