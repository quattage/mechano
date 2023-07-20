package com.quattage.mechano.core.electricity.observe;

import net.minecraft.server.level.ServerPlayer;

public interface IObservable {
    void onLookedAt(ServerPlayer player, NodeDataPacket packet);
}
