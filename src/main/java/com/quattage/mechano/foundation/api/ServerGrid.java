
package com.quattage.mechano.foundation.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.DiscriminatorData;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseHolder;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.createmod.catnip.data.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * A GlobalGrid is the top-level manager for {@link ServerMatrix ServerMatrices}.
 * Data Attachments are leveraged to add one GlobalGrid to each level/dimension.
 */
public final class ServerGrid extends SidedGridDispatcher {

    public ObjectArrayList<ServerMatrix> matrices;
    public Object2ObjectOpenHashMap<ChunkPos, ObjectOpenHashSet<GridLink>> linksByChunk = new Object2ObjectOpenHashMap<>();

    /**
     * Loads a ServerGrid from a serialized list of {@link ServerMatrix ServerMatrices}. 
     * All ServerMatrices, their {@link GridNode GridNodes}, and their {@link GridLink GridLinks}
     * are loaded from here. This method is called by the {@link com.quattage.mechano.foundation.api.SidedGridDisptacher.Serializer serializer}
     * @param subgrids A two-dimensional {@link ListTag} - A list of subgrids, where each subgrid is a list of {@link CompoundTag CompoundTags}
     * @param world World that the resulting ServerGrid uses to look up {@link PowerGridBlockEntity PGBEs}
     * @return a new ServerGrid with data primed from the provided list of grids.
     */
    public static ServerGrid loadFrom(ListTag subgrids, ServerLevel world) {
        ServerGrid freshGlobal = new ServerGrid(world,  new ObjectArrayList<>(subgrids.size()));
        for(int x = 0; x < subgrids.size(); x++) {
            ListTag writtens = subgrids.getList(x);
            if(writtens.isEmpty()) continue;
            ServerMatrix freshLocal = new ServerMatrix(freshGlobal, writtens.size());
            for(int y = 0; y < writtens.size(); y++) {
                CompoundTag node = writtens.getCompound(y);
                createNodeAndMakeProvisionalLinks(freshGlobal, freshLocal, DiscriminatorData.read(node), node.getList("links", Tag.TAG_COMPOUND));
            }
            // if the powergrid failed to deserialize make sure it gets removed
            if(freshLocal.nodes.isEmpty()) {
                if(freshLocal.gridIndex < 0 || freshLocal.gridIndex >= subgrids.size() || freshGlobal.matrices.remove(freshLocal.gridIndex) == null)
                    freshGlobal.matrices.remove(freshLocal);
                freshLocal.nullify();
            }
        }
        return freshGlobal;
    }

    /**
     * A breakout method for making {@link ServerGrid#loadFrom} easier to read - 
     * This method is responsible for de-serializing the GridNode and its links <p>
     * getOrCreate is used here to ensure that only one canonical reference to each GridNode exists after
     * the ServerGrid is loaded. GridNodes may have already been created as links by previous calls
     * to this method.
     * 
     * TODO A recursive approach may reduce iteration count slightly if links are created depth-first rather than re-addressing provisional links
     */
    private static void createNodeAndMakeProvisionalLinks(ServerGrid grid, ServerMatrix instantiator, GridUUID address, @Nullable ListTag links) {
        if(links == null || links.isEmpty()) return;
        GridNode newStart = instantiator.getOrCreateProvisional(address);
        if(newStart == null) return;
        newStart.links.ensureCapacity(links.size());

        for(int x = 0; x < links.size(); x++) {
            CompoundTag serializedLink = links.getCompound(x);
            GridUUID endAddress = DiscriminatorData.read(serializedLink);
            GridNode newEnd = instantiator.getOrCreateProvisional(endAddress);
            if(newEnd == null || newStart.equals(newEnd)) continue;

            Transmitter<?> trns = TransmitterRegistry.INSTANCE.get(serializedLink);
            GridLink newLink = new GridLink(grid.getWorld(), newStart, newEnd, trns);

            if(newLink.getTransmitter().needsSerialization()) {
                CompoundTag data = serializedLink.getCompound("data");
                if(!data.isEmpty()) newLink.getTransmitter().loadFrom(data);
            }
            newStart.links.add(newLink);
            grid.trackLink(newLink);
        }
        newStart.links.trim();
    }

    /**
     * Creates a new ServerGrid with a predefined list of subgrids.
     * Used internally by the {@link SidedGridDispatcher#SERIALIZER}
     * @param world ServerLevel that owns this grid
     * @param subgrids Subgrids to instantiate the new ServerGrid with
     */
    public ServerGrid(ServerLevel world, ObjectArrayList<ServerMatrix> subgrids) {
        super(world);
        this.matrices = subgrids;
    }




