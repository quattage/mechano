
package com.quattage.mechano.foundation.api;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.api.switchboard.LinkResponsePacket;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

    public static final boolean ALLOW_DYNAMIC_REASSERTIONS = true;

    public ObjectArrayList<ServerMatrix> matrices;
    private LinkDataTracker tracker = null;

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
            ServerMatrix freshLocal = new ServerMatrix(freshGlobal, writtens.size());
            for(int y = 0; y < writtens.size(); y++) {
                CompoundTag node = writtens.getCompound(y);
                freshGlobal.createNodeAndMakeProvisionalLinks(world, freshLocal, UUIDDiscriminator.read(node), node.getList("links", Tag.TAG_COMPOUND), false);
            }
            freshLocal.loadInto(freshGlobal);
        }
        freshGlobal.matrices.trim();
        return freshGlobal;
    }

    /**
     * Creates a new ServerGrid with a predefined list of subgrids.
     * Used internally by the {@link SidedGridDispatcher#SERIALIZER}
     * @param world ServerLevel that owns this grid
     * @param subgrids Subgrids to instantiate the new ServerGrid with
     */
    protected ServerGrid(ServerLevel world, ObjectArrayList<ServerMatrix> subgrids) {
        super(world);
        this.matrices = subgrids;
        if(Mechano.USE_VERBOSE_LINK_TRACKING)
            tracker = new LinkDataTracker().enable().withLogging(LOGGER);
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
        GridNode newStart = GridNode.getOrCreateOnLoad(instantiator, address, !storeDeferred);
        if(newStart == null) {
            if(storeDeferred) deferred.add(new DeferredMember(null, instantiator, address, links));
            return;
        }
        loadLinksFor(newStart, instantiator, links, true);
    }

    protected void loadLinksFor(GridNode newStart, ServerMatrix instantiator, ListTag links, boolean storeDeferred) {
        newStart.primeLinks(links.size());
        for(int x = 0; x < links.size(); x++) {
            CompoundTag serializedLink = links.getCompound(x);
            GridUUID endAddress = UUIDDiscriminator.read(serializedLink);
            GridNode newEnd = GridNode.getOrCreateOnLoad(instantiator, endAddress, !storeDeferred);
            if(newEnd == null) {
                if(storeDeferred) this.deferred.add(new DeferredMember(newStart, instantiator, endAddress, links));
                continue;
            }
            GridLink newLink = new GridLink(world, newStart, newEnd, TransmitterRegistry.INSTANCE.get(serializedLink));
            if(newLink.getTransmitter().needsSerialization()) {
                CompoundTag data = serializedLink.getCompound("data");
                if(!data.isEmpty()) newLink.getTransmitter().loadFrom(data);
            }
            linkUnsafe(newLink, false);
        }
        newStart.trimLinks();
    }

    public @Nullable ServerMatrix getMatrixByIndex(int index) {
        if(index < 0 || index >= matrices.size()) return null;
        return matrices.get(index);
    }

    @Override
    protected void onLoad() {
        if(deferred == null || deferred.isEmpty()) 
            return;
        for(DeferredMember trgt : deferred) {
            if(trgt.root == null) 
                createNodeAndMakeProvisionalLinks(world, trgt.instantiator, trgt.address, trgt.links, true);
            else loadLinksFor(trgt.root, trgt.instantiator, trgt.links, false);
        }
        deferred = null;
    }

    @Override
    protected void onUnload() {
        deferred = new HashSet<>();
    }

    @Override
    protected void tick() {
        
    }

    // TODO do this lol
    public void swapEndPoint(GridUUID start, GridUUID oldEnd, GridUUID newEnd) {
        
    }

    /**
     * Destroys all {@link GridLink GridLinks} present in this {@link ServerGrid} that match the given 
     * <code>start</code> and <code>end</code> addresses. Calls to this method will send multiple packets
     * to sync {@link AnchorPoint} status as well as to destroy the link itself. This method is designed
     * to be invoked in explicit scenarios, where the player (or some entity or level event) deliberately 
     * destroys a link between two nodes. Chunk unloading is not a good use case for this method, 
     * since the changes that this method makes are synced between the client and server, and so are
     * permanent.
     * <h2>Link Symmetry</h2>
     * As far as implementations need to be aware, GridLinks are completely symmetrical. In the 
     * {@link ServerMatrix matrix} itself, The starting {@link GridNode node} stores a reference 
     * to the end, and vice-versa. This means that "start" and "end" as they're passed to this method
     * are totally ambiguous - you can pass your UUIDs in any order you want, and it will be dealt with 
     * internally.
     * @param start
     * @param end
     */
    public void destroyLink(GridUUID start, GridUUID end) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        if(start.equals(end)) {
            throw new IllegalArgumentException("A call to destroyLink() was made with identical start and end points! (" 
                + start + ") - For now, this condition results in a hard throw, so report this if you see it thanks" );
        }
        Griddable<?> startPoints = start.getOrFindGriddable(world);
        Griddable<?> endPoints = end.getOrFindGriddable(world);
        if(!Griddable.assertPairExists(startPoints, endPoints, start, end))
            return;
        GridNode startNode = GridNode.getFrom(getWorld(), start, startPoints);
        GridNode endNode = GridNode.getFrom(getWorld(), end, endPoints);
        if(startNode == null && endNode == null) {
            Mechano.LOGGER.error("Failed to destroy a link from " + start.toString(getWorld()) + " to " 
                + end.toString(getWorld()) + " - Neither address contains an in-world Griddable!");
            return;
        }
        final Set<GridUUID> empties = new HashSet<>();
        GridLink removed = startNode.getOwner().deLink(startNode, endNode, empties);
        LinkDataStorable.popAsServer(getWorld(), removed);
        removed.broadcast(getWorld(), GridResponse.TASK_DESTROY_LINK);
        startNode.getOwner().cleanup(empties, true);
    }

    /**
     * Creates a new {@link GridLink} between <code>start</code> and </code>end</code>
     * This method will perform several safety operations with {@link ServerMatrix} instances
     * to ensure the safety of data in this ServerGrid. These checks add additional overhead,
     * so if you want to add a link directly, you can use {@link #linkUnsafe} 
     * for lower-level access. Note that this method and its consituents all send packets
     * to the client themselves, so this shouldn't be done externally.
     * @param start The starting point of the link
     * @param end The ending point of the link
     * @param type The {@link TransmitterType} to use, which will supply a new 
     * {@link Transmitter} instance to the resulting link internally.
     */
    public void createLink(GridUUID start, GridUUID end, TransmitterType<?> type) {

        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        if(start.equals(end)) {
            throw new IllegalArgumentException("A call to createLink() was made with identical start and end points! (" 
                + start + ") - For now, this condition results in a hard throw, so report this if you see it thanks" );
        }
        if(type == null) type = MechanoTransmissionTypes.PERFECT_CONDUCTOR;

       // prep and sanity checks
        Griddable<?> startPoints = start.getOrFindGriddable(world);
        Griddable<?> endPoints = end.getOrFindGriddable(world);
        if(!Griddable.assertPairExists(startPoints, endPoints, start, end)) 
            return;

        boolean isStartSynced = startPoints.getSurrogate().isSynced(getWorld());
        boolean isEndSynced = endPoints.getSurrogate().isSynced(getWorld());

        if(isStartSynced && isEndSynced) {
            ServerMatrix startPG = startPoints.getSurrogate().getOwnerMatrix();
            ServerMatrix endPG = endPoints.getSurrogate().getOwnerMatrix();
            // nodes both belong to the same grid
            if(startPG.equals(endPG)) {
                endPG = null;
                GridNode startNode = GridNode.getOrCreate(startPG, startPoints, start);
                GridNode endNode = GridNode.getOrCreate(startPG, endPoints, end);
                GridLink newLink = new GridLink(world, startNode, endNode, type.make());
                startPoints.getSurrogate().sync(world, startPG);
                endPoints.getSurrogate().sync(world, startPG);
                if(startNode.hasLink(newLink)) {
                    newLink.sendToClientsTracking(getWorld(), LinkResponsePacket.of(newLink, GridResponse.FAIL_DUPLICATE));
                    return;
                }
                linkUnsafe(newLink, true);
                return;
            }
            // nodes both belong to different grids
            ServerMatrix merged = mergeMatrices(startPG, endPG);
            GridNode startNode = GridNode.getOrCreate(merged, startPoints, start);
            GridNode endNode = GridNode.getOrCreate(merged, endPoints, end);
            startPoints.getSurrogate().sync(world, merged);
            endPoints.getSurrogate().sync(world, merged);
            linkUnsafe(startNode, endNode, type.make(), true);
            return;
        }

        if(isStartSynced && !isEndSynced) {
            GridNode startNode = startPoints.getSurrogate().constituents().get(start);
            GridNode endNode = GridNode.getOrCreate(startPoints.getSurrogate().getOwnerMatrix(), endPoints, end);
            startPoints.getSurrogate().constituents().add(endNode);
            endPoints.getSurrogate().sync(getWorld(), startPoints.getSurrogate().getOwnerMatrix());
            endPoints.onAddedToMatrix(getWorld(), startPoints.getSurrogate().getOwnerMatrix());
            linkUnsafe(startNode, endNode, type.make(), true);
            return;
        }

        if(!isStartSynced && isEndSynced) {
            GridNode startNode = GridNode.getOrCreate(endPoints.getSurrogate().getOwnerMatrix(), startPoints, start); 
            GridNode endNode = endPoints.getSurrogate().constituents().get(end);
            endPoints.getSurrogate().constituents().add(startNode);
            startPoints.getSurrogate().sync(getWorld(), endPoints.getSurrogate().getOwnerMatrix());
            startPoints.onAddedToMatrix(getWorld(), endPoints.getSurrogate().getOwnerMatrix());
            linkUnsafe(startNode, endNode, type.make(), true);
            return;
        }

        if(!isStartSynced && !isEndSynced) {
            ServerMatrix newNodes = ServerMatrix.createAndPrepare(this);
            GridNode startNode = GridNode.createNew(newNodes, startPoints, start);
            GridNode endNode = GridNode.createNew(newNodes, endPoints, end);
            linkUnsafe(startNode, endNode, type.make(), true);
            return;
        }

        throw new IllegalStateException("bruh i got nothin lmao");
    }

    /**
     * Creates a symmetrical link between <code>start</code> and <code>end</code>. 
     * Does not perform any checks to ensure that this link is valid. When in doubt, use
     * {@link ServerGrid#createLink createLink} instead.
     * @param start GridNode start
     * @param end GridNode end
     * @param trns The transmitter that the link will contain
     * @param broadcast If <code>true</code>, this method will call 
     * @return The GridLink that was created
     */
    public GridLink linkUnsafe(GridNode start, GridNode end, Transmitter<?> trns, boolean broadcast) {
        GridLink link = new GridLink(getWorld(), start, end, trns);
        return linkUnsafe(link, broadcast);
    }

    /**
     * Creates a symmetrical link between <code>start</code> and <code>end</code>. 
     * Does not perform any checks to ensure that this link is valid. When in doubt, use
     * {@link ServerGrid#createLink createLink} instead.
     * @param link GridLink to add
     * @param trns The transmitter that the link will contain
     * @return The GridLink provided
     */
    public GridLink linkUnsafe(GridLink link, boolean broadcast) {
        link.getStartNode().addLink(link);
        GridLink inverse = link.inverseCopy();
        link.getEndNode().addLink(inverse);
        LinkDataStorable.put(getWorld(), link);
        if(broadcast) link.broadcast(getWorld(), GridResponse.TASK_CREATE_LINK);
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
     * bad access. Only use the ServerMatrix returned from this method.
     * 
     * @param matrixA the first matrix
     * @param matrixB the second matrix
     * @return A reference to the resulting ServerMatrix. This reference will be 
     * identical to the ServerMatrix found at the lower of the two provided indices, 
     * but it will contain the contents of both grids.
     * @throws ArrayIndexOutOfBoundsException if the indices provided are outside the bounds of the subgrids list.
     */
    public ServerMatrix mergeMatrices(ServerMatrix matrixA, ServerMatrix matrixB) {
        if(matrixA.getIndex() < matrixB.getIndex()) {
            matrices.remove(matrixB.getIndex());
            matrixA.addAll(matrixB.nodes);
            matrixB.destroy();
            updateGridIndices(matrixA.getIndex());
            return matrixA;
        }
        if(matrixA.getIndex() > matrixB.getIndex()) {
            matrices.remove(matrixA.getIndex());
            matrixB.addAll(matrixA.nodes);
            matrixA.destroy();
            updateGridIndices(matrixB.getIndex());
            return matrixB;
        }
        Mechano.LOGGER.warn("Attempted to merge matrices with the same index '" + matrixA.getIndex() + "'");
        return matrixA;
    }

    /**
     * Iteratively updates all ServerMatrix indices to match where they are
     * in this ServerGrid's subgrid array. This is necessary
     * any time the list gets smaller.
     * @param startingIndex Index to start from. Normally, this would be the index that was removed.
     */
    public void updateGridIndices(int startingIndex) {
        if(startingIndex < 0) startingIndex = 0;
        for(int x = startingIndex; x < matrices.size(); x++)
            matrices.get(x).setIndex(x);
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
        grid.destroy();
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
        int index = matrix.getIndex();
        if(index < 0 || index >= matrices.size())
            index = matrices.indexOf(matrix);
        if(index < 0) {
            matrix.destroy();
            return false;
        }
        matrices.remove(index);
        matrix.destroy();
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
                grid.destroy();
                continue;
            }
            grid.loadInto(this);
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
            if(grid == null || grid.nodes.isEmpty()) 
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

    protected boolean isValid() {
        return matrices != null && getWorld() != null;
    }

    @Override
    protected String getDistPrefix() {
        return "SERVER";
    }

    @Override
    protected LinkDataTracker getDebugTracker() {
        return tracker;
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
