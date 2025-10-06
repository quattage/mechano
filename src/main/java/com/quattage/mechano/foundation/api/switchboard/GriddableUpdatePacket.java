package com.quattage.mechano.foundation.api.switchboard;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.entity.GriddableContraptionAttachment;
import com.quattage.mechano.foundation.api.entity.GriddableEntityAttachment;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record GriddableUpdatePacket(int entityID, @Nullable long[] compositeIDs, boolean contraption) implements ClientboundPacketPayload {

    private static final StreamCodec<RegistryFriendlyByteBuf, @Nullable long[]> LONG_ARRAY = new StreamCodec<>() {
        @Override
        public @Nullable long[] decode(RegistryFriendlyByteBuf buffer) {
            long[] out =  buffer.readLongArray();
            return out.length > 0 ? out : null;
        }
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, @Nullable long[] value) {
            buffer.writeLongArray(value == null ? new long[0] : value);
        }
    };

    public static final StreamCodec<RegistryFriendlyByteBuf, GriddableUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, GriddableUpdatePacket::entityID,
        GriddableUpdatePacket.LONG_ARRAY, GriddableUpdatePacket::compositeIDs,
        ByteBufCodecs.BOOL, GriddableUpdatePacket::contraption,
        GriddableUpdatePacket::new
    );

    public static GriddableUpdatePacket of(GriddableEntityAttachment attachment) {
        Objects.requireNonNull(attachment);
        if(attachment.getSource() == null)
            throw new IllegalArgumentException("Can't send GriddableUpdatePacket for attachment " + attachment + " - This attachment has no source entity!");
        if(attachment instanceof GriddableContraptionAttachment gca) {
            return new GriddableUpdatePacket(attachment.getSource().getId(), gca.packComposite(), true);
        }
        return new GriddableUpdatePacket(attachment.getSource().getId(), null, false);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.GRIDDABLE_UPDATE_S2C;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ClientLevel world = (ClientLevel) player.level();
        Entity e = world.getEntity(entityID);
        if(e == null) {
            Mechano.LOGGER.warn("Couldn't apply Griddable update for entity at " + entityID + " - No entity could be found at this ID!");
            return;
        }
        if(contraption && e instanceof AbstractContraptionEntity ace)  {
            if(compositeIDs == null || compositeIDs.length <= 0) {
                Mechano.LOGGER.warn("Skipped handling Griddable update for " + ace + " - The provided composite is empty!");
                return;
            }
            GriddableContraptionAttachment attachment = new GriddableContraptionAttachment(ace);
            attachment.unpackComposite(compositeIDs);
            e.setData(MechanoData.ANCHOR_ATTACHMENT, attachment);
            return;
        }
        e.setData(MechanoData.ANCHOR_ATTACHMENT, new GriddableEntityAttachment(e));
    }
    
}
