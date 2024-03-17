package com.quattage.mechano.foundation.electricity.power;

import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoPackets;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.power.features.GridClientEdge;
import com.quattage.mechano.foundation.electricity.power.features.GridEdge;
import com.quattage.mechano.foundation.electricity.power.features.GridPath;
import com.quattage.mechano.foundation.network.GridSyncPacketType;
import com.quattage.mechano.foundation.network.GridEdgeUpdateSyncS2CPacket;
import com.quattage.mechano.foundation.network.GridPathUpdateS2CPacket;
import com.quattage.mechano.foundation.network.GridVertDestroySyncS2CPacket;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

// The GridSyncDirector keeps a HashMultimap list of players and what chunks they're looking at.
// When a new chunk enters the player's view, it sends a packet to that player if that chunk contains wires.
@EventBusSubscriber(modid = Mechano.MOD_ID)
public class GridSyncDirector {

    private static final HashMultimap<UUID, ChunkPos> targetedGridChunks = HashMultimap.create();
    
    public GridSyncDirector() {}

    public static void syncGridChunkWithPlayer(Level world, ChunkPos chunkPos, Player player, GridSyncPacketType type) {
        GlobalTransferGrid grid = GlobalTransferGrid.of(world);
        if(!(player instanceof ServerPlayer sPlayer)) return;
        for(LocalTransferGrid sys : grid.getSubgrids()) {
            for(GridEdge edge : sys.allEdges()) {
                if(chunkPos.equals(new ChunkPos(edge.getSideA().getPos()))) 
                    MechanoPackets.sendToClient(new GridEdgeUpdateSyncS2CPacket(type, edge.toLightweight()), sPlayer);
            }
        }
    }

    public static void informPlayerEdgeUpdate(GridSyncPacketType type, GridClientEdge edge) {
        MechanoPackets.sendToAllClients(new GridEdgeUpdateSyncS2CPacket(type, edge));
    }

    public static void informPlayerVertexDestroyed(GridSyncPacketType type, GID edge) {
        MechanoPackets.sendToAllClients(new GridVertDestroySyncS2CPacket(type, edge.getPos()));
    }

    public static void markChunksChanged(ClientLevel world, GridEdge edge) {
        markChunksChanged(world, edge.toLightweight());
    }

    public static void markChunksChanged(ClientLevel world, GridClientEdge edge) {
        BlockPos posA = edge.getSideA().getPos();
        BlockPos posB = edge.getSideB().getPos();

        BlockState stateA = world.getBlockState(posA);
        BlockState stateB = world.getBlockState(posB);
        world.sendBlockUpdated(posA, stateA, stateA, Block.UPDATE_ALL_IMMEDIATE);
        world.sendBlockUpdated(posB, stateB, stateB, Block.UPDATE_ALL_IMMEDIATE);
    }

    public static void markChunksChanged(ClientLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL_IMMEDIATE);
    }

    public static void sendPathDebug(GridPath path, GridSyncPacketType type) {
        Mechano.log("SENDING PATH PACKET");
        MechanoPackets.sendToAllClients(new GridPathUpdateS2CPacket(path, type));
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

    @SubscribeEvent
    public static void onPlayerJoin(EntityJoinLevelEvent event) {
        Level world = event.getLevel();
        Entity entity = event.getEntity();
        if(world.isClientSide()) return;
        if(!(entity instanceof ServerPlayer player)) return;

        GlobalTransferGrid allGrids = GlobalTransferGrid.of(world);
        for(LocalTransferGrid grid : allGrids.getSubgrids()) {
            for(GridPath path : grid.allPaths()) {
                MechanoPackets.sendToClient(new GridPathUpdateS2CPacket(path, GridSyncPacketType.ADD), player);
            }
        }
    }
}
