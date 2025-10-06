package com.quattage.mechano.infrastructure.manifest;

import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ManifestRequestPacket(GridUUID addr) implements ClientboundPacketPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, ManifestRequestPacket> STREAM_CODEC = StreamCodec.composite(
        UUIDDiscriminator.STREAM_CODEC, ManifestRequestPacket::addr,
        ManifestRequestPacket::new
    );

    @Override
    public PacketTypeProvider getTypeProvider() {
        return MechanoPackets.MANIFEST_S2C;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        Griddable<?> points = addr.getOrFindGriddable(player.level());
        if(points == null) {
            CatnipServices.NETWORK.sendToServer(new ManifestResponsePacket("\n\t┆\t\t" + "▪ Error (Host Not found)"));
            return;
        }
        String out = (points.getSurrogate().isSynced(player.level()) ? "Synced, " : "Unsynced, ") + points.getAnchors().size() + " anchors: ";
        for(int x = 0; x < points.getAnchors().size(); x++) {
            AnchorPoint anchor = points.getAnchor(x);
            if(anchor == null) {
                out += "\n\t┆\t\t\t▪ Error (null anchor)";
                continue;
            }
            out += "\n\t┆\t\t\t▪ Enabled? " + (anchor.isEnabled() + "").toUpperCase() + ", " + anchor.getCurrentConnections() + " / " + anchor.getMaxConnections() + " connections";
        }
        CatnipServices.NETWORK.sendToServer(new ManifestResponsePacket(out));
    }
}
