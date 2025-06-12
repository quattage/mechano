package com.quattage.mechano.infrastructure.manifest;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.PowerGridBlockEntity;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifier;
import com.quattage.mechano.foundation.api.landmark.client.AnchorPoint;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.StreamCodec;

public record ManifestRequestPacket(NodeIdentifier.Key address) implements ClientboundPacketPayload {

    public static final StreamCodec<ByteBuf, ManifestRequestPacket> STREAM_CODEC = StreamCodec.composite(
        NodeIdentifier.Key.STREAM_CODEC, ManifestRequestPacket::address,
        ManifestRequestPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.MANIFEST_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {
        PowerGridBlockEntity pgbe = address.getHost(player.level());
        if(pgbe == null) {
            send("\n\t┆\t\t" + "▪ Error (PGBE Not found)");
            return;
        }
        String out = (pgbe.surrogate.isSynced() ? "Synced, " : "Unsynced, ") + pgbe.anchors.size() + " anchors: ";
        for(int x = 0; x < pgbe.anchors.size(); x++) {
            AnchorPoint anchor = pgbe.anchors.getByIndex(x);
            if(anchor == null) {
                out += "\n\t┆\t\t\t▪ Error (null anchor)";
                continue;
            }
            out += "\n\t┆\t\t\t▪ Enabled? " + (anchor.isEnabled() + "").toUpperCase() + ", " + anchor.getCurrentConnections() + " / " + anchor.getMaxConnections() + " connections";
        }
        send(out);
    }

    private void send(String msg) {
        CatnipServices.NETWORK.sendToServer(new ManifestResponsePacket(msg));
    }
}