    /**
     * Create a link between any two {@link GridNode GridNodes} as long as their host
     * {@link PowerGridBlockEntity} instances exist in this ServerGrid's level.
     * This method is a no-questions-asked wrapper for {@link ServerGrid#linkUnsafe linkUnsafe}
     * that performs operations to maintain the integrity of this ServerGrid regardless of its 
     * internal state when linking. It does this by creating new {@link GridNode GridNodes} and/or 
     * {@link ServerMatrix ServerMatrices} where necessary. 
     * <p>
     * {@link ServerGrid#linkUnsafe linkUnsafe} instead.
     * @param start Starting address 
     * @param end Ending address
     * @param type The type of link that will be created between <code>start</code> and <code>end</code>
     * @return {@link LinkResponseHolder} holding the link that was created as well as a response describing whether or not
     * the link was successful
     */
    public LinkResponseHolder createLink(GridUUID start, GridUUID end, TransmitterType<?> type) {

        // prep and sanity checks
        AnchorPointable startHost = start.getHolder(world);
        AnchorPointable endHost = end.getHolder(world);

        if(startHost == null) {
            Mechano.LOGGER.error("Failed to create link from " + start + " to " + end + " - No valid PGBE could be found at the starting address!");
            GridNode startNode = lookup(start).getSecond();
            GridNode endNode = lookup(end).getSecond();
            return LinkResponseHolder.of(startNode, endNode, Response.Link.FAIL_SYNC_OUTDATED);
        } else if(endHost == null) {
            GridNode startNode = lookup(start).getSecond();
            GridNode endNode = lookup(end).getSecond();
            Mechano.LOGGER.error("Failed to create link from " + start + " to " + end + " - No valid PGBE could be found at the ending address!");
            return LinkResponseHolder.of(startNode, endNode, Response.Link.FAIL_SYNC_OUTDATED);
        }

        Transmitter<?> trns = type.make();

        // nodes both belong to grids
        if(startHost.getSurrogate().isSynced() && endHost.getSurrogate().isSynced()) {

            ServerMatrix startPG = startHost.getSurrogate().getOwner();
            ServerMatrix endPG = endHost.getSurrogate().getOwner();

            // nodes both belong to the same grid
            if(startPG.gridIndex == endPG.gridIndex) {
                endPG = null;
                GridNode startNode = startPG.nodes.get(start);
                GridNode endNode = startPG.nodes.get(end);
                GridLink newLink = new GridLink(getWorld(), startNode, endNode, trns);

                startHost.getSurrogate().owner = startPG;
                endHost.getSurrogate().owner = startPG;
                if(startNode.links.contains(newLink)) return LinkResponseHolder.of(newLink, Response.Link.FAIL_DUPLICATE);
                return LinkResponseHolder.of(linkUnsafe(newLink, startHost, endHost), Response.SUCCESS);
            }

            // nodes both belong to different grids
            ServerMatrix merged = mergeGrids(startPG.gridIndex, endPG.gridIndex);
            GridNode startNode = merged.nodes.get(start);
            GridNode endNode = merged.nodes.get(end);
            startHost.getSurrogate().owner = merged;
            endHost.getSurrogate().owner = merged;
            return LinkResponseHolder.of(linkUnsafe(startNode, startHost, endNode, endHost, trns), Response.SUCCESS);
        }

        // start belongs to grid, but end doesn't
        if(startHost.getSurrogate().isSynced() && !endHost.getSurrogate().isSynced()) {
            GridNode startNode = startHost.getSurrogate().owner.nodes.get(start);
            GridNode endNode = new GridNode(startHost.getSurrogate().getOwner(), endHost, end);
            startHost.getSurrogate().owner.nodes.add(endNode);
            endHost.getSurrogate().owner = startHost.getSurrogate().owner;
            endHost.onAddedToGrid(getWorld(), startHost.getSurrogate().owner);
            return LinkResponseHolder.of(linkUnsafe(startNode, startHost, endNode, endHost, trns), Response.SUCCESS);
        }

        // end belongs to grid, but start doesn't
        if(!startHost.getSurrogate().isSynced() && endHost.getSurrogate().isSynced()) {
            GridNode startNode = new GridNode(endHost.getSurrogate().getOwner(), startHost, start); 
            GridNode endNode = endHost.getSurrogate().owner.nodes.get(end);
            endHost.getSurrogate().owner.nodes.add(startNode);
            startHost.getSurrogate().owner = endHost.getSurrogate().owner;
            startHost.onAddedToGrid(getWorld(), endHost.getSurrogate().owner);
            return LinkResponseHolder.of(linkUnsafe(startNode, startHost, endNode, endHost, trns), Response.SUCCESS);
        }

        // neither belongs to grid
        if(!startHost.getSurrogate().isSynced() && !endHost.getSurrogate().isSynced()) {
            ServerMatrix newGrid = new ServerMatrix(this, 2);
            GridNode startNode = new GridNode(newGrid, startHost, start);
            GridNode endNode = new GridNode(newGrid, endHost, end);
            newGrid.nodes.add(startNode);
            newGrid.nodes.add(endNode);
            startHost.getSurrogate().owner = newGrid;
            endHost.getSurrogate().owner = newGrid;
            startHost.onAddedToGrid(getWorld(), newGrid);
            endHost.onAddedToGrid(getWorld(), newGrid);
            return LinkResponseHolder.of(linkUnsafe(startNode, startHost, endNode, endHost, trns), Response.SUCCESS);
        }

        return LinkResponseHolder.of(null, Response.FAIL_GENERIC);
    }









