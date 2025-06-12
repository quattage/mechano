
package com.quattage.mechano.foundation.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifiable;
import com.quattage.mechano.foundation.api.landmark.base.NodeIdentifier;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseHolder;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.data.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;

/**
 * A GlobalGrid is the top-level manager for {@link PowerGrid PowerGrids}.
 * Data Attachments are leveraged to add one GlobalGrid to each level/dimension.
 */
public final class GlobalServerGrid extends SidedGridDispatcher {

    public ObjectArrayList<PowerGrid> subgrids;
    public Object2ObjectOpenHashMap<ChunkPos, List<GridLink>> linksByChunk = new Object2ObjectOpenHashMap<>();

    /**
     * Loads a GlobalServerGrid from a serialized list of {@link PowerGrid PowerGrids}. 
     * All PowerGrids, their {@link GridNode GridNodes}, and their {@link GridLink GridLinks}
     * are loaded from here. This method is called by the {@link com.quattage.mechano.foundation.api.SidedGridDisptacher.Serializer serializer}
     * @param subgrids A two-dimensional {@link ListTag} - A list of subgrids, where each subgrid is a list of {@link CompoundTag CompoundTags}
     * @param world World that the resulting GlobalServerGrid uses to look up {@link PowerGridBlockEntity PGBEs}
     * @return a new GlobalServerGrid with data primed from the provided list of grids.
     */
    public static GlobalServerGrid loadFrom(ListTag subgrids, ServerLevel world) {

        GlobalServerGrid freshGlobal = new GlobalServerGrid(world,  new ObjectArrayList<>(subgrids.size()));
        for(int x = 0; x < subgrids.size(); x++) {
            ListTag writtens = subgrids.getList(x);
            if(writtens.isEmpty()) continue;
            PowerGrid freshLocal = new PowerGrid(freshGlobal, writtens.size());
            for(int y = 0; y < writtens.size(); y++) {
                CompoundTag node = writtens.getCompound(y);
                createNodeAndMakeProvisionalLinks(freshGlobal, freshLocal, NodeIdentifier.Key.loadFrom(node), node.getList("links", Tag.TAG_COMPOUND));
            }

            // if the powergrid failed to deserialize make sure it gets removed
            if(freshLocal.nodes.isEmpty()) {
                if(freshLocal.gridIndex < 0 || freshLocal.gridIndex >= subgrids.size() || freshGlobal.subgrids.remove(freshLocal.gridIndex) == null)
                    freshGlobal.subgrids.remove(freshLocal);
                freshLocal.nullify();
            }
        }
        return freshGlobal;
    }

    /**
     * A breakout method for making {@link GlobalServerGrid#loadFrom} easier to read - 
     * This method is responsible for de-serializing the GridNode and its links <p>
     * getOrCreate is used here to ensure that only one canonical reference to each GridNode exists after
     * the GlobalServerGrid is loaded. GridNodes may have already been created as links by previous calls
     * to this method.
     * 
     * TODO A recursive approach may reduce iteration count slightly if links are created depth-first rather than re-addressing provisional links
     */
    private static void createNodeAndMakeProvisionalLinks(GlobalServerGrid grid, PowerGrid instantiator, NodeIdentifier.Key address, @Nullable ListTag links) {

        if(links == null || links.isEmpty()) return;
        GridNode newStart = instantiator.getOrCreateProvisional(address);
        if(newStart == null) return;

        newStart.links.ensureCapacity(links.size());

        for(int x = 0; x < links.size(); x++) {

            CompoundTag serializedLink = links.getCompound(x);
            NodeIdentifier.Key endAddress = NodeIdentifier.Key.loadFrom(serializedLink);
            GridNode newEnd = instantiator.getOrCreateProvisional(endAddress);
            if(newEnd == null || newStart.equals(newEnd)) continue;

            Transmitter<?> trns = TransmitterRegistry.INSTANCE.get(serializedLink);
            GridLink newLink = new GridLink(newStart, newEnd, trns);

            if(newLink.getConnection().needsSerialization()) {
                CompoundTag data = serializedLink.getCompound("data");
                if(!data.isEmpty()) newLink.getConnection().loadFrom(data);
            }

            newStart.links.add(newLink);
            grid.trackLink(newLink);
        }

        newStart.links.trim();
    }

