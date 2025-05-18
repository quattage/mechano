package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseHolder;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.LevelReader;

public record LinkResponsePacket(NodeIdentifier.Key start, NodeIdentifier.Key end, LinkResponseHolder lrh, TransmitterType<?> transmitter, Response.Task task) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, LinkResponsePacket> STREAM_CODEC = StreamCodec.composite(
        NodeIdentifier.Key.STREAM_CODEC, LinkResponsePacket::start, 
        NodeIdentifier.Key.STREAM_CODEC, LinkResponsePacket::end,
        LinkResponseHolder.STREAM_CODEC, LinkResponsePacket::lrh,
        TransmitterType.STREAM_CODEC, LinkResponsePacket::transmitter,
        Response.Task.STREAM_CODEC, LinkResponsePacket::task,
        LinkResponsePacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_S2C;
    }


    // TODO Response.Task is unused for now, but may be necessary in the future
    @Override
    public void handle(LocalPlayer player) {

        LevelReader world = player.level();

        PowerGridBlockEntity startBE = start.getHost(world);
        PowerGridBlockEntity endBE = end.getHost(world);

        if(startBE == null) {
            Mechano.LOGGER.error("Couldn't handle LinkResponse from " + start + " to " + end + " - No valid PGBE could be found at the starting address!");
            return;
        } else if(endBE == null) {
            Mechano.LOGGER.error("Couldn't handle LinkResponse from " + start + " to " + end + " - No valid PGBE could be found at the ending address!");
            return;
        }

        startBE.surrogate.sync(world, null);
        startBE.anchors.getByIndex(start.getIndex()).sync(lrh.anchorData()[0], null);
        endBE.surrogate.sync(world, null);
        endBE.anchors.getByIndex(end.getIndex()).sync(lrh.anchorData()[1], null);
        // throw new UnsupportedOperationException("Unsupported packet handler task '" + task + "'");
    }
}
