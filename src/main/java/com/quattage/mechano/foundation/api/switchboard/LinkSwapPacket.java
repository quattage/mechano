package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.ClientGrid;
import com.quattage.mechano.foundation.api.LinkDataStorage.DataScope;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.AwaitingLinkBuffer.ProcessMode;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSynchronizer;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record LinkSwapPacket(AnchorSynchronizer start, AnchorSynchronizer end, GridUUID replace, GridResponse task) implements ClientboundPacketPayload {
    
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkSwapPacket> STREAM_CODEC = StreamCodec.composite(
        AnchorSynchronizer.STREAM_CODEC, LinkSwapPacket::start,
        AnchorSynchronizer.STREAM_CODEC, LinkSwapPacket::end,
        UUIDDiscriminator.STREAM_CODEC, LinkSwapPacket::replace,
        GridResponse.STREAM_CODEC, LinkSwapPacket::task,
        LinkSwapPacket::new
    );

    public static LinkSwapPacket ofStart(GridNode start, GridNode end, GridUUID newStart) {
        return new LinkSwapPacket(AnchorSynchronizer.of(start), AnchorSynchronizer.of(end), newStart, GridResponse.TASK_SWAP_START);
    }

    public static LinkSwapPacket ofEnd(GridNode start, GridNode end, GridUUID newEnd) {
        return new LinkSwapPacket(AnchorSynchronizer.of(start), AnchorSynchronizer.of(end), newEnd, GridResponse.TASK_SWAP_END);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_SWAP_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {
        ClientGrid grid = SidedGridDispatcher.client(player);
        replace.setDataScope(DataScope.BLOCKENTITY);
        switch(task) {
            case TASK_SWAP_START -> grid.swapStartingPoint(start, end, replace, ProcessMode.TRY_THEN_SCHEDULE);
            case TASK_SWAP_END -> grid.swapEndingPoint(start, end, replace, ProcessMode.TRY_THEN_SCHEDULE);
            case null, default -> GridResponse.logUnhandled(task, this);
        }
    }
}
