package com.quattage.mechano.foundation.gridapi.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.gridapi.LinkDataStorable;
import com.quattage.mechano.foundation.gridapi.landmark.GridCatenary;
import com.quattage.mechano.foundation.gridapi.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.UUIDDiscriminator;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record LinkSwapPacket(GridUUID start, GridUUID end, GridUUID replace, GridResponse task) implements ClientboundPacketPayload {
    
    public static final StreamCodec<RegistryFriendlyByteBuf, LinkSwapPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, LinkSwapPacket::start,
        UUIDDiscriminator.STREAM_CODEC, LinkSwapPacket::end,
        UUIDDiscriminator.STREAM_CODEC, LinkSwapPacket::replace,
        GridResponse.STREAM_CODEC, LinkSwapPacket::task,
        LinkSwapPacket::new
    );

    public static LinkSwapPacket ofStart(GridUUID start, GridUUID end, GridUUID newStart) {
        return new LinkSwapPacket(start, end, newStart, GridResponse.TASK_SWAP_START);
    }

    public static LinkSwapPacket ofEnd(GridUUID start, GridUUID end, GridUUID newEnd) {
        return new LinkSwapPacket(start, end, newEnd, GridResponse.TASK_SWAP_END);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_SWAP_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {

        ClientLevel world = (ClientLevel)player.level();
        ConnectionKey key = new ConnectionKey(start, end);
        GridCatenary cat = LinkDataStorable.getAsClient(player.level(), key);
        if(cat == null) throw new IllegalStateException("Couldn't find link matching " + key);

        switch(task) {
            case TASK_SWAP_START -> cat.replaceAddresses(world, replace, end);
            case TASK_SWAP_END -> cat.replaceAddresses(world, start, replace);
            case null, default -> GridResponse.logUnhandled(this, task);
        }
    }
}
