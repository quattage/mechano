package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSyncHolder;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record AnchorSyncPacket(AnchorSyncHolder anchor) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorSyncPacket> STREAM_CODEC = StreamCodec.composite(
        AnchorSyncHolder.STREAM_CODEC, AnchorSyncPacket::anchor,
        AnchorSyncPacket::new
    );
    @Override public PacketTypeProvider getTypeProvider() { return MechanoPackets.ANCHOR_SYNC_S2C; }
    @Override public void handle(LocalPlayer player) { 
        anchor.applyAndGet(player.level(), true); 
    }
}
