package com.quattage.mechano.foundation.api.network;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmarks.DispatchedNode;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record DispatchSyncClientBoundPacket(BlockPos pos, DispatchedNode.SyncTask task) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, DispatchSyncClientBoundPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DispatchSyncClientBoundPacket::pos,
        DispatchedNode.SyncTask.STREAM_CODEC, DispatchSyncClientBoundPacket::task,
        DispatchSyncClientBoundPacket::new
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

        NodeIdentifier.Key address = new NodeIdentifier.Key(pos);
        PowerGridBlockEntity pgbe = address.getHost(player.level());

        if(pgbe == null) {
            Mechano.LOGGER.warn("Failed to handle dispatch status sync at " + address + " - The BlockEntity at this address was not found.");
            return;
        }
        switch(task) {
            case SYNC:
                pgbe.surrogate.sync(world, false);
                break;
            case UNSYNC:
                pgbe.surrogate.forget(false);
                break;
            default:
                break;
        }
    }
}
