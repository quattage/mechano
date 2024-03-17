package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.power.GridClientCache;
import com.quattage.mechano.foundation.electricity.power.GridSyncDirector;
import com.quattage.mechano.foundation.electricity.power.features.GridClientEdge;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GridEdgeUpdateSyncS2CPacket implements Packetable {
    
    private final GridSyncPacketType type;
    private final GridClientEdge edge;

    public GridEdgeUpdateSyncS2CPacket(GridSyncPacketType type, GridClientEdge edge) {
        this.type = type;
        this.edge = edge;
    }

    public GridEdgeUpdateSyncS2CPacket(FriendlyByteBuf buf) {
        this.type = GridSyncPacketType.get(buf.readInt());
        this.edge = new GridClientEdge(buf);
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(type.ordinal());
        edge.toBytes(buf);
    }

    
    @Override
    @SuppressWarnings("resource")
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            switch(type) {
                case ADD:
                    GridClientCache.getInstance().addToQueue(edge);
                    break;
                case REMOVE:
                    GridClientCache.getInstance().removeFromQueue(edge);
                    Mechano.log("RECEIVED PACKET AND REMOVED EDGE");
                    break;
                default:
                    break;
            }
        });
        GridSyncDirector.markChunksChanged(Minecraft.getInstance().level, edge);
        return true;
    }
}