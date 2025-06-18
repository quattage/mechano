package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.anchor.DispatchedAnchorNode;
import com.quattage.mechano.foundation.api.landmark.DiscriminatorData;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record DispatchSyncPacket(GridUUID addr, Response.Task task) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, DispatchSyncPacket> STREAM_CODEC = StreamCodec.composite(
        DiscriminatorData.STREAM_CODEC, DispatchSyncPacket::addr,
        Response.Task.STREAM_CODEC, DispatchSyncPacket::task,
        DispatchSyncPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.DISPATCH_SYNC_C2S;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        LevelReader world = player.level();
        if(world == null) return;
        DispatchedAnchorNode surrogate = addr.getSurrogate(world);
        if(surrogate == null) return;

        switch(task) {
            case SYNC:
                surrogate.sync(world, null);
                break;
            case UNSYNC:
                surrogate.forget(world);
                break;
            default:
                break;
        }
    }
}
