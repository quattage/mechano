
package com.quattage.mechano.foundation.api;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.landmark.Connection.InsertionMode;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.Response;
import com.quattage.mechano.foundation.api.switchboard.Response.LinkResponseHolder;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.data.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;

/**
 * A GlobalGrid is the top-level manager for {@link ServerMatrix ServerMatrices}.
 * Data Attachments are leveraged to add one GlobalGrid to each level/dimension.
 */
public final class ServerGrid extends SidedGridDispatcher {

    public ObjectArrayList<ServerMatrix> matrices;

    private @Nullable Set<DeferredMember> deferred = new HashSet<>();

    /**
     * Loads a ServerGrid from a serialized list of {@link ServerMatrix ServerMatrices}. 
     * All ServerMatrices, their {@link GridNode GridNodes}, and their {@link GridLink GridLinks}
     * are loaded from here. This method is called by the {@link com.quattage.mechano.foundation.api.SidedGridDisptacher.Serializer serializer}
     * @param subgrids A two-dimensional {@link ListTag} - A list of subgrids, where each subgrid is a list of {@link CompoundTag CompoundTags}
     * @param world World that the resulting ServerGrid uses to look up {@link GriddableBlockEntity PGBEs}
     * @return a new ServerGrid with data primed from the provided list of grids.
     */
    public static ServerGrid loadFrom(ListTag subgrids, ServerLevel world) {
        ServerGrid freshGlobal = new ServerGrid(world,  new ObjectArrayList<>(subgrids.size() + 1));
        for(int x = 0; x < subgrids.size(); x++) {
            ListTag writtens = subgrids.getList(x);
            if(writtens.isEmpty()) continue;
            ServerMatrix freshLocal = new ServerMatrix(writtens.size());
            for(int y = 0; y < writtens.size(); y++) {
                CompoundTag node = writtens.getCompound(y);
                freshGlobal.createNodeAndMakeProvisionalLinks(world, freshLocal, UUIDDiscriminator.read(node), node.getList("links", Tag.TAG_COMPOUND), false);
            }
            if(!freshLocal.nodes.isEmpty()) {
                freshLocal.global = freshGlobal;
                freshLocal.gridIndex = freshGlobal.matrices.size();
                freshGlobal.matrices.add(freshLocal);
            }
        }
        freshGlobal.matrices.trim();
        return freshGlobal;
    }

