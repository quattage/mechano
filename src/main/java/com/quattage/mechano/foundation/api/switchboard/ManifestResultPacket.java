package com.quattage.mechano.foundation.api.switchboard;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.quattage.mechano.MechanoPackets;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ManifestResultPacket(String message) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, ManifestResultPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ManifestResultPacket::message,
        ManifestResultPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.MANIFEST_RESULT_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {
        Path directory = Minecraft.getInstance().gameDirectory.toPath().resolve("logs");
        if(!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        directory = directory.resolve("mechano_grid_dump.log");
        if(!Files.exists(directory)) {
            try {
                Files.createFile(directory);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.writeString(directory, message, StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
