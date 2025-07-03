package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.landmark.Connection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseHolder;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.LevelReader;

public record LinkResponsePacket(GridUUID start, GridUUID end, LinkResponseHolder lrh, TransmitterType<?> transmitter, Response.Task task) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkResponsePacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, LinkResponsePacket::start,
        UUIDDiscriminator.STREAM_CODEC, LinkResponsePacket::end,
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
        AnchorPointable<?> startPoints = start.getAnchorPoints(world);
        AnchorPointable<?> endPoints = end.getAnchorPoints(world);

        AnchorPoint startAnchor = null;
        if(startPoints != null) {
            startPoints.getSurrogate().sync(world, null);
            startAnchor = startPoints.getAnchor(start.getIndex());
            if(startAnchor != null) startAnchor.sync(lrh.anchorData()[0], null);
        }

        AnchorPoint endAnchor = null;
        if(endPoints != null) {
            endPoints.getSurrogate().sync(world, null);
            endAnchor = endPoints.getAnchor(start.getIndex());
            if(endAnchor != null) endAnchor.sync(lrh.anchorData()[1], null);
        }

        if(!lrh.response().indicatesSuccess()) return;

        switch(task) {
            case CREATE -> {
                ConnectionKey key = new ConnectionKey(start, end);
                GridCatenary cat = (GridCatenary)key.findIn(world);
                if(cat == null) {
                    if(!assertAnchorsExist(world, startAnchor, endAnchor)) return;
                    cat = new GridCatenary(world, startAnchor, endAnchor, transmitter(), false);
                }
                cat.pushTo(world);
            }
            case SYNC -> {
                ConnectionKey key = new ConnectionKey(start, end);
                GridCatenary cat = (GridCatenary)key.findIn(world);
                if(cat == null) {
                    if(!assertAnchorsExist(world, startAnchor, endAnchor)) return;
                    cat = new GridCatenary(world, startAnchor, endAnchor, transmitter(), false);
                    cat.pushTo(world);
                }
                cat.prebuildWire(world);
            }
            case DESTROY, UNSYNC -> {
                ConnectionKey key = new ConnectionKey(start, end);
                GridCatenary cat = (GridCatenary)key.findIn(world);
                if(cat == null) return;
                cat.removeFrom(world);
            }
            case RELEASE_END -> {
                Mechano.LOGGER.error("LinkRepsonse packet with task '" + task + "' failed to handle - Task is not supported by this packet!");
            }
        }
    }


    private boolean assertAnchorsExist(LevelReader world, AnchorPoint startAnchor, AnchorPoint endAnchor) {
        if(startAnchor == null && endAnchor == null) {
            Mechano.LOGGER.error("LinkRepsonse packet with task '" + task + "' failed to handle - Context is missing both start and end AnchorPoints for link (" + start + " -> " + end.toString());
            return false;
        }
        if(startAnchor == null) {
            Mechano.LOGGER.error("LinkRepsonse packet with task '" + task + "' failed to handle - Context is missing starting AnchorPoint for link (" + start + " -> " + end.toString());
            return false;
        }
        if(endAnchor == null) {
            Mechano.LOGGER.error("LinkRepsonse packet with task '" + task + "' failed to handle - Context is missing ending AnchorPoint for link (" + start + " -> " + end.toString());
            return false;
        }
        return true;
    }
}
