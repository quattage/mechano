package com.quattage.mechano.foundation.electricity.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GID;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridClientEdge;
import com.quattage.mechano.foundation.electricity.grid.landmarks.GridVertex;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncHelper;
import com.quattage.mechano.foundation.electricity.grid.network.GridSyncPacketType;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

/***
 * The GlobalTransferGrid is an elevated level controller for LocalTransferGrids.
 * It offers functionality to intelligently manage a list of networks, where
 * individual networks are stored as subsystems which can be added to, removed from,
 * split, merged, repaired, etc.
 */
public class GlobalTransferGrid {

    private final ArrayList<LocalTransferGrid> subgrids = new ArrayList<LocalTransferGrid>();
    private final Level world;

    public GlobalTransferGrid(Level world) {
        if(world == null)
            throw new NullPointerException("Error instantiating new GlobalTransferGrid - World cannot be null!");
        if(world.isClientSide)
            throw new IllegalStateException("Error instantiating new GlobalTransferGrid - GlobalTransferGrid cannot be registered on the client!");
        this.world = world;
    }

    public static GlobalTransferGrid of(Level world) {
        if(world == null) throw new NullPointerException("Error getting GlobalTransferGrid - World is null!");
        if(world.isClientSide()) return null;
        LazyOptional<GlobalTransferGrid> network = world.getCapability(Mechano.CAPABILITIES.SERVER_GRID_CAPABILITY);
        if(!network.isPresent()) throw new RuntimeException("Error getting GlobalTransferGrid from " + world.dimension().location() 
            + " - No handler registered for this dimension!");
        GlobalTransferGrid realNetwork = network.orElseThrow(RuntimeException::new);
        return realNetwork;
    }

