package com.quattage.mechano.foundation.api.switchboard;

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

public record DispatchSyncPacket(BlockPos pos, DispatchedNode.SidedTask task) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, DispatchSyncPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DispatchSyncPacket::pos,
        DispatchedNode.SidedTask.STREAM_CODEC, DispatchSyncPacket::task,
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

        NodeIdentifier.Key address = new NodeIdentifier.Key(pos);
        PowerGridBlockEntity pgbe = address.getHost(player.level());

        if(pgbe == null)
            return;

        switch(task) {
            case SYNC:
                pgbe.surrogate.sync(world, null);
                break;
            case UNSYNC:
                pgbe.surrogate.forget(world);
                break;
            default:
                break;
        }
    }
}
