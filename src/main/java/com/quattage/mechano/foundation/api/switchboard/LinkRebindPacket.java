package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.landmark.Connection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseDoubleHolder;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.LevelReader;

public record LinkRebindPacket(GridUUID start, GridUUID end, GridUUID newEnd, LinkResponseDoubleHolder lrh) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkRebindPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, LinkRebindPacket::start,
        UUIDDiscriminator.STREAM_CODEC, LinkRebindPacket::end,
        UUIDDiscriminator.STREAM_CODEC, LinkRebindPacket::newEnd,
        LinkResponseDoubleHolder.STREAM_CODEC, LinkRebindPacket::lrh,
        LinkRebindPacket::new
    );

    @Override
    
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_REBIND_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {

        LevelReader world = player.level();
        AnchorPointable<?> startPoints = start.getAnchorPoints(world);
        AnchorPointable<?> endPoints = end.getAnchorPoints(world);
        AnchorPointable<?> newEndPoints = newEnd.getAnchorPoints(world);

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

        AnchorPoint newEndAnchor = null;
        if(newEndPoints != null) {
            newEndPoints.getSurrogate().sync(world, null);
            newEndAnchor = newEndPoints.getAnchor(start.getIndex());
            if(newEndAnchor != null) newEndAnchor.sync(lrh.anchorData()[2], null);
        }

        ConnectionKey key = new ConnectionKey(start, end);
        GridCatenary cat = (GridCatenary)key.findIn(world);
        if(cat == null) {
            Mechano.LOGGER.warn("Can't rebind catenary at [" + start + " -> " + newEnd + "] - No catenary exists between these addresses!");
            return;
        }

        cat.rebindEndpoint(world, newEndAnchor);
    }
}