    /**
     * Creates a new GlobalServerGrid with a predefined list of subgrids.
     * Used internally by the {@link SidedGridDispatcher#SERIALIZER}
     * @param world ServerLevel that owns this grid
     * @param subgrids Subgrids to instantiate the new GlobalServerGrid with
     */
    protected GlobalServerGrid(ServerLevel world, ObjectArrayList<PowerGrid> subgrids) {
        super(world);
        this.subgrids = subgrids;
    }




    /**
     * Create a link between any two {@link GridNode GridNodes} as long as their host
     * {@link PowerGridBlockEntity} instances exist in this GlobalServerGrid's level.
     * This method is a no-questions-asked wrapper for {@link GlobalServerGrid#linkUnsafe linkUnsafe}
     * that performs operations to maintain the integrity of this GlobalServerGrid regardless of its 
     * internal state when linking. It does this by creating new {@link GridNode GridNodes} and/or 
     * {@link PowerGrid PowerGrids} where necessary. 
     * <p>
     * {@link GlobalServerGrid#linkUnsafe linkUnsafe} instead.
     * @param start Starting address 
     * @param end Ending address
     * @param type The type of link that will be created between <code>start</code> and <code>end</code>
     * @return {@link LinkResponseHolder} holding the link that was created as well as a response describing whether or not
     * the link was successful
     */
    public LinkResponseHolder createLink(NodeIdentifiable start, NodeIdentifiable end, TransmitterType<?> type) {

        // prep and sanity checks
        LevelReader world = getLevelReader();
        PowerGridBlockEntity startBE = start.getHost(world);
        PowerGridBlockEntity endBE = end.getHost(world);
        if(startBE == null) {
            Mechano.LOGGER.error("Failed to create link from " + start + " to " + end + " - No valid PGBE could be found at the starting address!");
            GridNode startNode = lookup(start).getSecond();
            GridNode endNode = lookup(end).getSecond();
            return LinkResponseHolder.of(startNode, endNode, Response.Link.FAIL_SYNC_OUTDATED);
        } else if(endBE == null) {
            GridNode startNode = lookup(start).getSecond();
            GridNode endNode = lookup(end).getSecond();
            Mechano.LOGGER.error("Failed to create link from " + start + " to " + end + " - No valid PGBE could be found at the ending address!");
            return LinkResponseHolder.of(startNode, endNode, Response.Link.FAIL_SYNC_OUTDATED);
        }

        Transmitter<?> trns = type.make();

        // nodes both belong to grids
        if(startBE.surrogate.isSynced() && endBE.surrogate.isSynced()) {

            PowerGrid startPG = startBE.surrogate.owner;
            PowerGrid endPG = endBE.surrogate.owner;

            // nodes both belong to the same grid
            if(startPG.gridIndex == endPG.gridIndex) {
                endPG = null;
                GridNode startNode = startPG.nodes.get(start);
                GridNode endNode = startPG.nodes.get(end);
                GridLink newLink = new GridLink(startNode, endNode, trns);
                startBE.surrogate.owner = startPG;
                endBE.surrogate.owner = startPG;
                if(startNode.links.contains(newLink)) return LinkResponseHolder.of(newLink, Response.Link.FAIL_DUPLICATE);
                return LinkResponseHolder.of(linkUnsafe(newLink, startBE, endBE), Response.SUCCESS);
            }

            // nodes both belong to different grids
            PowerGrid merged = mergeGrids(startPG.gridIndex, endPG.gridIndex);
            GridNode startNode = merged.nodes.get(start);
            GridNode endNode = merged.nodes.get(end);
            startBE.surrogate.owner = merged;
            endBE.surrogate.owner = merged;
            return LinkResponseHolder.of(linkUnsafe(startNode, startBE, endNode, endBE, trns), Response.SUCCESS);
        }

        // start belongs to grid, but end doesn't
        if(startBE.surrogate.isSynced() && !endBE.surrogate.isSynced()) {
            GridNode startNode = startBE.surrogate.owner.nodes.get(start);
            GridNode endNode = new GridNode(startBE.surrogate.owner, endBE, end.getIndex());
            startBE.surrogate.owner.nodes.add(endNode);
            endBE.surrogate.owner = startBE.surrogate.owner;
            endBE.onAddedToGrid(getWorld(), startBE.surrogate.owner);
            return LinkResponseHolder.of(linkUnsafe(startNode, startBE, endNode, endBE, trns), Response.SUCCESS);
        }

        // end belongs to grid, but start doesn't
        if(!startBE.surrogate.isSynced() && endBE.surrogate.isSynced()) {
            GridNode startNode = new GridNode(endBE.surrogate.owner, startBE, start.getIndex()); 
            GridNode endNode = endBE.surrogate.owner.nodes.get(end);
            endBE.surrogate.owner.nodes.add(startNode);
            startBE.surrogate.owner = endBE.surrogate.owner;
            startBE.onAddedToGrid(getWorld(), endBE.surrogate.owner);
            return LinkResponseHolder.of(linkUnsafe(startNode, startBE, endNode, endBE, trns), Response.SUCCESS);
        }

        // neither belongs to grid
        if(!startBE.surrogate.isSynced() && !endBE.surrogate.isSynced()) {
            PowerGrid newGrid = new PowerGrid(this, 2);
            GridNode startNode = new GridNode(newGrid, startBE, start.getIndex());
            GridNode endNode = new GridNode(newGrid, endBE, end.getIndex());
            newGrid.nodes.add(startNode);
            newGrid.nodes.add(endNode);
            startBE.surrogate.owner = newGrid;
            endBE.surrogate.owner = newGrid;
            startBE.onAddedToGrid(getWorld(), newGrid);
            endBE.onAddedToGrid(getWorld(), newGrid);
            return LinkResponseHolder.of(linkUnsafe(startNode, startBE, endNode, endBE, trns), Response.SUCCESS);
        }

        return LinkResponseHolder.of(null, Response.FAIL_GENERIC);
    }









