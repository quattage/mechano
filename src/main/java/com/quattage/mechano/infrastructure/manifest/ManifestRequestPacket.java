package com.quattage.mechano.infrastructure.manifest;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.landmark.DiscriminatorData;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ManifestRequestPacket(GridUUID addr) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ManifestRequestPacket> STREAM_CODEC = StreamCodec.composite(
        DiscriminatorData.STREAM_CODEC, ManifestRequestPacket::addr,
        ManifestRequestPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.MANIFEST_S2C;
    }

    @Override
    public void handle(LocalPlayer player) {
        AnchorPointable host = addr.getHolder(player.level());
        if(host == null) {
            send("\n\t┆\t\t" + "▪ Error (PGBE Not found)");
            return;
        }
        String out = (host.getSurrogate().isSynced() ? "Synced, " : "Unsynced, ") + host.getAnchors().size() + " anchors: ";
        for(int x = 0; x < host.getAnchors().size(); x++) {
            AnchorPoint anchor = host.getAnchor(x);
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
