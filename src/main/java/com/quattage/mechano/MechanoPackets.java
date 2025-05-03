package com.quattage.mechano;

import java.util.Locale;

import com.quattage.mechano.foundation.SBESyncPacket;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.neoforged.bus.api.IEventBus;

public enum MechanoPackets implements BasePacketPayload.PacketTypeProvider {
    
    SBE_SYNC(SBESyncPacket.class, SBESyncPacket.CODEC)
    ;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> Type<T> getType() {
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

