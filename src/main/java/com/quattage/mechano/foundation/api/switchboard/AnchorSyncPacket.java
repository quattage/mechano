package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.ClientGrid;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.switchboard.AwaitingLinkBuffer.ProcessMode;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSynchronizer;

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
        ClientGrid grid = SidedGridDispatcher.client(player);
        grid.syncSingleAnchor(anchor, ProcessMode.TRY_THEN_SCHEDULE);
    }
}
