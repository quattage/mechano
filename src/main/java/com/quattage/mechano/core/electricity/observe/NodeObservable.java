package com.quattage.mechano.core.electricity.observe;

import net.minecraft.server.level.ServerPlayer;

public interface NodeObservable {
    void onLookedAt(ServerPlayer player, NodeDataPacket packet);
}
