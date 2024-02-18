package com.quattage.mechano;

import com.quattage.mechano.foundation.network.Packetable;
import com.quattage.mechano.foundation.network.AnchorSelectC2SPacket;
import com.quattage.mechano.foundation.network.EnergySyncS2CPacket;
import com.quattage.mechano.foundation.network.GridEdgeUpdateSyncS2CPacket;
import com.quattage.mechano.foundation.network.GridVertDestroySyncS2CPacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class MechanoPackets {

    private static SimpleChannel NETWORK;
    private static int packetId = 0;

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
            .named(Mechano.asResource("packets"))
            .networkProtocolVersion(() -> "0.1")
            .clientAcceptedVersions(s -> true)
            .serverAcceptedVersions(s -> true)
            .simpleChannel();
        
        NETWORK = net;

        registerPackets();
    }

    public static void registerPackets() {  

        //S2C
        NETWORK.messageBuilder(EnergySyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(EnergySyncS2CPacket::new)
            .encoder(EnergySyncS2CPacket::toBytes)
            .consumerMainThread(EnergySyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridVertDestroySyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridVertDestroySyncS2CPacket::new)
            .encoder(GridVertDestroySyncS2CPacket::toBytes)
            .consumerMainThread(GridVertDestroySyncS2CPacket::handle)
            .add();

        NETWORK.messageBuilder(GridEdgeUpdateSyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(GridEdgeUpdateSyncS2CPacket::new)
            .encoder(GridEdgeUpdateSyncS2CPacket::toBytes)
            .consumerMainThread(GridEdgeUpdateSyncS2CPacket::handle)
            .add();

        //C2S
        NETWORK.messageBuilder(AnchorSelectC2SPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(AnchorSelectC2SPacket::new)
            .encoder(AnchorSelectC2SPacket::toBytes)
            .consumerMainThread(AnchorSelectC2SPacket::handle)
            .add();
    }

    public static <T extends Packetable> void sendToServer(T message) {
        NETWORK.sendToServer(message);
    }

    public static <T extends Packetable> void sendToClient(T message, ServerPlayer recipient) {
        NETWORK.send(PacketDistributor.PLAYER.with(() -> recipient), message);
    }

    public static <T extends Packetable> void sendToAllClients(T message) {
        NETWORK.send(PacketDistributor.ALL.noArg(), message);
    }

    private static int nextId() {
        return packetId++;
    }
}
