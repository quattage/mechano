

package com.quattage.mechano;

import java.util.Locale;

import com.quattage.mechano.foundation.api.switchboard.AnchorSurrogateDestroyPacket;
import com.quattage.mechano.foundation.api.switchboard.AnchorSyncPacket;
import com.quattage.mechano.foundation.api.switchboard.EntityForceVelocityS2CPacket;
import com.quattage.mechano.foundation.api.switchboard.GriddableUpdatePacket;
import com.quattage.mechano.foundation.api.switchboard.LinkRequestPacket;
import com.quattage.mechano.foundation.api.switchboard.LinkResponsePacket;
import com.quattage.mechano.foundation.api.switchboard.LinkSwapPacket;
import com.quattage.mechano.infrastructure.command.LinkPeekCommand.LinkPeekRequestPacket;
import com.quattage.mechano.infrastructure.manifest.ManifestRequestPacket;
import com.quattage.mechano.infrastructure.manifest.ManifestResponsePacket;
import com.quattage.mechano.infrastructure.manifest.ManifestResultPacket;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;

public enum MechanoPackets implements BasePacketPayload.PacketTypeProvider {
    
    ENTITY_VELOCITY_S2C(EntityForceVelocityS2CPacket.class, EntityForceVelocityS2CPacket.STREAM_CODEC),
    LINK_C2S(LinkRequestPacket.class, LinkRequestPacket.STREAM_CODEC),
    LINK_S2C(LinkResponsePacket.class, LinkResponsePacket.STREAM_CODEC),
    LINK_SWAP_S2C(LinkSwapPacket.class, LinkSwapPacket.STREAM_CODEC),
    LINK_PEEK_S2C(LinkPeekRequestPacket.class, LinkPeekRequestPacket.STREAM_CODEC),
    MANIFEST_S2C(ManifestRequestPacket.class, ManifestRequestPacket.STREAM_CODEC),
    MANIFEST_C2S(ManifestResponsePacket.class, ManifestResponsePacket.STREAM_CODEC),
    MANIFEST_RESULT_S2C(ManifestResultPacket.class, ManifestResultPacket.STREAM_CODEC),
    ANCHOR_SYNC_S2C(AnchorSyncPacket.class, AnchorSyncPacket.STREAM_CODEC),
    ANCHOR_DESTROY_C2S(AnchorSurrogateDestroyPacket.class, AnchorSurrogateDestroyPacket.STREAM_CODEC),
    GRIDDABLE_UPDATE_S2C(GriddableUpdatePacket.class, GriddableUpdatePacket.STREAM_CODEC);
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

