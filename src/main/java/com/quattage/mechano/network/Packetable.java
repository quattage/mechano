package com.quattage.mechano.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public interface Packetable {
    public boolean handle(Supplier<NetworkEvent.Context> supplier);
    public void toBytes(FriendlyByteBuf buf);
}
