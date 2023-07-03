package com.quattage.mechano.core.blockEntity.observe;

import net.minecraft.server.level.ServerPlayer;

public interface IObservable {
    void onLookedAt(ServerPlayer player, NodeDataPacket packet);
}
