package com.quattage.mechano.foundation;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.SimpleBlockEntity.BERefreshable;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record SBESyncPacket(BlockPos pos, byte st) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, SBESyncPacket> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, SBESyncPacket::pos, 
        ByteBufCodecs.BYTE, SBESyncPacket::st,
        SBESyncPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.SBE_SYNC;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {

        ClientLevel world = Minecraft.getInstance().level;
        if(world == null) return;
        BlockState state = world.getBlockState(pos);
        if(!(state.getBlock() instanceof BERefreshable sbeBlock)) return;

        switch(st) {
            case 0:
                // sbeBlock.onBlockPlaced(null, world, pos, state);
                break;
            case 1:
                // sbeBlock.onBlockBroken(null, world, pos, state);
                break;
            case 2:
                // sbeBlock.onBlockStateChanged(null, world, pos, state);
                break;
        }
    }
}
