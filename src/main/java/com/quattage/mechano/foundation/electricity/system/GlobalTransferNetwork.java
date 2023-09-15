package com.quattage.mechano.foundation.electricity.system;

import java.util.ArrayList;

import com.quattage.mechano.Mechano;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/***
 * The GlobalTransferNetwork is an elevated level controller for TransferSystems.
 * It offers functionality to intelligently manage a list of networks, where
 * individual networks are stored as subsystems which can be added to, removed from,
 * split, merged, repaired, etc.
 */
public class GlobalTransferNetwork {
    
    private ArrayList<TransferSystem> subsystems = new ArrayList<TransferSystem>();
    private boolean loaded = false;

    public static final GlobalTransferNetwork NETWORK = new GlobalTransferNetwork();

    public GlobalTransferNetwork() {}

    public void readFrom(CompoundTag in) {
        CompoundTag net = in.getCompound(NetworkSavedData.MECHANO_NETWORK_KEY);

        if(NetworkSavedData.SAVE_VERSION != net.getInt("ver")) {
            Mechano.LOGGER.warn("Unable to serialize GlobalTransferNetwork from disk - saved copy was marked as depricated!");
            return;
        }

        ListTag subs = net.getList("all", Tag.TAG_COMPOUND);
        Mechano.log("Reading " + subs.size() + " TransferNetworks");
        for(int x = 0; x < subs.size(); x++) {
            Mechano.log("ADDING NETWORK " + subs.getCompound(x));
            subsystems.add(new TransferSystem(subs.getCompound(x)));
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        out.putInt("ver", NetworkSavedData.SAVE_VERSION);
        out.put("all", writeAllSubsystems());
        in.put(NetworkSavedData.MECHANO_NETWORK_KEY, out);
        return in;
    }

    public boolean needsLoaded() {
        if(!loaded) {
            loaded = true;
            return true;
        }
        return false;
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
     * Links two SystemNodes without question. If these nodes don't belong to a subsystem,
     * a new subsystem is made form them. If both nodes are in independent subsystems, these
     * subsystems are merged.
     * @param nodeOne
     * @param nodeTwo
     */
    public void link(SystemNode nodeOne, SystemNode nodeTwo) {
        Pair<Integer, TransferSystem> fromSystem = getSystemContaining(nodeOne);
        Pair<Integer, TransferSystem> toSystem = getSystemContaining(nodeTwo);

        if(fromSystem == null && toSystem == null) {
            TransferSystem newSystem = new TransferSystem();
            newSystem.addNode(nodeOne);
            newSystem.addNode(nodeTwo);
            newSystem.link(nodeOne, nodeTwo);
            subsystems.add(newSystem);

        } else if(fromSystem != null && toSystem == null) {
            fromSystem.getSecond().addNode(nodeTwo);
            fromSystem.getSecond().link(nodeOne, nodeTwo);

        } else if(fromSystem == null && toSystem != null) {
            toSystem.getSecond().addNode(nodeOne);
            toSystem.getSecond().link(nodeOne, nodeTwo);

        } else if(fromSystem.getFirst() == toSystem.getFirst()) {
            fromSystem.getSecond().link(nodeOne, nodeTwo);

        } else if(fromSystem.getFirst() != toSystem.getFirst()) {
            if(fromSystem.getFirst() < toSystem.getFirst()) {
                fromSystem.getSecond().mergeWith(toSystem.getSecond());
                subsystems.remove(toSystem.getSecond());
            } else {
                toSystem.getSecond().mergeWith(fromSystem.getSecond());
                subsystems.remove(fromSystem.getSecond());
            }
        }
        onSystemModified();
        debug("LINK OPERATION");
    }

    /***
     * Links two SystemNodes without question. If these nodes don't belong to a subsystem,
     * a new subsystem is made form them. If both nodes are in independent subsystems, these
     * subsystems are merged.
     * @param linkOne
     * @param linkTwo
     */
    public void link(SystemVertex linkOne, SystemVertex linkTwo) {
        SystemNode nodeOne = getOrCreateNodeAt(linkOne);
        SystemNode nodeTwo = getOrCreateNodeAt(linkTwo);
        Pair<Integer, TransferSystem> fromSystem = getSystemContaining(nodeOne);
        Pair<Integer, TransferSystem> toSystem = getSystemContaining(nodeTwo);

        if(fromSystem == null && toSystem == null) {
            TransferSystem newSystem = new TransferSystem();
            newSystem.addNode(nodeOne);
            newSystem.addNode(nodeTwo);
            newSystem.link(nodeOne, nodeTwo);
            subsystems.add(newSystem);

        } else if(fromSystem != null && toSystem == null) {
            fromSystem.getSecond().addNode(nodeTwo);
            fromSystem.getSecond().link(nodeOne, nodeTwo);

        } else if(fromSystem == null && toSystem != null) {
            toSystem.getSecond().addNode(nodeOne);
            toSystem.getSecond().link(nodeOne, nodeTwo);

        } else if(fromSystem.getFirst() == toSystem.getFirst()) {
            fromSystem.getSecond().link(nodeOne, nodeTwo);

        } else if(fromSystem.getFirst() != toSystem.getFirst()) {
            if(fromSystem.getFirst() < toSystem.getFirst()) {
                fromSystem.getSecond().mergeWith(toSystem.getSecond());
                subsystems.remove(toSystem.getSecond());
            } else {
                toSystem.getSecond().mergeWith(fromSystem.getSecond());
                subsystems.remove(fromSystem.getSecond());
            }
        }
        onSystemModified();
        debug("LINK OPERATION");
    }

    public void onSystemModified() {
        NetworkSavedData.markInstanceDirty();
    }

    /***
     * Removes the link between nodes at two given positions as long as they exist.
     * @throws NullPointerException if no node could be found at either given BlockPos
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(SystemVertex linkOne, SystemVertex linkTwo) {
        unlink(linkOne, linkTwo, true);
    }

    /***
     * Removes the link between nodes at two given positions as long as they exist.
     * @throws NullPointerException if no node could be found at either given BlockPos
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(SystemVertex linkOne, SystemVertex linkTwo, boolean clean) {
        SystemNode nodeOne = getNodeAt(linkOne);
        SystemNode nodeTwo = getNodeAt(linkTwo);
        if(nodeOne == null) throw new NullPointerException("Failed to unlink SystemNode from a global context - " + 
            "No valid SystemNode at " + linkOne + " could be found! (first provided parameter)");

        if(nodeTwo == null) throw new NullPointerException("Failed to unlink SystemNode from a global context - " + 
            "No valid SystemNode at " + linkTwo + " could be found! (second provided parameter)");

        nodeOne.unlinkFrom(nodeTwo);
        nodeTwo.unlinkFrom(nodeOne);
        debug("UNLINK OPERATION");
        if(clean)
            declusterize();
        onSystemModified();
    }

    /***
     * Removes the link between two provided SystemNodes.
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(SystemNode nodeOne, SystemNode nodeTwo) {
        unlink(nodeOne, nodeTwo, true);
    }

    /***
     * Removes the link between two provided SystemNodes.
     * @param linkOne
     * @param linkTwo
     * @param clean (Defaults to true, reccomended) If true, the network
     * will be declusterized at the end of the unlinking operation.
     */
    public void unlink(SystemNode nodeOne, SystemNode nodeTwo, boolean clean) {
        nodeOne.unlinkFrom(nodeTwo);
        nodeTwo.unlinkFrom(nodeOne);
        debug("UNLINK OPERATION");
        if(clean)
            declusterize();
        onSystemModified();
    }

    /***
     * Removes clusters from TransferSystems in this GlobalTransferNetwork.
     * Whenever a TransferSystem has discontinuities, this method breaks those
     * "clusters" out into their own TransferSystem instances. This way we can
     * always guarantee that valid paths exist between SystemNodes in a TransferSystem.
     */
    public void declusterize() {
        ArrayList<TransferSystem> evaluated = new ArrayList<>();
        for(TransferSystem sys : subsystems) 
            evaluated.addAll(sys.trySplit());
        subsystems.clear();
        subsystems.addAll(evaluated);
        debug("DECLUSTERIZE RESULT");
    }

    public Pair<Integer, TransferSystem> getSystemContaining(SystemNode node) {
        int x = 0;
        for(TransferSystem sys : subsystems) {
            if(sys.containsNode(node)) return Pair.of(x, sys);
            x++;
        }
        return null;
    }

    public Pair<Integer, TransferSystem> getSystemContaining(SystemVertex link) {
        int x = 0;
        for(TransferSystem sys : subsystems) {
            if(sys.getNode(link) != null) return Pair.of(x, sys);
            x++;
        }
        return null;
    }

    /***
     * Gets the SystemNode at this BlockPos.
     * @param pos
     * @return Returns the SystemNode at this BlockPos, or null if none exists.
     */
    public SystemNode getNodeAt(SystemVertex link) {
        for(TransferSystem sys : subsystems) {
            SystemNode node = sys.getNode(link);
            if(node != null) return node;
        }
        return null;
    }

    /***
     * Gets the SystemNode at this BlockPos. If one does not exist,
     * a new SystemNode will be created there instead.
     * @param pos
     * @return Returns the pre-existing or new SystemNode at the given BlockPos
     */
    public SystemNode getOrCreateNodeAt(SystemVertex link) {
        for(TransferSystem sys : subsystems) {
            SystemNode node = sys.getNode(link);
            if(node != null) return node;
        }
        return new SystemNode(link, true);
    }

    /***
     * Gets the node and the TransferSystem it belongs to regardless of sub index.
     * Only fetches based on BLockPos.
     */
    public Pair<TransferSystem, SystemNode> getSystemAndNode(SystemVertex link) {
        for(TransferSystem sys : subsystems) {
            SystemNode node = sys.getNode(link);
            if(node != null) return Pair.of(sys, node);
        }
        return null;
    }

    public int getSubsystemID(TransferSystem system) {
        return subsystems.indexOf(system);
    }

    public boolean isIndexValid(int index) {
        return -1 < index && index < subsystems.size();
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

    public void debug(String operation) {
        Mechano.log(operation + ": \n" + NETWORK);
    }
}