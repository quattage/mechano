package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import net.createmod.catnip.data.Pair;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.WireSpool;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GIDPair;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridClientEdge;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridEdge;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.rendering.WirePipeline;
import com.quattage.mechano.foundation.electricity.rendering.WireTextureProvider;
import com.quattage.mechano.foundation.electricity.rendering.WirePipeline.BakedModelHashKey;
import com.quattage.mechano.foundation.mixin.client.RenderChunkInvoker;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;


/**
 * The GridClientCache is responsible for receiving edges from the GridSyncDirector via packets,
 * and managing a cache of those edges to reliably and quickly associate edges with their chunk positions
 * in-world. The cache is used as a basis for finding edges within targeted chunks and rendering them
 * as a part of that chunk.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
public class GridClientCache {

    private final Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>> 
        edgeCache = new Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>>();

    private final Object2ObjectOpenHashMap<GIDPair, GridClientEdge> 
    newEdgeCache = new Object2ObjectOpenHashMap<GIDPair, GridClientEdge>();
    
    // if this system ever becomes truly watertight, this will not be necessary
    private final Object2ObjectOpenHashMap<GIDPair, GID[]>
        pathCache  = new Object2ObjectOpenHashMap<>();

    private final ClientLevel world;

    public GridClientCache(ClientLevel world) {
        this.world = world;
    }

    public static GridClientCache of(Level world) {
        if(world == null) throw new NullPointerException("Error getting GlobalTransferGrid - World is null!");
        if(!world.isClientSide()) return null;
        LazyOptional<GridClientCache> cache = world.getCapability(Mechano.CAPABILITIES.CLIENT_CACHE_CAPABILITY);
        if(!cache.isPresent()) throw new RuntimeException("Error getting GlobalTransferGrid from " + world.dimension().location() 
            + " - No handler registered for this dimension!");
        GridClientCache realCache = cache.orElseThrow(RuntimeException::new);
        return realCache;
    }

    @SuppressWarnings("resource")
    public static GridClientCache ofInstance() {
        return GridClientCache.of(Minecraft.getInstance().level);
    }

    public void addToQueue(GridClientEdge edge) {
        addToQueue(edge, false);
    }

    public void addToQueue(GridClientEdge edge, boolean shouldAddNew) {

        if(edge.goesNowhere()) return;

        if(edge.getAge() == 0 || (!shouldAddNew)) {
            synchronized(edgeCache) {
                edge.setAge(0);
                SectionPos sectionPosition = SectionPos.of(edge.getSideA().getBlockPos());
                List<GridClientEdge> section = edgeCache.get(sectionPosition);
                if(section == null) section = new ArrayList<GridClientEdge>();
                section.add(edge);
                edgeCache.put(sectionPosition, section);
            }
            markChunksChanged(world, edge);
            return;
        }

        newEdgeCache.put(edge.toHashable(), edge);
        Mechano.log("NEC: " + newEdgeCache);
    }

    public Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>> getRenderQueue() {
        return edgeCache;
    }

    public void removeFromQueue(GridClientEdge edge) {
        synchronized(edgeCache) {
            SectionPos queryA = SectionPos.of(edge.getSideA().getBlockPos());
            SectionPos queryB = SectionPos.of(edge.getSideB().getBlockPos());

            boolean sided = false;
            List<GridClientEdge> edgeList = edgeCache.get(queryA);
            if(edgeList == null) {
                sided = true;
                edgeList = edgeCache.get(queryB);
            }
            
            Iterator<GridClientEdge> edgeIterator = edgeList.iterator();
            while(edgeIterator.hasNext()) {
                GridClientEdge foundEdge = edgeIterator.next();
                if(foundEdge.equals(edge)) edgeIterator.remove();
            }

            if(edgeList.isEmpty()) {
                if(!sided)
                    edgeCache.remove(queryA);
                else
                    edgeCache.remove(queryB); 
            }
        }
        markChunksChanged(world, edge);
    }

    public void clearFromChunkAt(BlockPos pos) {
        edgeCache.remove(SectionPos.of(pos));
    }

    public void clearAllOccurancesOf(BlockPos pos) {     
        synchronized(edgeCache) {       
            SectionPos section = SectionPos.of(pos);
            List<GridClientEdge> edgeList = edgeCache.get(section);
            if(edgeList == null || edgeList.isEmpty()) return;
            Iterator<GridClientEdge> edgeIterator = edgeList.iterator();
            while(edgeIterator.hasNext()) {
                GridClientEdge edge = edgeIterator.next();
                if(edge.containsPos(pos)) edgeIterator.remove();
            }

            if(edgeList.isEmpty())
                edgeCache.remove(section);
        }
    }

    public void renderConnectionsInChunk(RenderChunk renderChunk, Set<RenderType> renderTypes, ChunkBufferBuilderPack chunkBuffers, BlockPos pos) {
        synchronized(edgeCache) {
            if(renderChunk == null) return;
            
            List<GridClientEdge> edgeList = edgeCache.get(SectionPos.of(pos));
            if(edgeList == null  || edgeList.isEmpty()) return;

            VertexConsumer builder = getBufferFromChunk(renderChunk, renderTypes, chunkBuffers).apply(RenderType.cutoutMipped());
            SectionPos sectionCenter = SectionPos.of(pos);
        
            for(GridClientEdge edge : edgeCache.get(sectionCenter)) {

                // sanity checks
                if(edge.getAge() > 0) continue;
                Pair<AnchorPoint, WireAnchorBlockEntity> fromAnchor =
                    AnchorPoint.getAnchorAt(world, edge.getSideA());
                if(fromAnchor == null || fromAnchor.getFirst() == null) 
                    continue;

                Pair<AnchorPoint, WireAnchorBlockEntity> toAnchor = 
                    AnchorPoint.getAnchorAt(world, edge.getSideB());
                if(toAnchor == null || toAnchor.getFirst() == null) 
                    continue;
                //////////

                PoseStack matrixStack = new PoseStack();
                matrixStack.pushPose(); 

                BlockPos posDiff = edge.getSideA().getBlockPos().subtract(pos);

                Vec3 startOffset = fromAnchor.getFirst().getLocalOffset();
                matrixStack.translate(posDiff.getX() + startOffset.x, posDiff.getY() + startOffset.y, posDiff.getZ() + startOffset.z);

                Vec3 startPos = fromAnchor.getFirst().getPos();
                Vec3 endPos = toAnchor.getFirst().getPos();
                Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

                float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
                matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

                int[] lightmap = WirePipeline.deriveLightmap(world, startPos, endPos);
                WirePipeline.INSTANCE.renderStatic(
                    new BakedModelHashKey(startPos, endPos), 
                    builder, matrixStack, wireOrigin, 
                    lightmap[0], lightmap[1], lightmap[2], lightmap[3], 
                    WireTextureProvider.getWireSprite(WireSpool.ofType(edge.getTypeID()))
                );

                matrixStack.popPose();
                
            }

            // if(failed) Mechano.LOGGER.warn("Non-Fatal error rendering edge at " + sectionCenter + " - most likely accessed after removal.");
        }
    }

    public ObjectSet<SectionPos> getAllSections() {
        return edgeCache.keySet();
    }

    public boolean containsPos(SectionPos pos) {
        return edgeCache.containsKey(pos);
    }

    public Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>> getEdgeCache() {
        return edgeCache;
    }

    public static void markChunksChanged(ClientLevel world, GridEdge edge) {
        markChunksChanged(world, edge.toLightweight());
    }

    public static void markChunksChanged(ClientLevel world, GridClientEdge edge) {
        BlockPos posA = edge.getSideA().getBlockPos();
        BlockPos posB = edge.getSideB().getBlockPos();

        BlockState stateA = world.getBlockState(posA);
        BlockState stateB = world.getBlockState(posB);
        world.sendBlockUpdated(posA, stateA, stateA, 3);
        world.sendBlockUpdated(posB, stateB, stateB, 3);
    }

    public void markValidPath(GID[] path) {
        pathCache.put(new GIDPair(path[0], path[path.length - 1]), path);
    }

    public void unmarkPath(GID[] path) {
        pathCache.remove(new GIDPair(path[0], path[path.length - 1]));
    }

    public Object2ObjectOpenHashMap<GIDPair, GID[]> getAllPaths() {
        return this.pathCache;
    }

    public static boolean hasNewEdge(BlockEntity be) {
        if(!(be instanceof WireAnchorBlockEntity wbe)) return false;
        GridClientCache cache = GridClientCache.of(wbe.getLevel());
        return cache.newEdgeCache.containsKey(wbe.getBlockPos());
    }

    public Collection<GridClientEdge> getAllNewEdges() {
        return newEdgeCache.values();
    }

    public ClientLevel getWorld() {
        return this.world;
    }

    private static long lastTime = System.nanoTime();

    @SubscribeEvent
    @SuppressWarnings("resource")
	public static void onViewTick(ViewportEvent event) {
		
        ClientLevel world = Minecraft.getInstance().level;
        if(world == null) return;
        GridClientCache cache = GridClientCache.of(world);
        if(cache == null) return;

        long time = System.nanoTime();
        Iterator<GridClientEdge> newEdges = cache.newEdgeCache.values().iterator();
        while(newEdges.hasNext()) {
            GridClientEdge edge = newEdges.next();
            if(edge.getAge() > 0) {
                if(!edge.existsIn(world)) {
                    newEdges.remove();
                    continue;
                }
                edge.tickAge(time - lastTime);
                continue;
            } 

            newEdges.remove();
            cache.addToQueue(edge);
            markChunksChanged(world, edge);
        }

        lastTime = time;
    }

    public static List<GridClientEdge> getEdgesIn(Level world, SectionPos at) {
        return GridClientCache.of(world).getEdgeCache().get(at);
    }

    @SubscribeEvent
    @SuppressWarnings("resource")
    public static void onLeave(ClientPlayerNetworkEvent.LoggingOut event) {
        
        LocalPlayer player = event.getPlayer();
        if(player == null) return;

        Level world = player.level();
        if(world == null) return;

        GridClientCache.of(world).clearAll();
        Mechano.LOGGER.info("Clearing all cached wire geometry");
    }

    public void clearAll() {
        edgeCache.clear();
        newEdgeCache.clear();
        pathCache.clear();
    }

    private Function<RenderType, VertexConsumer> getBufferFromChunk(RenderChunk renderChunk, Set<RenderType> renderTypes, ChunkBufferBuilderPack chunkBuffers) {
        return renderType -> {
            BufferBuilder builder = chunkBuffers.builder(renderType);
            if(renderTypes.add(renderType)) ((RenderChunkInvoker)renderChunk).invokeBeginLayer(builder);
            return builder;
        };
    }
}