package com.quattage.mechano.foundation.electricity.power;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.anchor.interaction.AnchorInteractType;
import com.quattage.mechano.foundation.electricity.power.features.GID;
import com.quattage.mechano.foundation.electricity.power.features.GridVertex;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

    public static GlobalTransferGrid get(Level world) {
        if(world == null) throw new NullPointerException("Error getting GlobalTransferGrid - World is null!");
        if(world.isClientSide()) return null;
        LazyOptional<GlobalTransferGrid> network = world.getCapability(Mechano.NETWORK_CAPABILITY);
        if(!network.isPresent()) throw new RuntimeException("Error getting GlobalTransferGrid from " + world.dimension().location() 
            + " - No handler registered for this dimension!");
        GlobalTransferGrid realNetwork = network.orElseThrow(RuntimeException::new);
        return realNetwork;
    }

    public void readFrom(CompoundTag in) {
        CompoundTag net = in.getCompound(getDimensionName());

        ListTag subs = net.getList("all", Tag.TAG_COMPOUND);
        Mechano.LOGGER.warn("Global network in [" + getDimensionName() + "] is reading " + subs.size() + " TransferNetworks from NBT");

        boolean hadFailures = false;
        for(int x = 0; x < subs.size(); x++) {
            CompoundTag subsystem = subs.getCompound(x);
            Mechano.LOGGER.info("Adding a LocalTransferGrid containing the following data:\n" + subsystem);
            LocalTransferGrid sysToAdd = new LocalTransferGrid(this, subsystem, world);

            // systems may be missing members due to failed registration so deal with them conditionally
            if(sysToAdd.size() != subsystem.getList("nt", Tag.TAG_COMPOUND).size()) {
                subgrids.add(sysToAdd);
                hadFailures = true; 
            }
            else if(sysToAdd.isEmpty()) 
                hadFailures = true; 
            else 
                subgrids.add(sysToAdd); 
        }
        if(hadFailures) {
            Mechano.LOGGER.warn("GlobalTransferGrid [" + getDimensionName() + "] detected that 1 or more LocalTransferGrids failed to register or registered with missing components! " + 
                "Affected blocks' coordinates should be above this message. If you've recently experienced a crash, this might be why.");
            declusterize();
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        out.put("all", writeAllSubsystems());
        in.put(getDimensionName(), out);
        return in;
    }

    public String getDimensionName() { 
        if(world == null) return "NONE";
        return world.dimension().location().toString();
    }

    public ListTag writeAllSubsystems() {
        ListTag out = new ListTag();
        for(LocalTransferGrid sys : subgrids)
            out.add(sys.writeTo(new CompoundTag()));
        return out;
    }

    public int getSubsystemCount() {
        return subgrids.size();
    }

    public ArrayList<LocalTransferGrid> all() {
        return subgrids;
    }

    /***
     * Links two GridVerticies without question. If these vertices don't belong to a subsystem,
     * a new subsystem is made from them. If both vertices are in independent subsystems, these
     * subsystems are merged.
     * @param idA
     * @param idB
     */
    public AnchorInteractType link(GID idA, GID idB, int wireType) {
        Pair<Integer, LocalTransferGrid> sysA = getSystemContaining(idA);
        Pair<Integer, LocalTransferGrid> sysB = getSystemContaining(idB);

        if(idA.equals(idB)) return AnchorInteractType.GENERIC;
        if(doesLinkExist(idA, idB)) return AnchorInteractType.LINK_EXISTS;

        BlockEntity beA = world.getBlockEntity(idA.getPos());
        if(!(beA instanceof WireAnchorBlockEntity wbeA)) return AnchorInteractType.GENERIC;
        BlockEntity beB = world.getBlockEntity(idB.getPos());
        if(!(beB instanceof WireAnchorBlockEntity wbeB)) return AnchorInteractType.GENERIC;

        if(sysA == null && sysB == null) {
            LocalTransferGrid newSystem = new LocalTransferGrid(this);

            newSystem.addVert(new GridVertex(wbeA, newSystem, idA.getPos(), idA.getSubIndex()));
            newSystem.addVert(new GridVertex(wbeB, newSystem, idB.getPos(), idB.getSubIndex()));
            newSystem.linkVerts(idA, idB, wireType);
            subgrids.add(newSystem);

        } else if(sysA != null && sysB == null) {
            sysA.getSecond().addVert(new GridVertex(wbeB, sysA.getSecond(), idB.getPos(), idB.getSubIndex()));
            sysA.getSecond().linkVerts(idA, idB, wireType);

        } else if(sysA == null && sysB != null) {
            sysB.getSecond().addVert(new GridVertex(wbeA, sysB.getSecond(), idA.getPos(), idA.getSubIndex()));
            sysB.getSecond().linkVerts(idA, idB, wireType);

        } else if(sysA.getFirst() == sysB.getFirst()) {
            sysA.getSecond().linkVerts(idA, idB, wireType);

        } else if(sysA.getFirst() != sysB.getFirst()) {
            // comparison here ensures that grids get merged down rather than up
            // so we don't end up with null grids in the array
            if(sysA.getFirst() < sysB.getFirst()) {
                sysA.getSecond().mergeWith(sysB.getSecond());
                subgrids.remove(sysB.getSecond());
                sysA.getSecond().linkVerts(idA, idB, wireType);
            } else {
                sysB.getSecond().mergeWith(sysA.getSecond());
                subgrids.remove(sysA.getSecond());
                sysB.getSecond().linkVerts(idA, idB, wireType);
            }
        }



        return AnchorInteractType.LINK_ADDED;
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
        GridVertex nodeOne = getVertAt(linkOne);
        GridVertex nodeTwo = getVertAt(linkTwo);
        if(nodeOne == null) throw new NullPointerException("Failed to unlink GridVertex from a global context - " + 
            "No valid GridVertex at " + linkOne + " could be found! (first provided parameter)");

        if(nodeTwo == null) throw new NullPointerException("Failed to unlink GridVertex from a global context - " + 
            "No valid GridVertex at " + linkTwo + " could be found! (second provided parameter)");

        nodeOne.unlinkFrom(nodeTwo);
        nodeTwo.unlinkFrom(nodeOne);

        if(clean)
            declusterize();

    }

    /***
     * Removes the link between vertices at two given positions as long as they exist.
     * @throws NullPointerException if no node could be found at either given BlockPos
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */ 
    public void unlink(GID linkOne, GID linkTwo) {
        unlink(linkOne, linkTwo, true);
    }

    /***
     * Removes the link between two provided GridVerticies.
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operationtt.
     */
    public void unlink(GridVertex vertOne, GridVertex vertTwo, boolean clean) {
        vertOne.unlinkFrom(vertTwo);
        vertTwo.unlinkFrom(vertOne);
        if(clean)
            declusterize();
    }

    /***
     * Removes the link between two provided GridVerticies.
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(GridVertex vertOne, GridVertex vertTwo) {
        unlink(vertOne, vertTwo, true);
    }

    public void destroyVertex(GID id) {
        boolean modified = false;
        for(LocalTransferGrid sys : subgrids) {
            if(sys.contains(id)) 
                modified = sys.destroyVertsAt(id);
        }
        if(modified) {
            declusterize();
        }
    }

    /***
     * Splits LocalTransferGrids by their discontinuities.
     * Called automatically whenever a node is removed
     * from the system.
     */
    public void declusterize() {
        ArrayList<LocalTransferGrid> evaluated = new ArrayList<>();
        for(LocalTransferGrid sys : subgrids) 
            evaluated.addAll(sys.trySplit());
        subgrids.clear();
        subgrids.addAll(evaluated);
    } 


    // TODO Directed DFS for better optimization 
    // store the index of the parent LocalTransferGrid in each vertex so we don't have to loop over all systems
    // also, if the vertex has only one connection, it won't generate any discontinuities when broken, no DFS requried.

    public Pair<Integer, LocalTransferGrid> getSystemContaining(GID id) {
        int x = 0;
        for(LocalTransferGrid sys : subgrids) {
            if(sys.getNode(id) != null) return Pair.of(x, sys);
            x++;
        }
        return null;
    }

    /***
     * Gets the GridVertex at this GID.
     * @param pos
     * @return Returns the GridVertex at this GID, or null if none exists.
     */
    public GridVertex getVertAt(GID id) {
        for(LocalTransferGrid sys : subgrids) {
            GridVertex node = sys.getNode(id);
            if(node != null) return node;
        }
        return null;
    }

    public List<GridVertex> getAllVertsAt(BlockPos pos) {
        ArrayList<GridVertex> out = new ArrayList<GridVertex>();
        for(LocalTransferGrid sys : subgrids)
            out.addAll(sys.getAllNodesAt(pos));
        return out;
    }

    public boolean doesLinkExist(GID idA, GID idB) {
        if(idA == null || idB == null) return false;

        GridVertex vA = getVertAt(idA);
        if(vA == null) return false;

        GridVertex vB = getVertAt(idB);
        if(vB == null) return false;

        if(vA.isLinkedTo(vB)) return true;
        return false;
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
        GridVertex vert = getVertAt(id);
        if(vert == null) return true;
        if(anchor.getMaxConnections() > vert.links.size()) return true;
        return false;
    }

    public ArrayList<LocalTransferGrid> getSubgrids() {
        return subgrids;
    }

    public Iterator<LocalTransferGrid> getSubsystemsIterator() {
        return subgrids.iterator();
    }

    /***
     * Gets the node and the LocalTransferGrid it belongs to based on a provided GID
     */
    public Pair<LocalTransferGrid, GridVertex> getSystemAndNode(GID id) {
        for(LocalTransferGrid sys : subgrids) {
            GridVertex node = sys.getNode(id);
            if(node != null) return Pair.of(sys, node);
        }
        return null;
    }

    public int getSubsystemID(LocalTransferGrid system) {
        return subgrids.indexOf(system);
    }

    public boolean isIDValid(int id) {
        return -1 < id && id < subgrids.size();
    }

    public boolean isClient() {
        return world.isClientSide();
    }

    public Level getWorld() {
        return world;
    }

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
}