    protected void readFrom(CompoundTag in) {
        CompoundTag net = in.getCompound(getDimensionName());

        ListTag subs = net.getList("all", Tag.TAG_COMPOUND);
        Mechano.LOGGER.warn("Global network in [" + getDimensionName() + "] is reading " + subs.size() + " TransferNetworks from NBT");

        for(int x = 0; x < subs.size(); x++) {
            CompoundTag subsystem = subs.getCompound(x);
            Mechano.LOGGER.info("Adding a LocalTransferGrid containing the following data:\n" + subsystem);
            LocalTransferGrid sysToAdd = new LocalTransferGrid(this, subsystem, world);
            if(!sysToAdd.isEmpty()) subgrids.add(sysToAdd); 
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        out.put("all", writeAllSubsystems());
        in.put(getDimensionName(), out);
        return in;
    }

    private ListTag writeAllSubsystems() {
        ListTag out = new ListTag();
        for(LocalTransferGrid grid : subgrids)
            out.add(grid.writeTo(new CompoundTag()));
        return out;
    }

    public String getDimensionName() { 
        if(world == null) return "NONE";
        return world.dimension().location().toString();
    }

    public int getSubsystemCount() {
        return subgrids.size();
    }

    /***
     * Links two GridVerticies without question. If these vertices don't belong to an active grid,
     * a new grid is made from them. If both vertices are in independent subgrids, these
     * subgrids are merged.
     * @param idA
     * @param idB
     */
    public LinkResult link(Entity linker, GID idA, GID idB, int typeID) {
        Pair<Integer, LocalTransferGrid> sysA = getSystemContaining(idA);
        Pair<Integer, LocalTransferGrid> sysB = getSystemContaining(idB);

        if(idA.equals(idB)) return LinkResult.GENERIC;
        //if(getLinkBetween(idA, idB) != null) return AnchorInteractType.LINK_EXISTS;

        BlockEntity beA = world.getBlockEntity(idA.getBlockPos());
        if(!(beA instanceof WireAnchorBlockEntity wbeA)) return LinkResult.GENERIC;
        BlockEntity beB = world.getBlockEntity(idB.getBlockPos());
        if(!(beB instanceof WireAnchorBlockEntity wbeB)) return LinkResult.GENERIC;

        if(sysA == null && sysB == null) {
            LocalTransferGrid newSystem = new LocalTransferGrid(this);

            GridVertex vA = new GridVertex(wbeA, newSystem, idA);
            GridVertex vB = new GridVertex(wbeB, newSystem, idB);

            newSystem.addVert(vA);
            newSystem.addVert(vB);
            newSystem.linkVerts(vA, vB, typeID, true);
            subgrids.add(newSystem);

        } else if(sysA != null && sysB == null) {
            
            sysA.getSecond().addVert(new GridVertex(wbeB, sysA.getSecond(), idB));
            sysA.getSecond().linkVerts(idA, idB, typeID, true);

        } else if(sysA == null && sysB != null) {
            sysB.getSecond().addVert(new GridVertex(wbeA, sysB.getSecond(), idA));
            sysB.getSecond().linkVerts(idA, idB, typeID, true);

        } else if(sysA.getFirst() == sysB.getFirst()) {
            if(!sysA.getSecond().linkVerts(idA, idB, typeID, true))
                return LinkResult.ALREADY_EXISTS;

        } else if(sysA.getFirst() != sysB.getFirst()) {

            subgrids.remove((int)sysA.getFirst());
            subgrids.remove(sysB.getSecond());
            LocalTransferGrid merged = LocalTransferGrid.ofMerged(this, true, sysA.getSecond(), sysB.getSecond());
            merged.linkVerts(idA, idB, typeID, false);
            merged.findAllPaths(true); 

            subgrids.add(merged);
        }

        String entity = linker instanceof Player p ? p.getName().getString() : linker.toString();
        Mechano.LOGGER.info("Link (" + idA + " -> " + idB + ") established by player '" + entity + "'' in GlobalTransferGrid(" + getDimensionName() + ")");
        GridSyncHelper.informPlayerEdgeUpdate(GridSyncPacketType.ADD_NEW, new GridClientEdge(idA, idB, typeID));
        return LinkResult.SUCCESS;
    }

    /***
     * Removes the link between vertices at two given positions as long as they exist.
     * @throws NullPointerException if no node could be found at either given BlockPos
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(GID linkOne, GID linkTwo, boolean clean) {
        GridVertex vertOne = getVertAt(linkOne);
        GridVertex vertTwo = getVertAt(linkTwo);
        if(vertOne == null) throw new NullPointerException("Failed to unlink GridVertex from a global context - " + 
            "No valid GridVertex at " + linkOne + " could be found! (first provided parameter)");

        if(vertTwo == null) throw new NullPointerException("Failed to unlink GridVertex from a global context - " + 
            "No valid GridVertex at " + linkTwo + " could be found! (second provided parameter)");

        vertOne.popLink(vertTwo);
        vertTwo.popLink(vertOne);

        if(clean) declusterize();

    }

    public void findAndDestroyVertex(GID id, boolean shouldClean) {
        for(LocalTransferGrid grid : subgrids) {
            GridVertex vert = grid.popVert(id);
            if(vert != null) {
                declusterize(vert.getOrFindParent());
                return;
            }
        }
    }

    public void destroyVertex(GridVertex vert, boolean shouldClean) {
        if((vert.getOrFindParent().popVert(vert.getID()) != null) && shouldClean)
            declusterize(vert.getOrFindParent());
    }

    /***
     * Splits LocalTransferGrids by their discontinuities.
     * Called automatically whenever a node is removed
     * from the system.
     */
    private void declusterize() {
        ArrayList<LocalTransferGrid> evaluated = new ArrayList<>();
        for(LocalTransferGrid grid : subgrids) 
            evaluated.addAll(grid.trySplit());
        subgrids.clear();
        subgrids.addAll(evaluated);
    } 

    /***
     * Splits a given LocalTransferGrid by its discontinuities.
     * @param grid Grid to split
     */
    private void declusterize(LocalTransferGrid grid) {
        ArrayList<LocalTransferGrid> evaluated = new ArrayList<>();
        evaluated.addAll(grid.trySplit());
        subgrids.remove(grid);
        subgrids.addAll(evaluated);
    }

    /***
     * Gets the LocalTransferGrid that contains the given id, or null of one does not exist.
     * @param id
     * @return A pair, where the first member is the index of the LocalTransferGrid,
     * and the second member is the LocalTransferGrid itself
     */
    public Pair<Integer, LocalTransferGrid> getSystemContaining(GID id) {
        int x = 0;
        for(LocalTransferGrid grid : subgrids) {
            if(grid.getVertAt(id) != null) return Pair.of(x, grid);
            x++;
        }
        return null;
    }

    public GridVertex getVertAt(GID id) {
        for(LocalTransferGrid grid : subgrids) {
            GridVertex node = grid.getVertAt(id);
            if(node != null) return node;
        }
        return null;
    }

    /***
     * @return True if the vertex at the given GID is both present and has available connections. A Vertex is
     * assumed available if it does not currently exist.
     */
    public boolean isVertAvailable(GID id) {
        Pair<AnchorPoint, WireAnchorBlockEntity> anchorPair = AnchorPoint.getAnchorAt(world, id);
        if(anchorPair == null) return false;
        AnchorPoint anchor = anchorPair.getFirst();
        if(anchor == null) return false;

        GridVertex vert = anchorPair.getFirst().getParticipant();
        if(vert == null) vert = getVertAt(id);
        if(vert == null) return true;
        
        if(anchor.getMaxConnections() > vert.links.size()) return true;
        return false;
    }

    public Level getWorld() {
        return world;
    }


    //////////////////////////////////////////// debugging helpers ////////////////////////////////////////////

    public String toString() {
        String head = "[";
        if(subgrids.isEmpty()) return "[EMPTY]";

        String systems = "";
        for(int x = 0; x < subgrids.size(); x++) {
            systems += "System " + x + ": \n" + subgrids.get(x);
            if(x != subgrids.size() - 1)
                systems += "\t\n";
        }

        return head + "\n" + systems + "]";
    }

    /**
     * @return A Set containing every unique BlockPos addressed by this GlobalTransferGrid
     */
    public Set<BlockPos> collectAllVertexPositions() {
        Set<BlockPos> out = new HashSet<>();
        for(LocalTransferGrid subgrid : subgrids) {
            for(GridVertex vert : subgrid.allVerts())
                out.add(vert.getID().getBlockPos());
        }

        return out;
    }

    /**
     * Removes every GridVertex in this GlobalTranferGrid that belongs 
     * to the provided BlockPos
     * @param pos
     * @return The number of GridVertex objects that were removed
     */
    public int removeAllVertsAt(BlockPos pos) {
        int removed = 0;
        for(LocalTransferGrid subgrid : subgrids) {
            Iterator<GridVertex> matrixIterator = subgrid.allVerts().iterator();
            while(matrixIterator.hasNext()) {
                GridVertex vert = matrixIterator.next();
                if(vert.getID().getBlockPos().equals(pos)) {
                    vert.markRemoved();
                    matrixIterator.remove();
                    removed++;
                }
            }
            declusterize(subgrid);
        }
        
        return removed;
    }

    public void clear() {
        subgrids.clear();
    }

    public ArrayList<LocalTransferGrid> getSubgrids() {
        return subgrids;
    }
}