    /**
     * Takes the contents from one {@link ServerMatrix} in this ServerGrid and 
     * merges it into the other. 
     * <p>
     * Nodes are merged by order of index, so a the contents of a grid with a HIGHER
     * index are merged onto a grid with a LOWER index. This also means that no new 
     * ServerMatrix instances are created - the ServerMatrix located at the lower of the 
     * two indices is recycled. 
     * <p>
     * Additionally, if the merge is successful, this call will immediately mark 
     * the the grid belonging to the higher index for removal. If this ServerMatrix 
     * instance is stored anywhere, its reference should be nullified to prevent 
     * a leak. Only use the ServerMatrix returned from this method.
     * 
     * @param index1 The index of the first ServerMatrix to merge
     * @param index2 The index of the second ServerMatrix to merge
     * @return A reference to the resulting ServerMatrix. This reference will be 
     * identical to the ServerMatrix found at the lower of the two provided indices, 
     * but it will contain the contents of both grids.
     * @throws ArrayIndexOutOfBoundsException if the indices provided are outside the bounds of the subgrids list.
     */
    public ServerMatrix mergeGrids(int index1, int index2) {

        if(index1 < 0 || index1 >= matrices.size())
            throw new ArrayIndexOutOfBoundsException("Failed to merge grids - Index1 '" + index1 + "' is out of bounds for a ServerGrid with " + matrices.size() + " subgrids!");
        if(index2 < 0 || index2 >= matrices.size())
            throw new ArrayIndexOutOfBoundsException("Failed to merge grids - Index1 '" + index2 + "' is out of bounds for a ServerGrid with " + matrices.size() + " subgrids!");

        if(index1 == index2)
            return matrices.get(index1);

        // merge 2 into 1
        if(index1 < index2) {
            ServerMatrix grid1 = matrices.get(index1);
            grid1.gridIndex = index1;
            ServerMatrix grid2 = matrices.remove(index2);
            grid1.addAll(grid2.nodes);
            grid2.nullify();
            updateGridIndices(index1);
            return grid1;
        }

        // merge 1 into 2
        if(index2 < index1) {
            ServerMatrix grid2 = matrices.get(index2);
            grid2.gridIndex = index2;
            ServerMatrix grid1 = matrices.remove(index1);
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
     * {@link ServerGrid#createLink createLink} instead.
     * @param start GridNode start
     * @param end GridNode end
     * @param trns The transmitter that the link will contain
     * @return The GridLink that was created
     */
    public GridLink linkUnsafe(GridNode start, @Nullable AnchorPointable startHost, GridNode end, @Nullable AnchorPointable endHost, Transmitter<?> trns) {
        GridLink link = new GridLink(getWorld(), start, end, trns);
        return linkUnsafe(link, startHost, endHost);
    }




    /**
     * Creates a symmetrical link between <code>start</code> and <code>end</code>. 
     * Does not perform any checks to ensure that this link is valid. When in doubt, use
     * {@link ServerGrid#createLink createLink} instead.
     * @param link GridLink to add
     * @param trns The transmitter that the link will contain
     * @return The GridLink provided
     */
    public GridLink linkUnsafe(GridLink link, @Nullable AnchorPointable startHost, @Nullable AnchorPointable endHost) {
        link.getStartNode().links.add(link);
        trackLink(link);
        GridLink inverse = link.copyAndFlip();
        inverse.getStartNode().links.add(inverse);
        trackLink(inverse);
        link.getTransmitter().onConnectionCreated(getWorld(), link);
        if(startHost != null) startHost.onConnectionMade(world, link);
        if(endHost != null) endHost.onConnectionMade(world, inverse);
        return link;
    }


    /**
     * Adds the provied GridLink to this ServerGrid's 
     * tracked links set.
     * @param link Link to add
     */
    public void trackLink(GridLink link) {
        ChunkPos startChunkPos = new ChunkPos(link.getStartNode().getAddress().getBlockPos(world));
        ObjectOpenHashSet<GridLink> links = linksByChunk.get(startChunkPos);
        if(links == null) {
            links = new ObjectOpenHashSet<>();
            links.add(link);
            linksByChunk.put(startChunkPos, links);
            return;
        }
        links.add(link);
    }

    public void untrackLink(GridLink link) {
        ChunkPos startChunkPos = new ChunkPos(link.getStartNode().getAddress().getBlockPos(world));
        List<GridLink> links = linksByChunk.get(startChunkPos);
        if(links == null) return;
        links.remove(link);
        if(links.isEmpty()) linksByChunk.remove(startChunkPos);
    }


    /**
     * Finds a node at the given address by iterating through all subgrids.
     * @param address {@link com.quattage.mechano.foundation.api.landmark.GridIdentifier.base.NodeIdentifier NodeIdentifier} to look for
     * @return A pair containing the {@link GridNode} and its {@link ServerMatrix parent}. If a node is not found
     * at the given address, the contents of the pair will be null.
     */
    public Pair<ServerMatrix, GridNode> lookup(GridUUID address) {
        for(int x = 0; x < matrices.size(); x++) {
            ServerMatrix grid = matrices.get(x);
            GridNode get = grid.nodes.get(address);
            if(get != null) return Pair.of(grid, get);
        }
        return Pair.of(null, null);
    }




    /**
     * Iterates over all {@link ServerMatrix} instances
     * and clears them. Broadcasts updates as a result.
     */
    public void clear() {
        Iterator<ServerMatrix> it = matrices.iterator();
        while(it.hasNext()) {
            ServerMatrix grid = it.next();
            grid.clear();
            it.remove();
        }
        matrices.trim(1);
    }




    /**
     * Iteratively updates all ServerMatrix indices to match where they are
     * in this ServerGrid's subgrid array. This is necessary
     * any time the list gets smaller.
     * @param startingIndex Index to start from. Normally, this would be the index that was removed.
     */
    public void updateGridIndices(int startingIndex) {
        for(int x = startingIndex; x < matrices.size(); x++)
            matrices.get(x).gridIndex = x;
    }




    /**
     * Destroys the grid at the given index, or does nothing
     * if the index doesn't exist in this ServerGrid.
     * @param index
     * @return <code>true</code> if this ServerGrid was modified as a result of this call
     */
    public boolean destroyGridAt(int index) {
        if(index < 0 || index >= matrices.size())
            return false;
        ServerMatrix grid = matrices.remove(index);
        if(grid == null) return false;
        grid.nullify();

        if(matrices.isEmpty()) {
            matrices.trim();
            return true;
        }

        updateGridIndices(index);
        return true;
    }


    /**
     * Destroys the grid at the given index, or does nothing
     * if the index doesn't exist in this ServerGrid.
     * @param grid ServerMatrix to destroy
     * @return <code>true</code> if this ServerGrid was modified as a result of this call
     */
    public boolean destroyGrid(ServerMatrix grid) {
        int index = grid.gridIndex;
        if(grid.gridIndex < 0 || grid.gridIndex >= matrices.size())
            index = matrices.indexOf(grid);
        if(index < 0) {
            grid.nullify();
            return false;
        }
        matrices.remove(index);
        grid.nullify();

        if(matrices.isEmpty()) {
            matrices.trim();
            return true;
        }

        updateGridIndices(index);
        return true;
    }

    /**
     * Adds every member of the supplied list to this ServerGrid's 
     * subgrids list and updates their indices.
     * @param grids Grids to add
     */
    public void addAll(List<ServerMatrix> grids) {
        if(grids.isEmpty()) return;
        matrices.ensureCapacity(matrices.size() + grids.size());
        for(int x = 0; x < grids.size(); x++) {
            ServerMatrix grid = grids.get(x);
            if(grid.nodes.isEmpty()) {
                grid.nullify();
                continue;
            }
            matrices.add(grid);
            grid.gridIndex = matrices.size() - 1;
        }
        matrices.trim();
    }


    /**
     * @return The ServerLevel that this ServerGrid is attached to.
     */
    @Override
    public ServerLevel getWorld() {
        return (ServerLevel)super.getWorld();
    }




    /**
     * Writes this entire ServerGrid to a new ListTag
     * @returns A new ListTag, made of {@link CompoundTag CompoundTags}
     * acquired by the ServerMatrix's {@link com.quattage.mechano.foundation.api.landmark.NodeMap#write writing process}
     */
    @Override
    protected @Nullable ListTag writeAll() {
        ListTag output = new ListTag();
        for(ServerMatrix grid : matrices) {
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
