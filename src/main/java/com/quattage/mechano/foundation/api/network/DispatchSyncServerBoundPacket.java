package com.quattage.mechano.foundation.api.network;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmarks.DispatchedNode;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record DispatchSyncServerBoundPacket(BlockPos pos,DispatchedNode.SyncTask task) implements ServerboundPacketPayload {

    public static final StreamCodec<ByteBuf, DispatchSyncServerBoundPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DispatchSyncServerBoundPacket::pos,
        DispatchedNode.SyncTask.STREAM_CODEC, DispatchSyncServerBoundPacket::task,
        DispatchSyncServerBoundPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.DISPATCH_SYNC_S2C;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(ServerPlayer player) {
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
                pgbe.surrogate.sync(false);
                break;
            case UNSYNC:
                pgbe.surrogate.forget(false);
                break;
            default:
                break;
        }
    }
}
