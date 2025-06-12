package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifier;
import com.quattage.mechano.foundation.api.landmark.client.AnchorPoint;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AnchorPointSyncPacket(NodeIdentifier.Key address, byte connections, boolean enabled) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, AnchorPointSyncPacket> STREAM_CODEC = StreamCodec.composite(
        NodeIdentifier.Key.STREAM_CODEC, AnchorPointSyncPacket::address,
        ByteBufCodecs.BYTE, AnchorPointSyncPacket::connections,
        ByteBufCodecs.BOOL, AnchorPointSyncPacket::enabled,
        AnchorPointSyncPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.ANCHOR_SYNC_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {
        AnchorPoint anchor = AnchorPoint.retrieve(player.level(), address);
        if(anchor == null)
            return;
        anchor.sync(connections, null);
    }
}
