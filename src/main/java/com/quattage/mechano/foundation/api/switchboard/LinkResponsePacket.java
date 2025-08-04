package com.quattage.mechano.foundation.api.switchboard;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.ClientGrid;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSyncHolder;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record LinkResponsePacket(AnchorSyncHolder start, AnchorSyncHolder end, TransmitterType<?> trns, GridResponse response) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkResponsePacket> STREAM_CODEC = StreamCodec.composite(
        AnchorSyncHolder.STREAM_CODEC, LinkResponsePacket::start,
        AnchorSyncHolder.STREAM_CODEC, LinkResponsePacket::end,
        TransmitterType.STREAM_CODEC, LinkResponsePacket::trns,
        GridResponse.STREAM_CODEC, LinkResponsePacket::response,
        LinkResponsePacket::new
    );

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
        ClientGrid grid = SidedGridDispatcher.client(player);
        switch(response) {
            case TASK_CREATE_LINK -> grid.handleCatenaryCreation(start, end, trns, true);
            case TASK_SYNC_ANCHORS -> grid.handleCatenarySync(start, end, trns, true);
            case TASK_DESTROY_LINK -> grid.handleCatenaryDestruction(start, end);
            case null, default -> Mechano.LOGGER.error("No valid response could be provided for link task '" + response + "!'");
        }
    }
}
