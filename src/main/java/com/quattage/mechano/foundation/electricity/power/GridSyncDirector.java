package com.quattage.mechano.foundation.electricity.power;

import java.util.ArrayList;
import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.power.features.GIDPair;
import com.quattage.mechano.foundation.electricity.power.features.GridClientEdge;
import com.quattage.mechano.foundation.electricity.power.features.GridEdge;
import com.quattage.mechano.foundation.network.GridSyncPacketType;
import com.quattage.mechano.foundation.network.GridEdgeUpdateSyncS2CPacket;
import com.quattage.mechano.foundation.network.GridVertDestroySyncS2CPacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = Mechano.MOD_ID)
public class GridSyncDirector {

    private static final HashMultimap<UUID, ChunkPos> targetedGridChunks = HashMultimap.create();
    
    public GridSyncDirector() {}

    public static void syncGridChunkWithPlayer(Level world, ChunkPos pos, Player player, GridSyncPacketType type) {
        GlobalTransferGrid grid = GlobalTransferGrid.get(world);
        ArrayList<GridClientEdge> connections = findEdgesWithinChunk(grid, pos);
        if(!(player instanceof ServerPlayer sPlayer)) return;
        for(GridClientEdge edge : findEdgesWithinChunk(grid, pos)) {
            MechanoPackets.sendToClient(new GridEdgeUpdateSyncS2CPacket(type, edge), sPlayer);
        }
    }

    public static ArrayList<GridClientEdge> findEdgesWithinChunk(GlobalTransferGrid grid, ChunkPos chunkPos) {
        ArrayList<GridClientEdge> out = new ArrayList<>();
        for(LocalTransferGrid sys : grid.getSubsystems()) {
            for(GridEdge edge : sys.getEdgeMatrix().values()) {
                if(chunkPos.equals(new ChunkPos(edge.getSideA().getPos())))
                    out.add(edge.toLightweight());
            }
        }
        return out;
    }

    public static void informPlayerEdgeUpdate(GridSyncPacketType type, GridClientEdge edge) {
        MechanoPackets.sendToAllClients(new GridEdgeUpdateSyncS2CPacket(type, edge));
    }

    public static void informPlayerVertexDestroyed(GridSyncPacketType type, GID edge) {
        MechanoPackets.sendToAllClients(new GridVertDestroySyncS2CPacket(type, edge.getPos()));
    }

    @SubscribeEvent
    public static void onChunkEnterPlayerView(ChunkWatchEvent.Watch event) {
        if(targetedGridChunks.put(event.getPlayer().getUUID(), event.getPos()))
            syncGridChunkWithPlayer(event.getLevel(), event.getPos(), event.getPlayer(), GridSyncPacketType.ADD);
    }

    @SubscribeEvent
    public static void onChunkLeavePlayerView(ChunkWatchEvent.UnWatch event) {
        if(targetedGridChunks.remove(event.getPlayer().getUUID(), event.getPos()))
            syncGridChunkWithPlayer(event.getLevel(), event.getPos(), event.getPlayer(), GridSyncPacketType.REMOVE);
    }
}
