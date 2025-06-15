package com.quattage.mechano.foundation.catenary;

import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.meshing.GeoHolder;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.server.level.ServerPlayer;

public class CatenaryManager {

    private final GeoHolder mesher = GeoHolder.asEmpty();

    public void mark(ServerPlayer player, TransmitterType<?> type) {

    }
}