    /**
     * Takes the contents from one {@link PowerGrid} in this GlobalServerGrid and 
     * merges it into the other. 
     * <p>
     * Nodes are merged by order of index, so a the contents of a grid with a HIGHER
     * index are merged onto a grid with a LOWER index. This also means that no new 
     * PowerGrid instances are created - the PowerGrid located at the lower of the 
     * two indices is recycled. 
     * <p>
     * Additionally, if the merge is successful, this call will immediately mark 
     * the the grid belonging to the higher index for removal. If this PowerGrid 
     * instance is stored anywhere, its reference should be nullified to prevent 
     * a leak. Only use the PowerGrid returned from this method.
     * 
     * @param index1 The index of the first PowerGrid to merge
     * @param index2 The index of the second PowerGrid to merge
     * @return A reference to the resulting PowerGrid. This reference will be 
     * identical to the PowerGrid found at the lower of the two provided indices, 
     * but it will contain the contents of both grids.
     * @throws ArrayIndexOutOfBoundsException if the indices provided are outside the bounds of the subgrids list.
     */
    public PowerGrid mergeGrids(int index1, int index2) {

        if(index1 < 0 || index1 >= subgrids.size())
            throw new ArrayIndexOutOfBoundsException("Failed to merge grids - Index1 '" + index1 + "' is out of bounds for a GlobalServerGrid with " + subgrids.size() + " subgrids!");
        if(index2 < 0 || index2 >= subgrids.size())
            throw new ArrayIndexOutOfBoundsException("Failed to merge grids - Index1 '" + index2 + "' is out of bounds for a GlobalServerGrid with " + subgrids.size() + " subgrids!");

        if(index1 == index2)
            return subgrids.get(index1);

        // merge 2 into 1
        if(index1 < index2) {
            PowerGrid grid1 = subgrids.get(index1);
            grid1.gridIndex = index1;
            PowerGrid grid2 = subgrids.remove(index2);
            grid1.addAll(grid2.nodes);
            grid2.nullify();
            updateGridIndices(index1);
            return grid1;
        }

        // merge 1 into 2
        if(index2 < index1) {
            PowerGrid grid2 = subgrids.get(index2);
            grid2.gridIndex = index2;
            PowerGrid grid1 = subgrids.remove(index1);
            grid2.addAll(grid1.nodes);
            grid1.nullify();
            updateGridIndices(index2);
            return grid2;
        }

        throw new UnsupportedOperationException("...what? the fuck?");
    }





/**
     * Creates a symmetrical link between <code>start</code> and <code>end</code>. 
     * Does not perform any checks to ensure that this link is valid. When in doubt, use
     * {@link GlobalServerGrid#createLink createLink} instead.
     * @param start GridNode start
     * @param end GridNode end
     * @param trns The transmitter that the link will contain
     * @return The GridLink that was created
     */
    public GridLink linkUnsafe(GridNode start, @Nullable PowerGridBlockEntity startBE, GridNode end, @Nullable PowerGridBlockEntity endBE, Transmitter<?> trns) {
        GridLink link = new GridLink(start, end, trns);
        return linkUnsafe(link, startBE, endBE);
    }




