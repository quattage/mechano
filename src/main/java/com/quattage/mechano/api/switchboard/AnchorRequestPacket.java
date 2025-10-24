package com.quattage.mechano.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.api.identifier.UUIDDiscriminator;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;


public record AnchorRequestPacket(GridUUID addr,GridResponse task) implements ServerboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorRequestPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, AnchorRequestPacket::addr,
        GridResponse.STREAM_CODEC, AnchorRequestPacket::task,
        AnchorRequestPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.ANCHOR_C2S;
    }

    @Override
    public void handle(ServerPlayer player) {
        Griddable<?> points = addr.getOrFindGriddable(player.level());
        if(points == null || !task.indicatesCompletion()) return;
        switch(task) {
            case TASK_SYNC_ANCHORS -> throw new UnsupportedOperationException("OH NOES! WE FORGOT TO IMPLEMENT THIS!!!! LOL");
            case TASK_FORGET_ANCHORS -> points.getSurrogate().destroy();
            case null, default -> GridResponse.logUnhandled(task, this);
        }
    }
}
