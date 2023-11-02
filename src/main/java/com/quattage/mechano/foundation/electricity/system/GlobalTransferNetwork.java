package com.quattage.mechano.foundation.electricity.system;

import java.util.ArrayList;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.electricity.WireNodeBlockEntity;
import com.quattage.mechano.foundation.electricity.core.DirectionalEnergyStorable;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/***
 * The GlobalTransferNetwork is an elevated level controller for TransferSystems.
 * It offers functionality to intelligently manage a list of networks, where
 * individual networks are stored as subsystems which can be added to, removed from,
 * split, merged, repaired, etc.
 */
public class GlobalTransferNetwork {
    
    private ArrayList<TransferSystem> subsystems = new ArrayList<TransferSystem>();
    private ServerLevel world = null;

    public static final GlobalTransferNetwork NETWORK = new GlobalTransferNetwork();

    public GlobalTransferNetwork() {}

    private void debug(String operation) {
        Mechano.log(operation + ": \n" + NETWORK);
    }

    public void readFrom(CompoundTag in, ServerLevel world) {
        CompoundTag net = in.getCompound(NetworkSavedData.MECHANO_NETWORK_KEY);

        if(NetworkSavedData.SAVE_VERSION != net.getInt("ver")) {
            Mechano.LOGGER.warn("Unable to serialize GlobalTransferNetwork from disk - saved copy was marked as depricated!");
            return;
        }

        ListTag subs = net.getList("all", Tag.TAG_COMPOUND);
        Mechano.LOGGER.warn("Reading " + subs.size() + " TransferNetworks from NBT");

        boolean hadFailures = false;
        for(int x = 0; x < subs.size(); x++) {
            CompoundTag subsystem = subs.getCompound(x);
            Mechano.LOGGER.info("Adding a TransferNetwork containing the following data:\n" + subsystem);
            TransferSystem sysToAdd = new TransferSystem(subsystem, world);

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
            Mechano.LOGGER.warn("The GlobalTransferNetwork detected that 1 or more TransferSystems failed to register or registered with missing components! " + 
                "Affected blocks' coordinates should be above this message. If you've recently experienced a crash, this might be why.");
            declusterize();
            onSystemModified();
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        out.putInt("ver", NetworkSavedData.SAVE_VERSION);
        out.put("all", writeAllSubsystems());
        in.put(NetworkSavedData.MECHANO_NETWORK_KEY, out);
        return in;
    }

    /***
     * Called whenever the status of a BlockEntity host has changes that should be reflected.
     */
    public void refreshVertex(SystemVertex vert) {
        if(!hasWorld()) return;            

        BlockEntity be = world.getBlockEntity(vert.getPos());
        BlockState state = world.getBlockState(vert.getPos());
        Direction dir = DirectionTransformer.getUp(state);

        if(be instanceof WireNodeBlockEntity wbe) {
            Mechano.log("Vertex refreshed at " + vert.getPos());
            if(DirectionalEnergyStorable.hasMatchingCaps(world, vert.getPos(), dir)) {
                vert.setIsMember();
                // other such goofy shit here
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

    public void initializeWithin(ServerLevel world) {
        this.world = world;
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
     * @param idA
     * @param idB
     */
    public void link(SVID idA, SVID idB) {
        Pair<Integer, TransferSystem> sysA = getSystemContaining(idA);
        Pair<Integer, TransferSystem> sysB = getSystemContaining(idB);

        if(sysA == null && sysB == null) {
            TransferSystem newSystem = new TransferSystem();
            newSystem.addVert(idA.toVertex());
            newSystem.addVert(idB.toVertex());
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
            } else {
                sysB.getSecond().mergeWith(sysA.getSecond());
                subsystems.remove(sysA.getSecond());
            }
        }
        onSystemModified(idA);
    }

    /***
     * called whenever a system is modified from a global context 
     * (discrete modifications on the individual level won't call this)
     */
    public void onSystemModified() {
        NetworkSavedData.markInstanceDirty();
    }

    /***
     * called whenever a system is modified from a global context 
     * (discrete modifications on the individual level won't call this)
     * @param id origin of the modification
     */
    public void onSystemModified(SVID id) {
        NetworkSavedData.markInstanceDirty(id);
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

    public boolean existsInSystem(SystemVertex vert) {
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
        return link.toVertex();
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

    public String toString() {
        String head = "[";
        if(subsystems.isEmpty()) return head + "BLANK";

        String systems = "";
        for(int x = 0; x < subsystems.size(); x++) {
            systems += "System " + x + ": \n" + subsystems.get(x);
            if(x != subsystems.size() - 1)
                systems += "\t\n";
        }

        return head + "\n" + systems + "]";
    }
}