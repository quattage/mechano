package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public record AnchorSurrogateDestroyPacket(GridUUID addr) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorSurrogateDestroyPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, AnchorSurrogateDestroyPacket::addr,
        AnchorSurrogateDestroyPacket::new
    );
    @Override public PacketTypeProvider getTypeProvider() { return MechanoPackets.ANCHOR_DESTROY_C2S; }
    @Override public void handle(ServerPlayer player) { 
        Griddable<?> points = addr.getAnchorPoints(player.level());
        if(points == null) return;
        points.destroySurrogate();
    }
}
