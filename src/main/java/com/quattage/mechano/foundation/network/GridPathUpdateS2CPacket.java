package com.quattage.mechano.foundation.network;

import java.util.function.Supplier;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.power.GridClientCache;
import com.quattage.mechano.foundation.electricity.power.GridSyncDirector;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.power.features.GIDPair;
import com.quattage.mechano.foundation.electricity.power.features.GridClientEdge;
import com.quattage.mechano.foundation.electricity.power.features.GridPath;
import com.quattage.mechano.foundation.electricity.power.features.GridVertex;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class GridPathUpdateS2CPacket implements Packetable {
    
    private final int rate;
    private final int pathSize;
    private final GridSyncPacketType type;
    private final GID[] path;
    

    public GridPathUpdateS2CPacket(GridPath gPath, GridSyncPacketType type) {
        
        this.rate = gPath.getRate();
        this.pathSize = gPath.size();
        this.type = type;
        
        this.path = new GID[pathSize];
        int x = 0;
        for(GridVertex vert : gPath.members()) {
            this.path[x] = vert.getGID();
            x++;
        }
    }

    public GridPathUpdateS2CPacket(FriendlyByteBuf buf) {
        this.rate = buf.readInt();
        this.pathSize = buf.readInt();
        this.type = GridSyncPacketType.get(buf.readInt());
        this.path = new GID[pathSize];
        for(int x = 0; x < pathSize; x++)
            this.path[x] = new GID(buf.readBlockPos(), buf.readInt());
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(rate);
        buf.writeInt(pathSize);
        buf.writeInt(type.ordinal());
        for(GID id : path) {
            buf.writeBlockPos(id.getPos());
            buf.writeInt(id.getSubIndex());
        }
    }

    @Override
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            switch(type) {
                case ADD:
                    Mechano.log("CLIENT ADD PATH HANDLE");
                    GridClientCache.getInstance().markValidPath(path);
                    break;
                case REMOVE:
                    Mechano.log("CLIENT REMOVE PATH HANDLE");
                    GridClientCache.getInstance().unmarkPath(path);
                    break;
                default:
                    break;
            }
        });
        return true;
    }
}