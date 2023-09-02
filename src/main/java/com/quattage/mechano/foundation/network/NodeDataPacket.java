package com.quattage.mechano.foundation.network;

import com.quattage.mechano.foundation.electricity.NodeBank;

import net.minecraft.network.FriendlyByteBuf;

public class NodeDataPacket {

    private NodeBank<?> nb;

    public NodeDataPacket(NodeBank<?> nb) {
        this.nb = nb;
    }

    public static void encode(NodeDataPacket packet, FriendlyByteBuf tag) {
        return;
    }

    public static NodeDataPacket decode(FriendlyByteBuf buf) {
        return null;
    }
}
