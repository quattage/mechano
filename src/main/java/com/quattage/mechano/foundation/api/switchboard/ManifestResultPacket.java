package com.quattage.mechano.foundation.api.switchboard;

import java.io.File;
import java.io.PrintWriter;

import com.quattage.mechano.Mechano;
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
        String directory = Minecraft.getInstance().gameDirectory.getAbsolutePath();
        directory += "/logs/mechano_grid_dump.log";
        File output = new File(directory);
        try(PrintWriter pw = new PrintWriter(output)) {
            pw.print(message);
            pw.close();
        } catch(Exception e) {
            Mechano.LOGGER.error("Failed to write mechano grid dump!");
            e.printStackTrace();
        }
    }
}
