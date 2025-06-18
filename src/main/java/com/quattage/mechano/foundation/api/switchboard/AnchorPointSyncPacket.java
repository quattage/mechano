package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.DiscriminatorData;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AnchorPointSyncPacket(GridUUID addr, byte connections, boolean enabled) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorPointSyncPacket> STREAM_CODEC = StreamCodec.composite(
        DiscriminatorData.STREAM_CODEC, AnchorPointSyncPacket::addr,
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
        AnchorPoint anchor = addr.getAnchor((ClientLevel)player.level());
        if(anchor == null) return;
        anchor.sync(connections, null);
    }
}
