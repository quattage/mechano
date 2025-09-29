package com.quattage.mechano.foundation.api.switchboard;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record EntityForceVelocityS2CPacket(int id, int x, int y, int z, boolean override) implements ClientboundPacketPayload {

    private static final float d0 = 3.9f;
    private static final float fac = 8000f;

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityForceVelocityS2CPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public EntityForceVelocityS2CPacket decode(RegistryFriendlyByteBuf buffer) {
            return new EntityForceVelocityS2CPacket(
                buffer.readVarInt(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readShort(),
                buffer.readBoolean()
            );
        }
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, EntityForceVelocityS2CPacket value) {
            buffer.writeVarInt(value.id);
            buffer.writeShort(value.x);
            buffer.writeShort(value.y);
            buffer.writeShort(value.z);
            buffer.writeBoolean(value.override);
        }
    };

    public static EntityForceVelocityS2CPacket of(Entity e, boolean overrideVelocity) {
        Vec3 dM = e.getDeltaMovement();
        double d1 = Mth.clamp(dM.x, -d0, d0);
        double d2 = Mth.clamp(dM.y, -d0, d0);
        double d3 = Mth.clamp(dM.z, -d0, d0);
        return new EntityForceVelocityS2CPacket(e.getId(), (int)(d1 * 8000d), (int)(d2 * 8000d), (int)(d3 * 8000d), overrideVelocity);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.ENTITY_VELOCITY_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {
        if(player == null) return;
        Entity e = player.level().getEntity(id);
        if(e == null) return;
        Vec3 vel = new Vec3(x / 8000d, y / 8000d, z / 8000d);
        VectorHelper.drawDebugRay(player.position(), vel, Color.RED);
        if(override) e.setDeltaMovement(x / 8000d, y / 8000d, z / 8000d);
        else e.push(vel);
    }
    
}
