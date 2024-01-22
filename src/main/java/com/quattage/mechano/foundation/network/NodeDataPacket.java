package com.quattage.mechano.foundation.network;

import com.quattage.mechano.foundation.electricity.AnchorPointBank;

import net.minecraft.network.FriendlyByteBuf;

public class NodeDataPacket {

    private AnchorPointBank<?> nb;

    public NodeDataPacket(AnchorPointBank<?> nb) {
        this.nb = nb;
    }

    public static void encode(NodeDataPacket packet, FriendlyByteBuf tag) {
        return;
    }

    public static NodeDataPacket decode(FriendlyByteBuf buf) {
        return null;
    }
}
