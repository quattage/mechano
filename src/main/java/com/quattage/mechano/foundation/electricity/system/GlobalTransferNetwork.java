package com.quattage.mechano.foundation.electricity.system;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.WireAnchorBlockEntity;
import com.quattage.mechano.foundation.electricity.core.DirectionalEnergyStorable;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.core.anchor.interaction.AnchorInteractType;
import com.quattage.mechano.foundation.electricity.system.edge.SystemEdge;
import com.simibubi.create.foundation.utility.Pair;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.LazyOptional;

/***
 * The GlobalTransferNetwork is an elevated level con++troller for TransferSystems.
 * It offers functionality to intelligently manage a list of networks, where
 * individual networks are stored as subsystems which can be added to, removed from,
 * split, merged, repaired, etc.
 */
public class GlobalTransferNetwork {

    private final ArrayList<TransferSystem> subsystems = new ArrayList<TransferSystem>();
    private final Object2ObjectOpenHashMap<SectionPos, List<SystemEdge>> clientRenderQueue;
    private final Level world;

    public GlobalTransferNetwork(Level world) {
        if(world == null)
            throw new NullPointerException("Error instantiating new GlobalTransferNetwork - World cannot be null!");

        this.world = world;

        if(isClient())
            clientRenderQueue = new Object2ObjectOpenHashMap<>();
        else
            clientRenderQueue = null;
    }

    public static GlobalTransferNetwork get(Level world) {
        if(world == null) throw new NullPointerException("Error getting GlobalTransferNetwork - World is null!");
        LazyOptional<GlobalTransferNetwork> network = world.getCapability(Mechano.NETWORK_CAPABILITY);
        if(!network.isPresent()) throw new RuntimeException("Error getting GlobalTransferNetwork from " + world.dimension().location() 
            + " - No handler registered for this dimension!");
        GlobalTransferNetwork realNetwork = network.orElseThrow(RuntimeException::new);
        return realNetwork;
    }