    /**
     * A breakout method for making {@link ServerGrid#loadFrom} easier to read - 
     * This method is responsible for de-serializing the GridNode and its links <p>
     * getOrCreate is used here to ensure that only one canonical reference to each GridNode exists after
     * the ServerGrid is loaded. GridNodes may have already been created as links by previous calls
     * to this method.
     * TODO A recursive approach may reduce iteration count slightly if links are created depth-first rather than re-addressing provisional links
     */
    private void createNodeAndMakeProvisionalLinks(LevelReader world, ServerMatrix instantiator, GridUUID address, @Nullable ListTag links, boolean storeDeferred) {
        if(links == null || links.isEmpty()) return;
        GridNode newStart = instantiator.getOrCreateProvisional(world, address, !storeDeferred);
        if(newStart == null) {
            if(storeDeferred) deferred.add(new DeferredMember(null, instantiator, address, links));
            return;
        }
        loadLinksFor(newStart, instantiator, links, true);
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

    protected void loadLinksFor(GridNode newStart, ServerMatrix instantiator, ListTag links, boolean storeDeferred) {
        newStart.prime(links.size());
        for(int x = 0; x < links.size(); x++) {
            CompoundTag serializedLink = links.getCompound(x);
            GridUUID endAddress = UUIDDiscriminator.read(serializedLink);
            GridNode newEnd = instantiator.getOrCreateProvisional(world, endAddress, !storeDeferred);
            if(newEnd == null) {
                if(storeDeferred) this.deferred.add(new DeferredMember(newStart, instantiator, endAddress, links));
                continue;
            }
            GridLink newLink = new GridLink(world, newStart, newEnd, TransmitterRegistry.INSTANCE.get(serializedLink));
            newLink.setTension(Tension.values()[serializedLink.getByte("ten")]);
            if(newLink.getTransmitter().needsSerialization()) {
                CompoundTag data = serializedLink.getCompound("data");
                if(!data.isEmpty()) newLink.getTransmitter().loadFrom(data);
            }
            linkUnsafe(newLink, null, null);
        }
        newStart.trim();
    }

    @Override
    protected void onLoad() {
        Mechano.LOGGER.info("deferred: " + deferred.toString());
        for(DeferredMember trgt : deferred) {
            if(trgt.root == null) createNodeAndMakeProvisionalLinks(world, trgt.instantiator, trgt.address, trgt.links, true);
            else loadLinksFor(trgt.root, trgt.instantiator, trgt.links, false);
        }
        deferred = null;
    }

    @Override
    protected void onUnload() {
        deferred = new HashSet<>();
    }

    /**
     * Create a link between any two {@link GridNode GridNodes} as long as their host
     * {@link GriddableBlockEntity} instances exist in this ServerGrid's level.
     * This method is a no-questions-asked wrapper for {@link ServerGrid#linkUnsafe linkUnsafe}
     * that performs operations to maintain the integrity of this ServerGrid regardless of its 
     * internal state when linking. It does this by creating new {@link GridNode GridNodes} and/or 
     * {@link ServerMatrix ServerMatrices} where necessary. 
     * <p>
     * If you already know your link is good, and you don't need the additional overhead
     * incurred by this method, you can use {@link ServerGrid#linkUnsafe linkUnsafe} instead.
     * @param start Starting address 
     * @param end Ending address
     * @param type The type of link that will be created between <code>start</code> and <code>end</code>
     * @return {@link LinkResponseHolder} holding the link that was created as well as a response describing whether or not
     * the link was successful
     */
    public LinkResponseHolder createLink(GridUUID start, GridUUID end, TransmitterType<?> type) {

       // prep and sanity checks
        AnchorPointable<?> startPoints = start.getAnchorPoints(world);
        AnchorPointable<?> endPoints = end.getAnchorPoints(world);

        if(startPoints == null) {
            Mechano.LOGGER.error("Failed to create link from " + start + " to " + end + " - No valid PGBE could be found at the starting address!");
            GridNode startNode = lookup(start).getSecond();
            GridNode endNode = lookup(end).getSecond();
            return LinkResponseHolder.of(startNode, endNode, Response.Link.FAIL_SYNC_OUTDATED);
        } else if(endPoints == null) {
            GridNode startNode = lookup(start).getSecond();
            GridNode endNode = lookup(end).getSecond();
            Mechano.LOGGER.error("Failed to create link from " + start + " to " + end + " - No valid PGBE could be found at the ending address!");
            return LinkResponseHolder.of(startNode, endNode, Response.Link.FAIL_SYNC_OUTDATED);
        }

        Transmitter<?> trns = type.make();

        // nodes both belong to grids
        if(startPoints.getSurrogate().isSynced() && endPoints.getSurrogate().isSynced()) {

            ServerMatrix startPG = startPoints.getSurrogate().getOwnerMatrix();
            ServerMatrix endPG = endPoints.getSurrogate().getOwnerMatrix();

            // nodes both belong to the same grid
            if(startPG.gridIndex == endPG.gridIndex) {
                endPG = null;
                GridNode startNode = startPG.nodes.get(start);
                GridNode endNode = startPG.nodes.get(end);
                GridLink newLink = new GridLink(world, startNode, endNode, trns);
                startPoints.getSurrogate().sync(world, startPG);
                endPoints.getSurrogate().sync(world, startPG);
                if(startNode.hasLink(newLink)) return LinkResponseHolder.of(newLink, Response.Link.FAIL_DUPLICATE);
                return LinkResponseHolder.of(linkUnsafe(newLink, startPoints, endPoints), Response.SUCCESS);
            }

            // nodes both belong to different grids
            ServerMatrix merged = mergeGrids(startPG.gridIndex, endPG.gridIndex);
            GridNode startNode = merged.nodes.get(start);
            GridNode endNode = merged.nodes.get(end);
            startPoints.getSurrogate().sync(world, merged);
            endPoints.getSurrogate().sync(world, merged);
            return LinkResponseHolder.of(linkUnsafe(startNode, startPoints, endNode, endPoints, trns), Response.SUCCESS);
        }

        // start belongs to grid, but end doesn't
        if(startPoints.getSurrogate().isSynced() && !endPoints.getSurrogate().isSynced()) {
            GridNode startNode = startPoints.getSurrogate().constituents().get(start);
            GridNode endNode = new GridNode(startPoints.getSurrogate().getOwnerMatrix(), endPoints, end);
            startPoints.getSurrogate().constituents().add(endNode);
            endPoints.getSurrogate().sync(startPoints.getSurrogate());
            endPoints.onAddedToGrid(getWorld(), startPoints.getSurrogate().getOwnerMatrix());
            return LinkResponseHolder.of(linkUnsafe(startNode, startPoints, endNode, endPoints, trns), Response.SUCCESS);
        }

        // end belongs to grid, but start doesn't
        if(!startPoints.getSurrogate().isSynced() && endPoints.getSurrogate().isSynced()) {
            GridNode startNode = new GridNode(endPoints.getSurrogate().getOwnerMatrix(), startPoints, start); 
            GridNode endNode = endPoints.getSurrogate().constituents().get(end);
            endPoints.getSurrogate().constituents().add(startNode);
            startPoints.getSurrogate().sync(endPoints.getSurrogate());
            startPoints.onAddedToGrid(getWorld(), endPoints.getSurrogate().getOwnerMatrix());
            return LinkResponseHolder.of(linkUnsafe(startNode, startPoints, endNode, endPoints, trns), Response.SUCCESS);
        }

        // neither belongs to grid
        if(!startPoints.getSurrogate().isSynced() && !endPoints.getSurrogate().isSynced()) {
            ServerMatrix newMatrix = new ServerMatrix(this, 2);
            GridNode startNode = new GridNode(newMatrix, startPoints, start);
            GridNode endNode = new GridNode(newMatrix, endPoints, end);
            newMatrix.nodes.add(startNode);
            newMatrix.nodes.add(endNode);
            startPoints.getSurrogate().sync(world, newMatrix);
            endPoints.getSurrogate().sync(world, newMatrix);
            startPoints.onAddedToGrid(getWorld(), newMatrix);
            endPoints.onAddedToGrid(getWorld(), newMatrix);
            return LinkResponseHolder.of(linkUnsafe(startNode, startPoints, endNode, endPoints, trns), Response.SUCCESS);
        }
        return LinkResponseHolder.of(null, Response.FAIL_GENERIC);
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
    public GridLink linkUnsafe(GridNode start, @Nullable AnchorPointable<?> startPoints, GridNode end, @Nullable AnchorPointable<?> endPoints, Transmitter<?> trns) {
        GridLink link = new GridLink(getWorld(), start, end, trns);
        return linkUnsafe(link, startPoints, endPoints);
    }

    /**
     * Creates a symmetrical link between <code>start</code> and <code>end</code>. 
     * Does not perform any checks to ensure that this link is valid. When in doubt, use
     * {@link ServerGrid#createLink createLink} instead.
     * @param link GridLink to add
     * @param trns The transmitter that the link will contain
     * @return The GridLink provided
     */
    public GridLink linkUnsafe(GridLink link, @Nullable AnchorPointable<?> startPoints, @Nullable AnchorPointable<?> endPoints) {
        link.getStartNode().addLink(link);
        GridLink inverse = link.inverseCopy();
        link.getEndNode().addLink(inverse);
        link.getTransmitter().onConnectionCreated(getWorld(), link);
        if(startPoints != null) startPoints.onConnectionMade(world, link);
        if(endPoints != null) endPoints.onConnectionMade(world, inverse);
        link.pushTo(getWorld(), InsertionMode.SINGLE);
        inverse.pushTo(getWorld(), InsertionMode.SINGLE);
        return link;
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

    public LinkResponseHolder destroyLink(GridUUID start, GridUUID end) {
        AnchorPointable<?> startPoints = start.getAnchorPoints(world);
        AnchorPointable<?> endPoints = end.getAnchorPoints(world);

        GridNode startNode = startPoints == null ? null 
            : startPoints.getSurrogate().isSynced() 
            ? startPoints.getSurrogate().getOwnerMatrix().nodes.get(start) 
            : null;

        GridNode endNode = endPoints == null ? null 
            : endPoints.getSurrogate().isSynced() 
            ? endPoints.getSurrogate().getOwnerMatrix().nodes.get(end) 
            : null;

        if(startNode != null)
            startNode.removeLinksInvolving(null, end);
        if(endNode != null)
            endNode.removeLinksInvolving(null, start);

        return LinkResponseHolder.of(startNode, endNode, Response.SUCCESS);
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
        grid.nullify();
        matrices.trim();
        updateGridIndices(index);
        return true;
    }


    /**
     * Destroys the matrix at the given index, or does nothing
     * if the index doesn't exist in this ServerGrid.
     * @param matrix ServerMatrix to destroy
     * @return <code>true</code> if this ServerGrid was modified as a result of this call
     */
    public boolean destroyMatrix(ServerMatrix matrix) {
        int index = matrix.gridIndex;
        if(index < 0 || index >= matrices.size())
            index = matrices.indexOf(matrix);
        if(index < 0) {
            matrix.nullify();
            return false;
        }
        matrices.remove(index);
        matrix.nullify();
        matrices.trim();
        updateGridIndices(index);
        return true;
    }

    /**
     * Adds every member of the supplied list to this ServerGrid's 
     * matrix list and updates their indices.
     * @param addedMatrices Matrices to add
     */
    public void addAll(List<ServerMatrix> addedMatrices) {
        if(addedMatrices.isEmpty()) return;
        matrices.ensureCapacity(matrices.size() + addedMatrices.size());
        for(int x = 0; x < addedMatrices.size(); x++) {
            ServerMatrix grid = addedMatrices.get(x);
            if(grid.nodes.isEmpty()) {
                grid.nullify();
                continue;
            }
            matrices.add(grid);
            grid.gridIndex = matrices.size() - 1;
            grid.global = this;
        }
        matrices.trim();
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

    /**
     * @return The ServerLevel that this ServerGrid is attached to.
     */
    @Override
    public ServerLevel getWorld() {
        return (ServerLevel)super.getWorld();
    }


    @Override
    protected String getDistPrefix() {
        return "SERVER";
    }

    private static record DeferredMember(@Nullable GridNode root, ServerMatrix instantiator, GridUUID address, @Nullable ListTag links) {
        @Override
        public final boolean equals(Object other) {
            if(!(other instanceof DeferredMember that)) return false;
            return this.address.equals(that.address);
        }
        @Override
        public final int hashCode() {
            return address.hashCode();
        }
    }
}
