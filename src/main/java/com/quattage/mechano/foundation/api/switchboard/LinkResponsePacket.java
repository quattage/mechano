package com.quattage.mechano.foundation.api.switchboard;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSyncHolder;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public record LinkResponsePacket(AnchorSyncHolder start, AnchorSyncHolder end, TransmitterType<?> transmitter, GridResponse response) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkResponsePacket> STREAM_CODEC = StreamCodec.composite(
        AnchorSyncHolder.STREAM_CODEC, LinkResponsePacket::start,
        AnchorSyncHolder.STREAM_CODEC, LinkResponsePacket::end,
        TransmitterType.STREAM_CODEC, LinkResponsePacket::transmitter,
        GridResponse.STREAM_CODEC, LinkResponsePacket::response,
        LinkResponsePacket::new
    );

    public static LinkResponsePacket ofAsymmetricSuccess(GridUUID start, GridNode end) {
        return new LinkResponsePacket(
            AnchorSyncHolder.of(start),
            AnchorSyncHolder.of(end),
            MechanoTransmissionTypes.PERFECT_CONDUCTOR,
            GridResponse.TASK_DESTROY_LINK
        );
    }

    public static LinkResponsePacket ofAsymmetricSuccess(GridNode start, GridUUID end) {
        return new LinkResponsePacket(
            AnchorSyncHolder.of(start),
            AnchorSyncHolder.of(end),
            MechanoTransmissionTypes.PERFECT_CONDUCTOR,
            GridResponse.TASK_DESTROY_LINK
        );
    }

    public static LinkResponsePacket of(GridNode start, GridNode end, @Nullable Transmitter<?> trns, GridResponse response) {
        return new LinkResponsePacket(
            AnchorSyncHolder.of(start),
            AnchorSyncHolder.of(end),
            trns == null ? MechanoTransmissionTypes.PERFECT_CONDUCTOR : trns.getType(),
            response
        );
    }

    public static LinkResponsePacket of(GridLink link, GridResponse response) {
        if(!link.hasPoints()) throw new IllegalArgumentException("Can't create a LinkResponsePacket from a link with missing points!");
        return new LinkResponsePacket(
            AnchorSyncHolder.of(link.getStartNode()),
            AnchorSyncHolder.of(link.getEndNode()),
            link.getTransmitter() == null ? MechanoTransmissionTypes.PERFECT_CONDUCTOR : link.getTransmitter().getType(),
            response
        );
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {

        if(!response.indicatesCompletion()) return;
        Level world = player.level();
        AnchorPoint startAnchor;
        AnchorPoint endAnchor;

        switch(response) {
            case TASK_CREATE_LINK -> {
                startAnchor = start.applyAndGet(world);
                endAnchor = end.applyAndGet(world);
                Mechano.LOGGER.info("PUSHING " + startAnchor.getAddress() + " -> " + endAnchor.getAddress());
                GridCatenary cat = new GridCatenary(world, startAnchor, endAnchor, transmitter(), CatenaryAttributes.Initializer.FRESH_SIMULATION);
                LinkDataStorable.put(world, cat);
            }
            case TASK_SYNC_ANCHORS -> {
                startAnchor = start.applyAndGet(world);
                endAnchor = end.applyAndGet(world);
                ConnectionKey key = new ConnectionKey(start, end);
                GridCatenary cat = LinkDataStorable.getAsClient(world, key);
                if(cat == null) {
                    if(!AnchorSyncHolder.assertAnchorsExist(response, startAnchor, endAnchor)) return;
                    cat = new GridCatenary(world, startAnchor, endAnchor, transmitter(), CatenaryAttributes.Initializer.RESTING_SIMULATION);
                    LinkDataStorable.put(world, cat);
                }
                else cat.reinitializeModel(world, CatenaryAttributes.Initializer.RESTING_SIMULATION);
            } 
            case TASK_DESTROY_LINK -> {
                startAnchor = start.applyAndGet(world, true);
                endAnchor = end.applyAndGet(world, true);
                ConnectionKey key = new ConnectionKey(start, end);
                LinkDataStorable.remove(world, key);
            }
            case TASK_FREE_LINK -> {
                Mechano.LOGGER.error("TODO IMPLEMENT TASK_FREE_LINK DUMBASS");
            }
            case null, default -> Mechano.LOGGER.warn("No valid response could be provided for link task '" + response + "!'");
        }
    }
}
