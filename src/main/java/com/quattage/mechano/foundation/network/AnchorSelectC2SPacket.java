package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class AnchorSelectC2SPacket implements Packetable {
    private final GID anchorLocation;

    public AnchorSelectC2SPacket(AnchorPoint anchor) {
        if(anchor == null)
            this.anchorLocation = null;
        else
            this.anchorLocation = anchor.getID();
    }

    public AnchorSelectC2SPacket(FriendlyByteBuf buf) {

        int index = buf.readInt();

        if(index == -1) {
            this.anchorLocation = null;
            return;
        }

        this.anchorLocation = new GID(buf.readBlockPos(), index);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        if(anchorLocation == null) {
            buf.writeInt(-1);
            buf.writeBlockPos(new BlockPos(0, 0, 0));
            return;
        }

        buf.writeInt(anchorLocation.getSubIndex());
        buf.writeBlockPos(anchorLocation.getPos());
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();            

            if(player.getMainHandItem().getItem() instanceof WireSpool spool) {
                spool.setSelectedAnchor(anchorLocation);
                return; // main hand is prioritized in this case
            }

            if(player.getOffhandItem().getItem() instanceof WireSpool spool) {
                spool.setSelectedAnchor(anchorLocation);
                return;
            }
        });
        return true;
    }
}