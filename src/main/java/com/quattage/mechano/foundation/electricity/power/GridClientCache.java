package com.quattage.mechano.foundation.electricity.power;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.power.features.GridClientEdge;
import com.quattage.mechano.foundation.electricity.power.features.GIDPair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GridClientCache {

    // TODO register as cap and tie it to the world? dimension mismatch issues? 
    public static GridClientCache INSTANCE = new GridClientCache();
    private final Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>> queue 
        = new Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>>();;

    public GridClientCache() {
        Mechano.logReg("ClientCache");
    }

    public void addToQueue(GridClientEdge edge) {
        SectionPos sectionPosition = SectionPos.of(edge.getSideA().getPos());
        List<GridClientEdge> section = queue.get(sectionPosition);
        if(section == null) section = new ArrayList<GridClientEdge>();
        section.add(edge);
        queue.put(SectionPos.of(edge.getSideA().getPos()), section);
        Mechano.log("Added " + edge + " to the renderQueue");
    }

    public Object2ObjectOpenHashMap<SectionPos, List<GridClientEdge>> getRenderQueue() {
        return queue;
    }

    public void removeFromQueue(GridClientEdge edge) {
        SectionPos queryA = SectionPos.of(edge.getSideA().getPos());
        SectionPos queryB = SectionPos.of(edge.getSideB().getPos());

        List<GridClientEdge> edgeList = queue.get(queryA);
        if(edgeList == null) edgeList = queue.get(queryB);
        
        Iterator<GridClientEdge> edgeIterator = edgeList.iterator();
        while(edgeIterator.hasNext()) {
            if(edgeIterator.next().equals(edge))
                edgeIterator.remove();
        }
    }

    public void clearFromChunkAt(BlockPos pos) {
        queue.remove(SectionPos.of(pos));
    }

    public void clearAllOccurancesOf(BlockPos pos) {            
        SectionPos section = SectionPos.of(pos);
        List<GridClientEdge> edgeList = queue.get(section);
        
        if(edgeList == null) return;

        Iterator<GridClientEdge> edgeIterator = edgeList.iterator();
        while(edgeIterator.hasNext()) {
            if(edgeIterator.next().containsPos(pos))
                edgeIterator.remove();
        }

        if(edgeList.isEmpty())
            queue.remove(section);
    }

    public List<GridClientEdge> getEdgesWithin(SectionPos section) {
        synchronized(queue) {
            List<GridClientEdge> edges = queue.get(section);
            return List.copyOf(edges);
        }
    }
}
