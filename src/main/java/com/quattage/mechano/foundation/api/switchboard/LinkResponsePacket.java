package com.quattage.mechano.foundation.api.switchboard;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.ClientGrid;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.switchboard.AwaitingLinkBuffer.ProcessMode;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSynchronizer;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterType;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record LinkResponsePacket(AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, GridResponse task) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkResponsePacket> STREAM_CODEC = StreamCodec.composite(
        AnchorSynchronizer.STREAM_CODEC, LinkResponsePacket::start,
        AnchorSynchronizer.STREAM_CODEC, LinkResponsePacket::end,
        TransmitterType.STREAM_CODEC, LinkResponsePacket::trns,
        GridResponse.STREAM_CODEC, LinkResponsePacket::task,
        LinkResponsePacket::new
    );

    public static LinkResponsePacket of(GridNode start, GridNode end, @Nullable Transmitter<?> trns, GridResponse response) {
        return new LinkResponsePacket(
            AnchorSynchronizer.of(start),
            AnchorSynchronizer.of(end),
            trns == null ? MechanoTransmissionTypes.PERFECT_CONDUCTOR : trns.getType(),
            response
        );
    }

    public static LinkResponsePacket of(GridLink link, GridResponse response) {
        if(!link.hasPoints()) throw new IllegalArgumentException("Can't create a LinkResponsePacket from a link with missing points!");
        return new LinkResponsePacket(
            AnchorSynchronizer.of(link.getStartNode()),
            AnchorSynchronizer.of(link.getEndNode()),
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
        ClientGrid grid = SidedGridDispatcher.client(player);
        if(!task.indicatesCompletion()) {
            grid.ensureCatenaryDestroyed(start, end);
            return;
        }
        switch(task) {
            case TASK_CREATE_LINK -> grid.handleCatenaryCreation(start, end, trns, ProcessMode.TRY_THEN_SCHEDULE);
            case TASK_SYNC_ANCHORS -> grid.handleCatenarySync(start, end, trns, ProcessMode.TRY_THEN_SCHEDULE);
            case TASK_DESTROY_LINK -> grid.handleCatenaryDestruction(start, end, ProcessMode.TRY_THEN_SCHEDULE);
            case TASK_DESTROY_LINK_LAZY -> grid.ensureCatenaryDestroyed(start, end);
            case null, default -> GridResponse.logUnhandled(task, this);
        }
    }
}
