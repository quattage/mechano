package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.Connection;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseDoubleHolder;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseHolder;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.entity.GriddableEntity;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;


public record LinkRequestPacket(GridUUID start, GridUUID end, TransmitterType<?> transmitter, Response.Task task) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, LinkRequestPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, LinkRequestPacket::start,
        UUIDDiscriminator.STREAM_CODEC, LinkRequestPacket::end,
        TransmitterType.STREAM_CODEC, LinkRequestPacket::transmitter,
        Response.Task.STREAM_CODEC, LinkRequestPacket::task,
        LinkRequestPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.LINK_C2S;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(ServerPlayer player) {
        
        ServerGrid global = SidedGridDispatcher.server(player);
        LinkResponseHolder lrh = null;
        LinkResponseDoubleHolder lrhd = null;
        GridUUID newEnd = null;

        switch(task) {
            case CREATE, SYNC -> lrh = global.createLink(start, end, transmitter);
            case DESTROY, UNSYNC -> lrh = global.destroyLink(start, end);
            case RELEASE_END -> {
                GriddableEntity ge = GriddableEntity.of(global.getWorld(), end.getPos(global.getWorld()));
                newEnd = ge.createAddress();
                lrhd = global.destroyLink(start, end, newEnd);
            }
        }
        if(lrh == null && lrhd == null)  {
            Mechano.LOGGER.warn("No valid response could be provided for link task '" + task + "!'");
            return;
        }

        if(newEnd != null) Connection.sendToClientsTracking(start, end, new LinkRebindPacket(start, end, newEnd, lrhd));
        else Connection.sendToClientsTracking(start, end, new LinkResponsePacket(start, end,  lrh, transmitter, task));
    }
}
