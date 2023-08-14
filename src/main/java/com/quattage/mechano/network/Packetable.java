package com.quattage.mechano.network;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/***
 * Represents the barebones requirements of a Packet class.
 * Also stores a set of Packets for registry.
 */
public interface Packetable {
    /***
     * Consumes this packet. Modify the world, blocks, etc. here.
     * @param supplier The message supplier
     * @return True if successful.
     */
    public abstract boolean handle(Supplier<NetworkEvent.Context> supplier);

    /***
     * Called when encoding this Packet.
     * @param buf Buffer to encode from
     */
    public abstract void toBytes(FriendlyByteBuf buf);

    
}