    /**
     * Creates a symmetrical link between <code>start</code> and <code>end</code>. 
     * Does not perform any checks to ensure that this link is valid. When in doubt, use
     * {@link GlobalServerGrid#createLink createLink} instead.
     * @param link GridLink to add
     * @param trns The transmitter that the link will contain
     * @return The GridLink provided
     */
    public GridLink linkUnsafe(GridLink link, @Nullable PowerGridBlockEntity startBE, @Nullable PowerGridBlockEntity endBE) {
        link.getStart().links.add(link);
        trackLink(link);
        GridLink inverse = link.copyAndFlip();
        inverse.getStart().links.add(inverse);
        trackLink(inverse);
        link.getConnection().onConnectionCreated(getWorld(), link);
        if(startBE != null) startBE.onConnectionMade(world, link);
        if(endBE != null) endBE.onConnectionMade(world, inverse);
        return link;
    }


    /**
     * Adds the provied GridLink to this GlobalServerGrid's 
     * tracked links set.
     * @param link Link to add
     */
    public void trackLink(GridLink link) {
        ChunkPos startChunkPos = new ChunkPos(link.getStart().getPos());
        List<GridLink> links = linksByChunk.get(startChunkPos);
        if(links == null) {
            links = new ArrayList<>();
            links.add(link);
            linksByChunk.put(startChunkPos, links);
            return;
        }
        int index = links.indexOf(link);
        if(index == -1)
            links.add(link);
        else links.set(index, link);
    }

    public void untrackLink(GridLink link) {
        ChunkPos startChunkPos = new ChunkPos(link.getStart().getPos());
        List<GridLink> links = linksByChunk.get(startChunkPos);
        if(links == null) return;
        links.remove(link);
        if(links.isEmpty()) linksByChunk.remove(startChunkPos);
    }


    /**
     * Finds a node at the given address by iterating through all subgrids.
     * @param address {@link com.quattage.mechano.foundation.api.landmark.base.NodeIdentifier NodeIdentifier} to look for
     * @return A pair containing the {@link GridNode} and its {@link PowerGrid parent}. If a node is not found
     * at the given address, the contents of the pair will be null.
     */
    public Pair<PowerGrid, GridNode> lookup(NodeIdentifiable address) {
        for(int x = 0; x < subgrids.size(); x++) {
            PowerGrid grid = subgrids.get(x);
            GridNode get = grid.nodes.get(address);
            if(get != null) return Pair.of(grid, get);
        }
        return Pair.of(null, null);
    }




