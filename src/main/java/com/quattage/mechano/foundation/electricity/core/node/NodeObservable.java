package com.quattage.mechano.foundation.electricity.core.node;

import com.quattage.mechano.foundation.network.NodeDataPacket;

import net.minecraft.server.level.ServerPlayer;

public interface NodeObservable {
    void onLookedAt(ServerPlayer player, NodeDataPacket packet);
}
