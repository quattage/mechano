
package com.quattage.mechano.foundation.api;

import java.util.Iterator;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.landmarks.GridLink;
import com.quattage.mechano.foundation.api.landmarks.GridNode;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifiable;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier.Key;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;

import dev.engine_room.flywheel.backend.compile.core.LinkResult;

import com.quattage.mechano.foundation.api.transmission.Transmitable.LinkResponse;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.data.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;


/**
 * A GlobalGrid is the top-level manager for {@link PowerGrid PowerGrids}.
 * Data Attachments are leveraged to add one GlobalGrid to each level/dimension.
 */
public class GlobalServerGrid extends SidedGridDispatcher {

    private ObjectArrayList<PowerGrid> subgrids;

    /**
     * Loads a GlobalServerGrid from a serialized list of {@link PowerGrid PowerGrids}. 
     * All PowerGrids, their {@link GridNode GridNodes}, and their {@link GridLink GridLinks}
     * are loaded from here. This method is called by the {@link com.quattage.mechano.foundation.api.SidedGridDisptacher.Serializer serializer}
     * @param subgrids ListTag of {@link CompoundTag CompoundTags}, where each one is 
     * @param world World that the resulting GlobalServerGrid uses to look up {@link PowerGridBlockEntity PGBEs}
     * @return a new GlobalServerGrid with data primed from the provided list of grids.
     */
    public static GlobalServerGrid loadFrom(ListTag subgrids, ServerLevel world) {
        GlobalServerGrid freshInstance = new GlobalServerGrid(world,  new ObjectArrayList<>(subgrids.size()));
        for(int x = 0; x < subgrids.size(); x++) {
            ListTag writtens = subgrids.getList(x);
            PowerGrid freshGrid = new PowerGrid(freshInstance, subgrids.size());
            for(int y = 0; y < writtens.size(); y++) {
                CompoundTag node = writtens.getCompound(x);
                createNodeAndMakeProvisionalLinks(freshGrid, Key.loadFrom(node), node.getList("links", Tag.TAG_COMPOUND));
            }
            if(!freshGrid.nodes.isEmpty()) freshInstance.subgrids.add(freshGrid);
        }
        return freshInstance;
    }

    /**
     * A breakout method for making the above method easier to read - 
     * This method is responsible for de-serializing the GridNode and its links <p>
     * getOrCreate is used here to ensure that only one canonical reference to each GridNode exists after
     * the GlobalServerGrid is loaded. GridNodes may have already been created as links by previous calls
     * to this method.
     * 
     * TODO A recursive approach may reduce iteration count slightly if links are created depth-first rather than re-addressing provisional links
     */
    private static void createNodeAndMakeProvisionalLinks(PowerGrid instantiator, Key address, @Nullable ListTag links) {
        GridNode freshOrigin = instantiator.getOrCreateProvisional(address);
        if(freshOrigin.hasLinks() || links == null) return;
        for(int x = 0; x < links.size(); x++) {
            CompoundTag link = links.getCompound(x);
            Key provisionalLink = Key.loadFrom(link);
            GridNode provisionalTarget = instantiator.getOrCreateProvisional(provisionalLink);
            GridLink freshLink = new GridLink(freshOrigin, provisionalTarget, MechanoTransmissionTypes.REGISTRY.get(link));
            freshOrigin.links.add(freshLink);
        }
    }

    /**
     * Creates a new GlobalServerGrid with a predefined list of subgrids
     * @param world ServerLevel that owns this grid
     * @param subgrids Subgrids to instantiate the new GlobalServerGrid with
     */
    protected GlobalServerGrid(ServerLevel world, ObjectArrayList<PowerGrid> subgrids) {
        super(world);
        this.subgrids = subgrids;
    }

    /**
     * Finds a node at the given address by iterating through all subgrids.
     * @param address {@link com.quattage.mechano.foundation.api.landmarks.NodeIdentifier NodeIdentifier} to look for
     * @return The {@link GridNode} at the given address
     */
    public Pair<PowerGrid, GridNode> lookup(NodeIdentifiable<?> address) {
        for(int x = 0; x < subgrids.size(); x++) {
            PowerGrid grid = subgrids.get(x);
            GridNode get = grid.nodes.get(address);
            if(get != null) return Pair.of(grid, get);
        }
        return null;
    }