    /**
     * Iterates over all {@link PowerGrid} instances
     * and clears them. Broadcasts updates as a result.
     */
    public void clear() {
        Iterator<PowerGrid> it = subgrids.iterator();
        while(it.hasNext()) {
            PowerGrid grid = it.next();
            grid.clear();
            it.remove();
        }
        subgrids.trim(1);
    }




    /**
     * Iteratively updates all PowerGrid indices to match where they are
     * in this GlobalServerGrid's subgrid array. This is necessary
     * any time the list gets smaller.
     * @param startingIndex Index to start from. Normally, this would be the index that was removed.
     */
    public void updateGridIndices(int startingIndex) {
        for(int x = startingIndex; x < subgrids.size(); x++)
            subgrids.get(x).gridIndex = x;
    }




    /**
     * Destroys the grid at the given index, or does nothing
     * if the index doesn't exist in this GlobalServerGrid.
     * @param index
     * @return <code>true</code> if this GlobalServerGrid was modified as a result of this call
     */
    public boolean destroyGridAt(int index) {
        if(index < 0 || index >= subgrids.size())
            return false;
        PowerGrid grid = subgrids.remove(index);
        if(grid == null) return false;
        grid.nullify();

        if(subgrids.isEmpty()) {
            subgrids.trim();
            return true;
        }

        updateGridIndices(index);
        return true;
    }


    /**
     * Destroys the grid at the given index, or does nothing
     * if the index doesn't exist in this GlobalServerGrid.
     * @param grid PowerGrid to destroy
     * @return <code>true</code> if this GlobalServerGrid was modified as a result of this call
     */
    public boolean destroyGrid(PowerGrid grid) {
        int index = grid.gridIndex;
        if(grid.gridIndex < 0 || grid.gridIndex >= subgrids.size())
            index = subgrids.indexOf(grid);
        if(index < 0) {
            grid.nullify();
            return false;
        }
        subgrids.remove(index);
        grid.nullify();

        if(subgrids.isEmpty()) {
            subgrids.trim();
            return true;
        }

        updateGridIndices(index);
        return true;
    }

    /**
     * Adds every member of the supplied list to this GlobalServerGrid's 
     * subgrids list and updates their indices.
     * @param grids Grids to add
     */
    public void addAll(List<PowerGrid> grids) {
        if(grids.isEmpty()) return;
        subgrids.ensureCapacity(subgrids.size() + grids.size());
        for(int x = 0; x < grids.size(); x++) {
            PowerGrid grid = grids.get(x);
            if(grid.nodes.isEmpty()) {
                grid.nullify();
                continue;
            }
            subgrids.add(grid);
            grid.gridIndex = subgrids.size() - 1;
        }
        subgrids.trim();
    }


    /**
     * @return The ServerLevel that this GlobalServerGrid is attached to.
     */
    @Override
    public ServerLevel getWorld() {
        return (ServerLevel)super.getWorld();
    }




    /**
     * Writes this entire GlobalServerGrid to a new ListTag
     * @returns A new ListTag, made of {@link CompoundTag CompoundTags}
     * acquired by the PowerGrid's {@link com.quattage.mechano.foundation.api.landmark.NodeSet#write writing process}
     */
    @Override
    protected @Nullable ListTag writeAll() {
        ListTag output = new ListTag();
        for(PowerGrid grid : subgrids) {
            if(grid == null || grid.nodes.isEmpty() || grid.global == null) 
                continue;
            output.add(grid.nodes.write());
        }
        return output;
    }



    @Override
    protected String getDistPrefix() {
        return "SERVER";
    }
}
