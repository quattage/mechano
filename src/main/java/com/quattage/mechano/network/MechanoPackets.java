package com.quattage.mechano.network;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.electricity.network.EnergySyncS2CPacket;

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
        buildPackets();
    }

    public static void buildPackets() {
        NETWORK.messageBuilder(EnergySyncS2CPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(EnergySyncS2CPacket::new)
            .encoder(EnergySyncS2CPacket::toBytes)
            .consumerMainThread(EnergySyncS2CPacket::handle)
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
