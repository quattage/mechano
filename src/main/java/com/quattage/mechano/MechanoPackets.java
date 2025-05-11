package com.quattage.mechano;

import java.util.Locale;

import com.quattage.mechano.foundation.api.network.DispatchSyncClientBoundPacket;
import com.quattage.mechano.foundation.api.network.DispatchSyncServerBoundPacket;
import com.quattage.mechano.foundation.api.network.LinkRequestPacket;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;

public enum MechanoPackets implements BasePacketPayload.PacketTypeProvider {
    
    LINK_C2S(LinkRequestPacket.class, LinkRequestPacket.STREAM_CODEC),
    DISPATCH_SYNC_C2S(DispatchSyncClientBoundPacket.class, DispatchSyncClientBoundPacket.STREAM_CODEC),
    DISPATCH_SYNC_S2C(DispatchSyncServerBoundPacket.class, DispatchSyncServerBoundPacket.STREAM_CODEC)
    ;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }

    private final CatnipPacketRegistry.PacketType<?> type;
    <T extends BasePacketPayload> MechanoPackets(Class<T> cl, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		String formatted_name = this.name().toLowerCase(Locale.ROOT);
		this.type = new CatnipPacketRegistry.PacketType<>(
            new CustomPacketPayload.Type<>(Mechano.asResource(formatted_name)),
            cl, codec
		);
	}

    public static void register(IEventBus modBus) {
        CatnipPacketRegistry registrar = new CatnipPacketRegistry(Mechano.ID, 0);
        for(MechanoPackets packet : MechanoPackets.values())
            registrar.registerPacket(packet.type);
        registrar.registerAllPackets();
    }
}

