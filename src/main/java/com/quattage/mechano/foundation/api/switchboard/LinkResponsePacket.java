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


    @Override
    public void handle(LocalPlayer player) {
        LevelReader world = player.level();
        PowerGridBlockEntity startBE = start.getHost(world);
        PowerGridBlockEntity endBE = end.getHost(world);
        if(task == Response.Task.CHUNK_LOAD) {
            handleAsymmetric(world, startBE);
            return;
        }
        handleDoubleSided(world, startBE, endBE);
    }


    private void handleDoubleSided(LevelReader world, PowerGridBlockEntity startBE, PowerGridBlockEntity endBE) {
        if(endBE == null && startBE != null) {
            startBE.surrogate.sync(world, null);
            startBE.anchors.getByIndex(start.getIndex()).sync(lrh.anchorData()[0], null);
            Mechano.LOGGER.error("Couldn't handle LinkResponse '" + task + "' from " + start + " to " + end + " - No valid PGBE could be found at the ending address, got " + startBE);
            return;
        } else if(startBE == null && endBE != null) {
            endBE.surrogate.sync(world, null);
            endBE.anchors.getByIndex(end.getIndex()).sync(lrh.anchorData()[1], null);
            Mechano.LOGGER.error("Couldn't handle LinkResponse '" + task + "' from " + start + " to " + end + " - No valid PGBE could be found at the starting address, got " + endBE);
            return;
        } else if(endBE == null && startBE == null) {
            Mechano.LOGGER.error("Couldn't handle LinkResponse '" + task + "' from " + start + " to " + end + " - No valid PGBE could be found at either address!");
            return;
        }

        startBE.surrogate.sync(world, null);
        startBE.anchors.getByIndex(start.getIndex()).sync(lrh.anchorData()[0], null);
        endBE.surrogate.sync(world, null);
        endBE.anchors.getByIndex(end.getIndex()).sync(lrh.anchorData()[1], null);
    }

    private void handleAsymmetric(LevelReader world, PowerGridBlockEntity be) {
        if(be == null)
            throw new NullPointerException("Couldn't handle LinkResponse '" + task + "' from " + start + " to " + end + " - No valid PGBE could be found at the starting address");
        be.surrogate.sync(world, null);
        be.anchors.getByIndex(start.getIndex()).sync(lrh.anchorData()[0], null);
    }
}
