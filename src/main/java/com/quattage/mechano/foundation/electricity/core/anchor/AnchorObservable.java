package com.quattage.mechano.foundation.electricity.core.anchor;

import com.quattage.mechano.foundation.network.NodeDataPacket;

import net.minecraft.server.level.ServerPlayer;

public interface AnchorObservable {
    void onLookedAt(ServerPlayer player, NodeDataPacket packet);
}
