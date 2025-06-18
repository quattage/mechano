package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.landmark.DiscriminatorData;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseHolder;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.LevelReader;

public record LinkResponsePacket(GridUUID start, GridUUID end, LinkResponseHolder lrh, TransmitterType<?> transmitter, Response.Task task) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkResponsePacket> STREAM_CODEC = StreamCodec.composite(
        DiscriminatorData.STREAM_CODEC, LinkResponsePacket::start,
        DiscriminatorData.STREAM_CODEC, LinkResponsePacket::end,
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
        AnchorPointable startHost = start.getHolder(world);
        AnchorPointable endHost = end.getHolder(world);
        if(task == Response.Task.CHUNK_LOAD) {
            handleAsymmetric(world, startHost);
            return;
        }
        handleDoubleSided(world, startHost, endHost);
    }


    private void handleDoubleSided(LevelReader world, AnchorPointable startHost, AnchorPointable endHost) {
        if(endHost == null && startHost != null) {
            startHost.getSurrogate().sync(world, null);
            startHost.getAnchors().getByIndex(start.getIndex()).sync(lrh.anchorData()[0], null);
            Mechano.LOGGER.error("Couldn't handle LinkResponse '" + task + "' from " + start + " to " + end + " - No valid PGBE could be found at the ending address, got " + startHost);
            return;
        } else if(startHost == null && endHost != null) {
            endHost.getSurrogate().sync(world, null);
            endHost.getAnchors().getByIndex(end.getIndex()).sync(lrh.anchorData()[1], null);
            Mechano.LOGGER.error("Couldn't handle LinkResponse '" + task + "' from " + start + " to " + end + " - No valid PGBE could be found at the starting address, got " + endHost);
            return;
        } else if(endHost == null && startHost == null) {
            Mechano.LOGGER.error("Couldn't handle LinkResponse '" + task + "' from " + start + " to " + end + " - No valid PGBE could be found at either address!");
            return;
        }

        startHost.getSurrogate().sync(world, null);
        startHost.getAnchors().getByIndex(start.getIndex()).sync(lrh.anchorData()[0], null);
        endHost.getSurrogate().sync(world, null);
        endHost.getAnchors().getByIndex(end.getIndex()).sync(lrh.anchorData()[1], null);
    }

    private void handleAsymmetric(LevelReader world,  AnchorPointable host) {
        if(host == null)
            throw new NullPointerException("Couldn't handle LinkResponse '" + task + "' from " + start + " to " + end + " - No valid PGBE could be found at the starting address");
        host.getSurrogate().sync(world, null);
        host.getAnchors().getByIndex(start.getIndex()).sync(lrh.anchorData()[0], null);
    }
}
