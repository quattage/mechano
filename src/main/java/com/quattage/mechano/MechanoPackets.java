
package com.quattage.mechano;

import java.util.Locale;

import com.quattage.mechano.foundation.api.switchboard.ManifestRequestPacket;
import com.quattage.mechano.foundation.api.switchboard.AnchorPointSyncPacket;
import com.quattage.mechano.foundation.api.switchboard.DispatchSyncPacket;
import com.quattage.mechano.foundation.api.switchboard.LinkRequestPacket;
import com.quattage.mechano.foundation.api.switchboard.LinkResponsePacket;
import com.quattage.mechano.foundation.api.switchboard.ManifestResponsePacket;
import com.quattage.mechano.foundation.api.switchboard.ManifestResultPacket;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;

public enum MechanoPackets implements BasePacketPayload.PacketTypeProvider {
    
    LINK_C2S(LinkRequestPacket.class, LinkRequestPacket.STREAM_CODEC),
    LINK_S2C(LinkResponsePacket.class, LinkResponsePacket.STREAM_CODEC),
    DISPATCH_SYNC_C2S(DispatchSyncPacket.class, DispatchSyncPacket.STREAM_CODEC),
    MANIFEST_S2C(ManifestRequestPacket.class, ManifestRequestPacket.STREAM_CODEC),
    MANIFEST_C2S(ManifestResponsePacket.class, ManifestResponsePacket.STREAM_CODEC),
    MANIFEST_RESULT_S2C(ManifestResultPacket.class, ManifestResultPacket.STREAM_CODEC),
    ANCHOR_SYNC_S2C(AnchorPointSyncPacket.class, AnchorPointSyncPacket.STREAM_CODEC),
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