    public void readFrom(CompoundTag in) {
        CompoundTag net = in.getCompound(getDimensionName());

        ListTag subs = net.getList("all", Tag.TAG_COMPOUND);
        Mechano.LOGGER.warn("Global network in [" + getDimensionName() + "] is reading " + subs.size() + " TransferNetworks from NBT");

        boolean hadFailures = false;
        for(int x = 0; x < subs.size(); x++) {
            CompoundTag subsystem = subs.getCompound(x);
            Mechano.LOGGER.info("Adding a TransferNetwork containing the following data:\n" + subsystem);
            TransferSystem sysToAdd = new TransferSystem(this, subsystem, world);

            // systems may be missing members due to failed registration so deal with them conditionally
            if(sysToAdd.size() != subsystem.getList("sub", Tag.TAG_COMPOUND).size()) {
                subsystems.add(sysToAdd);
                hadFailures = true; 
            }
            else if(sysToAdd.isEmpty()) 
                hadFailures = true; 
            else 
                subsystems.add(sysToAdd); 
        }
        if(hadFailures) {
            Mechano.LOGGER.warn("GlobalTransferNetwork [" + getDimensionName() + "] detected that 1 or more TransferSystems failed to register or registered with missing components! " + 
                "Affected blocks' coordinates should be above this message. If you've recently experienced a crash, this might be why.");
            declusterize();
            onSystemModified();
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

    public Object2ObjectOpenHashMap<SectionPos, List<SystemEdge>> getClientRenderQueue() {
        return clientRenderQueue;
    }

    public List<SystemEdge> getEdgesWithin(SectionPos section) {
        synchronized(clientRenderQueue) {
            if(clientRenderQueue == null) throw new IllegalStateException("Error accessing clientRenderQueue - Can only be accessed on the client! (clientRenderQueue is null)");
            List<SystemEdge> edges = clientRenderQueue.get(section);
            if(edges == null) return null;
            return List.copyOf(edges);
        }
    }

    /***
     * Called whenever the status of a BlockEntity host has changes that should be reflected.
     */
    public void refreshVertex(SystemVertex vert) {
        if(!hasWorld()) return;            

        BlockEntity be = world.getBlockEntity(vert.getPos());
        BlockState state = world.getBlockState(vert.getPos());
        Direction dir = DirectionTransformer.getUp(state);

        if(be instanceof WireAnchorBlockEntity wbe) {
            Mechano.log("Vertex refreshed at " + vert.getPos());
            if(DirectionalEnergyStorable.hasMatchingCaps(world, vert.getPos(), dir)) {
                vert.setIsMember();
                // other such goofy shit here

                // TODO the goofy shit
            }
        }
    }

    /***
     * Called whenever the status of a BlockEntity host has changes that should be reflected.
     */
    public void refreshVertex(BlockPos pos) {
        SystemVertex vert = getVertAt(new SVID(pos));
        refreshVertex(vert);
    }

    public boolean hasWorld() {
        return world != null;
    }

    public ListTag writeAllSubsystems() {
        ListTag out = new ListTag();
        for(TransferSystem sys : subsystems)
            out.add(sys.writeTo(new CompoundTag()));
        return out;
    }

    public int getSubsystemCount() {
        return subsystems.size();
    }

    public ArrayList<TransferSystem> all() {
        return subsystems;
    }

    /***
     * Links two SystemVerticies without question. If these nodes don't belong to a subsystem,
     * a new subsystem is made from them. If both nodes are in independent subsystems, these
     * subsystems are merged.
     * <p>
     * <strong>Merges are assumed to be From -> To,</strong> or idA -> idB. Only idB is checked for specific 
     * in-world things, since idA should be checked <strong>before this is called.</strong>
     * @param idA
     * @param idB
     */
    public AnchorInteractType link(SVID idA, SVID idB) {
        Pair<Integer, TransferSystem> sysA = getSystemContaining(idA);
        Pair<Integer, TransferSystem> sysB = getSystemContaining(idB);

        if(doesLinkExist(idA, idB)) return AnchorInteractType.LINK_EXISTS;

        if(sysA == null && sysB == null) {
            TransferSystem newSystem = new TransferSystem(this);
            newSystem.addVert(new SystemVertex(this, idA.getPos(), idA.getSubIndex()));
            newSystem.addVert(new SystemVertex(this, idB.getPos(), idB.getSubIndex()));
            newSystem.linkVerts(idA, idB);
            subsystems.add(newSystem);

        } else if(sysA != null && sysB == null) {
            sysA.getSecond().addVert(idB);
            sysA.getSecond().linkVerts(idA, idB);

        } else if(sysA == null && sysB != null) {
            sysB.getSecond().addVert(idA);
            sysB.getSecond().linkVerts(idA, idB);

        } else if(sysA.getFirst() == sysB.getFirst()) {
            sysA.getSecond().linkVerts(idA, idB);

        } else if(sysA.getFirst() != sysB.getFirst()) {
            // comparison here ensures that systems get merged down rather than up
            // so we don't end up with null systems in the array
            if(sysA.getFirst() < sysB.getFirst()) {
                sysA.getSecond().mergeWith(sysB.getSecond());
                subsystems.remove(sysB.getSecond());
                sysA.getSecond().linkVerts(idA, idB);
            } else {
                sysB.getSecond().mergeWith(sysA.getSecond());
                subsystems.remove(sysA.getSecond());
                sysB.getSecond().linkVerts(idA, idB);
            }

            
        }
        
        onSystemModified(idA);
        return AnchorInteractType.LINK_ADDED;
    }

    /***
     * called whenever a system is modified from a global context 
     * (discrete modifications on the individual level won't call this)
     */
    public void onSystemModified() {
        // NetworkSavedData.markInstanceDirty();
    }

    /***
     * called whenever a system is modified from a global context 
     * (discrete modifications on the individual level won't call this)
     * @param id origin of the modification
     */
    public void onSystemModified(SVID id) {
        // NetworkSavedData.markInstanceDirty(id);
    }

    /***
     * Removes the link between nodes at two given positions as long as they exist.
     * @throws NullPointerException if no node could be found at either given BlockPos
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(SVID linkOne, SVID linkTwo, boolean clean) {
        SystemVertex nodeOne = getVertAt(linkOne);
        SystemVertex nodeTwo = getVertAt(linkTwo);
        if(nodeOne == null) throw new NullPointerException("Failed to unlink SystemVertex from a global context - " + 
            "No valid SystemVertex at " + linkOne + " could be found! (first provided parameter)");

        if(nodeTwo == null) throw new NullPointerException("Failed to unlink SystemVertex from a global context - " + 
            "No valid SystemVertex at " + linkTwo + " could be found! (second provided parameter)");

        nodeOne.unlinkFrom(nodeTwo);
        nodeTwo.unlinkFrom(nodeOne);
        if(clean)
            declusterize();
        onSystemModified();
    }

    /***
     * Removes the link between nodes at two given positions as long as they exist.
     * @throws NullPointerException if no node could be found at either given BlockPos
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */ 
    public void unlink(SVID linkOne, SVID linkTwo) {
        unlink(linkOne, linkTwo, true);
    }

    /***
     * Removes the link between two provided SystemVerticies.
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operationtt.
     */
    public void unlink(SystemVertex vertOne, SystemVertex vertTwo, boolean clean) {
        vertOne.unlinkFrom(vertTwo);
        vertTwo.unlinkFrom(vertOne);
        if(clean)
            declusterize();
        onSystemModified();
    }

    /***
     * Removes the link between two provided SystemVerticies.
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(SystemVertex vertOne, SystemVertex vertTwo) {
        unlink(vertOne, vertTwo, true);
    }

    /***
     * Destroys all instances of this vertex in all child systems. 
     * @param vert SystemVertex to remove
     */
    public void destroyVertex(SystemVertex vert) {
        boolean modified = false;
        for(TransferSystem sys : subsystems) {
            if(sys.contains(vert))
                modified = sys.destroyVert(vert);
        }
        if(modified) {
            declusterize();
            onSystemModified();
        }
    }

    /***
     * Destroys all verticies at the provided SVID in all child systems. 
     * @param id SVID to remove
     */
    public void destroyVertex(SVID id) {
        boolean modified = false;
        for(TransferSystem sys : subsystems) {
            if(sys.contains(id)) 
                modified = sys.destroyVertsAt(id);
        }
        if(modified) {
            declusterize();
            onSystemModified();
        }
    }

    /***
     * Splits TransferSystems by their discontinuities.
     * Called automatically whenever a node is removed
     * from the system.
     */
    public void declusterize() {
        ArrayList<TransferSystem> evaluated = new ArrayList<>();
        for(TransferSystem sys : subsystems) 
            evaluated.addAll(sys.trySplit());
        subsystems.clear();
        subsystems.addAll(evaluated);
    } 
    // TODO Directed DFS for better optimization 
    // store the index of the parent TransferSystem in each vertex so we don't have to loop over all systems
    // also, if the vertex has only one connection, it won't generate any discontinuities when broken, no DFS requried.

    public Pair<Integer, TransferSystem> getSystemContaining(SVID id) {
        int x = 0;
        for(TransferSystem sys : subsystems) {
            if(sys.getNode(id) != null) return Pair.of(x, sys);
            x++;
        }
        return null;
    }

    /***
     * Gets the SystemVertex at this SVID.
     * @param pos
     * @return Returns the SystemVertex at this SVID, or null if none exists.
     */
    public SystemVertex getVertAt(SVID id) {
        for(TransferSystem sys : subsystems) {
            SystemVertex node = sys.getNode(id);
            if(node != null) return node;
        }
        return null;
    }

    public boolean doesLinkExist(SVID idA, SVID idB) {
        if(idA == null || idB == null) return false;

        SystemVertex vA = getVertAt(idA);
        if(vA == null) return false;

        SystemVertex vB = getVertAt(idB);
        if(vB == null) return false;

        if(vA.isLinkedTo(vB)) return true;
        return false;
    }

    public boolean doesVertExist(SystemVertex vert) {
        for(TransferSystem sys : subsystems) {
            for(SystemVertex vertCompare : sys.allVerts())
                if(vertCompare.equals(vert)) return true;
        }
        return false;
    }

    /***
     * Gets the SystemVertex at this BlockPos. If one does not exist,
     * a new SystemVertex will be created there instead.
     * @param link SVID to find the vertex or make one at
     * @return Returns the pre-existing or new SystemVertex at the given BlockPos
     */
    public SystemVertex getOrCreateVertAt(SVID link) {
        for(TransferSystem sys : subsystems) {
            SystemVertex node = sys.getNode(link);
            if(node != null) return node;
        }
        return new SystemVertex(this, link.getPos(), link.getSubIndex());
    }

    /***
     * @return True if the vertex at the given SVID is both present and has available connections. A Vertex is
     * assumed available if it does not currently exist.
     */
    public boolean isVertAvailable(SVID id) {
        Pair<AnchorPoint, WireAnchorBlockEntity> anchorPair = AnchorPoint.getAnchorAt(world, id);
        if(anchorPair == null) return false;

        AnchorPoint anchor = anchorPair.getFirst();
        if(anchor == null) return false;
        SystemVertex vert = getVertAt(id);
        if(vert == null) return true;

        Mechano.log(anchor.getMaxConnections() + " > " + vert.links.size());
        if(anchor.getMaxConnections() > vert.links.size()) return true;
        return false;
    }

    public Iterator<TransferSystem> getSubsystemsIterator() {
        return subsystems.iterator();
    }

    /***
     * Gets the node and the TransferSystem it belongs to based on a provided SVID
     */
    public Pair<TransferSystem, SystemVertex> getSystemAndNode(SVID id) {
        for(TransferSystem sys : subsystems) {
            SystemVertex node = sys.getNode(id);
            if(node != null) return Pair.of(sys, node);
        }
        return null;
    }

    // at the moment IDs are just indices
    public int getSubsystemID(TransferSystem system) {
        return subsystems.indexOf(system);
    }

    public boolean isIDValid(int id) {
        return -1 < id && id < subsystems.size();
    }

    public boolean isClient() {
        return world.isClientSide();
    }

    public Level getWorld() {
        return world;
    }

    public String toString() {
        String head = "[";
        if(subsystems.isEmpty()) return "[EMPTY]";

        String systems = "";
        for(int x = 0; x < subsystems.size(); x++) {
            systems += "System " + x + ": \n" + subsystems.get(x);
            if(x != subsystems.size() - 1)
                systems += "\t\n";
        }

        return head + "\n" + systems + "]";
    }
}