    /**
     * Create a link between any two {@link GridNode GridNodes} as long as their host
     * {@link PowerGridBlockEntity} instances exist in this GlobalServerGrid's level.
     * Calls to this method will perform additional operations where necessary to ensure
     * that the GridNodes at the provided {@link NodeIdentifialbe addresses} are mergable.
     * If nodes don't belong to the same {@link PowerGrid}, their PowerGrids are merged.
     * If nodes don't belong to ANY PowerGrid, a new grid will be made for them.
     * 
     * @param start Starting address 
     * @param end Ending address
     * @param type The type of link that will be created between <code>start</code> and <code>end</code>
     * @return A {@link LinkResponse} describing what happened
     * 
     * @throws NullPointerException if any provided field is null
     */
    public LinkResponse createLink(NodeIdentifiable<?> start, NodeIdentifiable<?> end, TransmitterType<?> type) {

        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(type);

        LevelReader world = getLevelReader();
        PowerGridBlockEntity startBE = start.getHost(world);
        PowerGridBlockEntity endBE = end.getHost(world);

        if(startBE == null) {
            Mechano.LOGGER.error("Failed to create link from " + start + " and " + end + " - No valid PGBE could be found at the starting address");
            return LinkResponse.FAIL_SYNC_OUTDATED;
        }

        if(endBE == null) {
            Mechano.LOGGER.error("Failed to create link from " + start + " and " + end + " - No valid PGBE could be found at the ending address");
            return LinkResponse.FAIL_SYNC_OUTDATED;
        }

        // nodes both belong to grids
        if(startBE.surrogate.isSynced() && endBE.surrogate.isSynced()) {

            PowerGrid startPG = startBE.surrogate.owner;
            PowerGrid endPG = endBE.surrogate.owner;

            // nodes both belong to the same grid
            if(startPG.gridIndex == endPG.gridIndex) {
                GridNode startNode = startPG.nodes.get(start);
                GridNode endNode = startPG.nodes.get(end);
                GridLink newLink = new GridLink(startNode, endNode, type.make());
                if(startNode.links.contains(newLink)) return LinkResponse.FAIL_DUPLICATE;
                startNode.links.add(newLink);
                endNode.links.add(newLink.inverseCopy());
                return LinkResponse.SUCCESS.andBailout();
            }

            // nodes both belong to different grids
            PowerGrid merged = mergeGrids(startPG.gridIndex, endPG.gridIndex);
            GridNode startNode = merged.nodes.get(start);
            GridNode endNode = merged.nodes.get(end);
            GridLink newLink = new GridLink(startNode, endNode, type.make());
            startNode.links.add(newLink);
            endNode.links.add(newLink.inverseCopy());
            return LinkResponse.SUCCESS.andBailout();
        }

        // start belongs to grid, but end doesn't
        if(startBE.surrogate.isSynced() && !endBE.surrogate.isSynced()) {
            PowerGrid preexisting = endBE.surrogate.getOwner();
            GridNode startNode = preexisting.nodes.get(start);
            GridNode endNode = new GridNode(preexisting, endBE, end.getIndex());
            GridLink newLink = new GridLink(startNode, endNode, type.make());
            startBE.surrogate.owner = preexisting;
            endBE.surrogate.owner = preexisting;
            startNode.links.add(newLink);
            endNode.links.add(newLink.inverseCopy());
            return LinkResponse.SUCCESS.andBailout();
        }

        // end belongs to grid, but start doesn't
        if(!startBE.surrogate.isSynced() && endBE.surrogate.isSynced()) {
            PowerGrid preexisting = endBE.surrogate.getOwner();
            GridNode startNode = new GridNode(preexisting, startBE, start.getIndex());
            GridNode endNode = preexisting.nodes.get(end);
            GridLink newLink = new GridLink(startNode, endNode, type.make());
            startBE.surrogate.owner = preexisting;
            endBE.surrogate.owner = preexisting;
            startNode.links.add(newLink);
            endNode.links.add(newLink.inverseCopy());
            return LinkResponse.SUCCESS.andBailout();
        }

        // neither belongs to grid
        if(!startBE.surrogate.isSynced() && !endBE.surrogate.isSynced()) {
            PowerGrid newGrid = new PowerGrid(this, 2);
            GridNode startNode = new GridNode(newGrid, startBE, start.getIndex());
            GridNode endNode = new GridNode(newGrid, endBE, end.getIndex());
            GridLink newLink = new GridLink(startNode, endNode, type.make());
            startBE.surrogate.owner = newGrid;
            endBE.surrogate.owner = newGrid;
            startNode.links.add(newLink);
            endNode.links.add(newLink.inverseCopy());
            subgrids.add(newGrid);
            newGrid.gridIndex = subgrids.size() - 1;
            return LinkResponse.SUCCESS.andBailout();
        }

        return LinkResponse.FAIL_GENERIC.andBailout();
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
     * Takes the contents from one {@link PowerGrid} in this GlobalServerGrid and 
     * merges it into the other. 
     * <p>
     * Nodes are merged by order of index, so a the contents of a grid with a HIGHER
     * index are merged onto a grid with a LOWER index. This also means that no new 
     * PowerGrid instances are created - the PowerGrid located at the lower of the 
     * two indices is recycled. 
     * <p>
     * Additionally, if the merge is successful, this call will create one stale 
     * PowerGrid instance that has been marked for removal. If this PowerGrid 
     * instance is stored anywhere, its reference should be nullified to prevent 
     * a leak. Only use the PowerGrid returned from this method.
     * 
     * 
     * @param index1 The index of the first PowerGrid to merge
     * @param index2 The index of the second PowerGrid to merge
     * @return A reference to the resulting PowerGrid. This reference will be 
     * identical to the PowerGrid found at the lower of the two provided indices.
     * @throws ArrayIndexOutOfBoundsException if the indices provided are outside the bounds of the subgrids list.
     */
    public PowerGrid mergeGrids(int index1, int index2) {

        if(index1 < 0 || index1 > subgrids.size() - 1)
            throw new ArrayIndexOutOfBoundsException("Failed to merge grids - Index1 '" + index1 + "' is out of bounds for a GlobalServerGrid with " + subgrids.size() + " subgrids!");
            if(index2 < 0 || index2 > subgrids.size() - 1)
            throw new ArrayIndexOutOfBoundsException("Failed to merge grids - Index1 '" + index2 + "' is out of bounds for a GlobalServerGrid with " + subgrids.size() + " subgrids!");

        if(index1 == index2) {
            return subgrids.get(index1);
        }

        // merge 1 onto 2
        if(index1 < index2) {
            PowerGrid grid1 = subgrids.get(index1);
            grid1.gridIndex = index1;
            PowerGrid grid2 = subgrids.remove(index2);
            grid1.addAll(grid2.nodes);
            grid2.destroy();
            return grid1;
        }

        // merge 2 onto 1
        if(index1 > index2) {
            PowerGrid grid2 = subgrids.get(index2);
            grid2.gridIndex = index2;
            PowerGrid grid1 = subgrids.remove(index1);
            grid2.addAll(grid1.nodes);
            grid1.destroy();
            return grid2;
        }

        throw new UnsupportedOperationException("...what? the fuck?");
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
     * acquired by the PowerGrid's {@link com.quattage.mechano.foundation.api.landmarks.NodeSet#write writing process}
     */
    @Override
    protected @Nullable ListTag writeAll() {
        ListTag output = new ListTag();
        for(PowerGrid grid : subgrids) {
            output.add(grid.nodes.write());
        }
        return output;
    }

    @Override
    protected String getDistPrefix() {
        return "SERVER";
    }

    @Override
    public String toString() {
        return "GlobalServerGrid(" + getDimensionName() + ", " + subgrids.size() + " members)";
    }
}
