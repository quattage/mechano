package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.ClientGrid;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributable;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.switchboard.AwaitingLinkBuffer.ProcessMode;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSynchronizer;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterType;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record LinkResponsePacket(AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, GridResponse task, short span) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkResponsePacket> STREAM_CODEC = StreamCodec.composite(
        AnchorSynchronizer.STREAM_CODEC, LinkResponsePacket::start,
        AnchorSynchronizer.STREAM_CODEC, LinkResponsePacket::end,
        TransmitterType.STREAM_CODEC, LinkResponsePacket::trns,
        GridResponse.STREAM_CODEC, LinkResponsePacket::task,
        ByteBufCodecs.SHORT, LinkResponsePacket::span,
        LinkResponsePacket::new
    );

    public static LinkResponsePacket of(GridLink link, GridResponse response) {
        if(!link.hasPoints()) throw new IllegalArgumentException("Can't create a LinkResponsePacket from a link with missing points!");
        return new LinkResponsePacket(
            AnchorSynchronizer.of(link.getStartNode()),
            AnchorSynchronizer.of(link.getEndNode()),
            link.getTransmitter() == null ? MechanoTransmissionTypes.PERFECT_CONDUCTOR : link.getTransmitter().getType(),
            response, CatenaryAttributable.packSpan(link.calculateSpan())
        );
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_S2C;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ClientGrid grid = SidedGridDispatcher.client(player);
        if(!task.indicatesCompletion()) {
            grid.ensureCatenaryDestroyed(start, end);
            return;
        }
        switch(task) {
            case TASK_CREATE_LINK -> grid.handleCatenaryCreation(start, end, CatenaryAttributable.getSpanFromShort(span), trns, ProcessMode.TRY_THEN_SCHEDULE);
            case TASK_SYNC_ANCHORS -> grid.handleCatenarySync(start, end, CatenaryAttributable.getSpanFromShort(span), trns, ProcessMode.TRY_THEN_SCHEDULE);
            case TASK_DESTROY_LINK -> grid.handleCatenaryDestruction(start, end, ProcessMode.TRY_THEN_SCHEDULE);
            case TASK_DESTROY_LINK_LAZY, TASK_FORGET_ANCHORS -> grid.ensureCatenaryDestroyed(start, end);
            case null, default -> GridResponse.logUnhandled(task, this);
        }
    }
}
