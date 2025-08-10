package com.quattage.mechano.foundation.gridapi.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.gridapi.switchboard.GridResponse.AnchorSynchronizer;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record AnchorSyncPacket(AnchorSynchronizer anchor) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorSyncPacket> STREAM_CODEC = StreamCodec.composite(
        AnchorSynchronizer.STREAM_CODEC, AnchorSyncPacket::anchor,
        AnchorSyncPacket::new
    );
    @Override public PacketTypeProvider getTypeProvider() { return MechanoPackets.ANCHOR_SYNC_S2C; }
    @Override public void handle(LocalPlayer player) { 
        anchor.applyAndGet(player.level(), false); 
    }
}
