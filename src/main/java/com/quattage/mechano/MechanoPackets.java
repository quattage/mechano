

package com.quattage.mechano;

import java.util.Locale;

import com.quattage.mechano.api.switchboard.GridActionC2SPacket;
import com.quattage.mechano.api.switchboard.GridActionS2CPacket;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.IEventBus;

public enum MechanoPackets implements BasePacketPayload.PacketTypeProvider, StringRepresentable {
    
    GRID_ACTION_S2C(GridActionS2CPacket.class, GridActionS2CPacket.STREAM_CODEC),
    GRID_ACTION_C2S(GridActionC2SPacket.class, GridActionC2SPacket.STREAM_CODEC),
    ;

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>)this.type.type();
    }


    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> MechanoPackets(Class<T> cl, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		this.type = new CatnipPacketRegistry.PacketType<>(
            new CustomPacketPayload.Type<>(Mechano.asResource(this.toString())),
            cl, codec
		);
	}

    public static void register(IEventBus modBus) {
        CatnipPacketRegistry registrar = new CatnipPacketRegistry(Mechano.ID, MechanoBuildParameters.VERSION);
        for(MechanoPackets packet : MechanoPackets.values())
            registrar.registerPacket(packet.type);
        registrar.registerAllPackets();
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }
}

