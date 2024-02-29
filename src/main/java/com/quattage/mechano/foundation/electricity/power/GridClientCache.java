package com.quattage.mechano.foundation.electricity.power;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoRenderTypes;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.power.features.GridClientEdge;
import com.quattage.mechano.foundation.electricity.rendering.WireModelRenderer;
import com.quattage.mechano.foundation.electricity.rendering.WireModelRenderer.BakedModelHashKey;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;
import com.quattage.mechano.foundation.mixin.client.RenderChunkInvoker;
import com.simibubi.create.foundation.utility.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


@OnlyIn(Dist.CLIENT)
/**
 * The GridClientCache is responsible for receiving edges from the GridSyncDirector via packets,
 * and managing a cache of those edges to reliably and quickly associate edges with their chunk positions
 * in-world. The cache is used as a basis for finding edges within targeted chunks and rendering them
 * as a part of that chunk.
 */
public class GridClientCache {

    public static GridClientCache INSTANCE = new GridClientCache();
    private final Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>> queue 
        = new Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>>();;
    private ClientLevel world;

    private int attempts = 0;

    public GridClientCache() {
        Mechano.LOGGER.info("GridClientCache initialized");

        try {
            world = Minecraft.getInstance().level;
        } catch(Exception e) {
            throw new IllegalStateException("Failed to access GridClientCache - An error occured while accessing the world.");
        }
    }

    public void addToQueue(GridClientEdge edge) {
        Mechano.log("adding " + edge + " to the renderQueue");
        synchronized(queue) {
            if(edge.goesNowhere()) return;
            SectionPos sectionPosition = SectionPos.of(edge.getSideA().getPos());
            List<GridClientEdge> section = queue.get(sectionPosition);
            if(section == null) section = new ArrayList<GridClientEdge>();
            section.add(edge);
            queue.put(sectionPosition, section);
        }
    }

    public Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>> getRenderQueue() {
        return queue;
    }

    public void removeFromQueue(GridClientEdge edge) {
        synchronized(queue) {
            SectionPos queryA = SectionPos.of(edge.getSideA().getPos());
            SectionPos queryB = SectionPos.of(edge.getSideB().getPos());


            boolean sided = false;
            List<GridClientEdge> edgeList = queue.get(queryA);
            if(edgeList == null) {
                sided = true;
                edgeList = queue.get(queryB);
            }
            
            Iterator<GridClientEdge> edgeIterator = edgeList.iterator();
            while(edgeIterator.hasNext()) {
                GridClientEdge foundEdge = edgeIterator.next();
                if(foundEdge.equals(edge)) edgeIterator.remove();
            }

            if(edgeList.isEmpty()) {
                if(!sided)
                    queue.remove(queryA);
                else
                    queue.remove(queryB);
            }
        }
    }

    public void clearFromChunkAt(BlockPos pos) {
        queue.remove(SectionPos.of(pos));
    }

    public void clearAllOccurancesOf(BlockPos pos) {     
        synchronized(queue) {       
            SectionPos section = SectionPos.of(pos);
            List<GridClientEdge> edgeList = queue.get(section);
            if(edgeList == null || edgeList.isEmpty()) return;
            Iterator<GridClientEdge> edgeIterator = edgeList.iterator();
            while(edgeIterator.hasNext()) {
                GridClientEdge edge = edgeIterator.next();
                if(edge.containsPos(pos)) edgeIterator.remove();
            }

            if(edgeList.isEmpty())
                queue.remove(section);
        }
    }

    public List<GridClientEdge> getEdgesWithin(SectionPos section) {
        synchronized(queue) {
            List<GridClientEdge> edges = queue.get(section);
            return List.copyOf(edges);
        }
    }

    public void renderConnectionsInChunk(RenderChunk renderChunk, Set<RenderType> renderTypes, ChunkBufferBuilderPack chunkBuffers, BlockPos pos) {
        if(renderChunk == null) return;
        
        List<GridClientEdge> edgeList = queue.get(SectionPos.of(pos));
        if(edgeList == null  || edgeList.isEmpty()) return;

        Function<RenderType, VertexConsumer> builder = getBufferFromChunk(renderChunk, renderTypes, chunkBuffers);
        
        SectionPos sectionCenter = SectionPos.of(pos);
        boolean failed = false;
        for(GridClientEdge edge : queue.get(sectionCenter)) {

            PoseStack matrixStack = new PoseStack();
            matrixStack.pushPose(); 
            BlockPos or = edge.getSideA().getPos().subtract(pos);

            Pair<AnchorPoint, WireAnchorBlockEntity> fromAnchor = AnchorPoint.getAnchorAt(world, edge.getSideA());
            if(fromAnchor == null || fromAnchor.getFirst() == null) {
                failed = true;
                continue;
            }

            Pair<AnchorPoint, WireAnchorBlockEntity> toAnchor = AnchorPoint.getAnchorAt(world, edge.getSideB());
            if(toAnchor == null || toAnchor.getFirst() == null) {
                failed = true;
                continue;
            }

            Vec3 startOffset = fromAnchor.getFirst().getLocalOffset();
            matrixStack.translate(or.getX() + startOffset.x, or.getY() + startOffset.y, or.getZ() + startOffset.z);

            Vec3 startPos = fromAnchor.getFirst().getPos();
            Vec3 endPos = toAnchor.getFirst().getPos();

            int[] lightmap = WireModelRenderer.deriveLightmap(world, startPos, endPos);
            Vector3f wireOrigin = new Vector3f((float)(endPos.x - startPos.x), (float)(endPos.y - startPos.y), (float)(endPos.z - startPos.z));

            float angleY = -(float)Math.atan2(wireOrigin.z(), wireOrigin.x());
            matrixStack.mulPose(new Quaternionf().rotateXYZ(0, angleY, 0));

            VertexConsumer buffer = builder.apply(RenderType.cutoutMipped());
            WireModelRenderer.INSTANCE.renderStatic(new BakedModelHashKey(startPos, endPos), buffer, matrixStack, wireOrigin, lightmap[0], lightmap[1], lightmap[2], lightmap[3], WireSpool.ofType(edge.getTypeID()).getWireSprite());
            matrixStack.popPose();
        }
        attempts++;
        if(failed && attempts < 5) renderConnectionsInChunk(renderChunk, renderTypes, chunkBuffers, pos);
        else attempts = 0;
    }

    public ObjectSet<SectionPos> getAllSections() {
        return queue.keySet();
    }

    public boolean containsPos(SectionPos pos) {
        return queue.containsKey(pos);
    }

    private Function<RenderType, VertexConsumer> getBufferFromChunk(RenderChunk renderChunk, Set<RenderType> renderTypes, ChunkBufferBuilderPack chunkBuffers) {
        return renderType -> {
            BufferBuilder builder = chunkBuffers.builder(renderType);
            if(renderTypes.add(renderType)) ((RenderChunkInvoker)renderChunk).invokeBeginLayer(builder);
            return builder;
        };
    }
}
