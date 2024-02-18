package com.quattage.mechano.foundation.network;

public enum GridSyncPacketType {
    ADD,
    REMOVE,
    SYNC;

    public static GridSyncPacketType get(int x) {
        return GridSyncPacketType.values()[x];
    }
}
