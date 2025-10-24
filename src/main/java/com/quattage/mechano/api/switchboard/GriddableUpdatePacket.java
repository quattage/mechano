package com.quattage.mechano.api.switchboard;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.api.entity.GriddableEntityAttachment;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record GriddableUpdatePacket(int entityID, CompoundTag updateTag) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, @Nullable long[]> LONG_ARRAY = new StreamCodec<>() {
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
        ByteBufCodecs.COMPOUND_TAG, GriddableUpdatePacket::updateTag,
        GriddableUpdatePacket::new
    );

    public static GriddableUpdatePacket of(GriddableEntityAttachment attachment) {
        Objects.requireNonNull(attachment);
        CompoundTag tag = attachment.writeTo(new CompoundTag());
        return new GriddableUpdatePacket(attachment.getSource().getId(), tag);
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
            Mechano.LOGGER.warn("Couldn't apply Griddable update for entity at " + entityID + " - No entity could be found at ID " + entityID + "!");
            return;
        }
        // HolderLookup<AttachmentType<?>> lookup = world.holderLookup(NeoForgeRegistries.ATTACHMENT_TYPES.key());
        GriddableEntityAttachment.SERIALIZER.read(e, updateTag, null);
    }
}
