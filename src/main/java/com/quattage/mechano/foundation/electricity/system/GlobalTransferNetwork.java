package com.quattage.mechano.foundation.electricity.system;

import java.util.ArrayList;

import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;

public class GlobalTransferNetwork {
    public ArrayList<TransferSystem> subsystems = new ArrayList<TransferSystem>();

    public int getSubsystemCount() {
        return subsystems.size();
    }

    /***
     * Combines two TransferSystems within this GlobalTransferNetwork.
     * More specifically, it takes first system provided, merges it into 
     * the second system provided, and then removes the second system from 
     * this GlobalTransferNetwork.
     * @throws NullPointerException If either system provided is not present in this GlobalTransferNetwork
     * @param firstIndex Index of first network
     * @param secondIndex Index of second network
     */
    public void forceMergeSystems(TransferSystem sysOne, TransferSystem sysTwo) {
        if(!subsystems.contains(sysOne)) 
            throw new NullPointerException("Failed to merge TransferSystems - '" + sysOne 
                + "' is not present in this GlobalTransferNetwork!");
        if(!subsystems.contains(sysTwo)) 
            throw new NullPointerException("Failed to merge TransferSystems - '" + sysTwo 
                + "' is not present in this GlobalTransferNetwork!");

        sysOne.mergeWith(sysTwo);
        subsystems.remove(sysTwo);
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

        // neither node is in a subsystem, so make the subsystem and link them
        if(fromSystem == null && toSystem == null) {
            TransferSystem newSystem = new TransferSystem();
            newSystem.addNode(nodeOne);
            newSystem.addNode(nodeTwo);
            newSystem.link(nodeOne, nodeTwo);
            subsystems.add(newSystem);
            return;
        }

        // first node is in a subsystem, second node is not
        if(fromSystem != null && toSystem == null) {
            fromSystem.getSecond().addNode(nodeTwo);
            fromSystem.getSecond().link(nodeOne, nodeTwo);
            return;
        }

        // first node is not in a subsystem, second node is
        if(fromSystem == null && toSystem != null) {
            toSystem.getSecond().addNode(nodeOne);
            toSystem.getSecond().link(nodeOne, nodeTwo);
            return;
        }

        // both nodes are in the same subsystem, so just link them
        if(fromSystem.getFirst() == toSystem.getFirst()) {
            fromSystem.getSecond().link(nodeOne, nodeTwo);
            return;
        }

        // both nodes are in seperate subsystems
        if(fromSystem.getFirst() != toSystem.getFirst()) {
            if(fromSystem.getFirst() < toSystem.getFirst()) {
                fromSystem.getSecond().mergeWith(toSystem.getSecond());
                subsystems.remove(toSystem.getSecond());
            } else {
                toSystem.getSecond().mergeWith(fromSystem.getSecond());
                subsystems.remove(fromSystem.getSecond());
            }
        }
    }

    /***
     * Whenever any "clusters" exist in a subsystem, they must be broken down.
     * This method scans the GlobalTransferNetwork for any TransferSystems that
     * contain discontinuous clusters. If there is no path from one vertex to
     * another within a single TransferSystem, this TransferSystem has
     * discontinuous clusters and must be broken down. <p>
     * During the process, if a node has no connections, it is removed.
     * Additionally, if a TransferSystem only has only one node, it won't be
     * added to this GlobalTransferNetwork.
     */
    public void declusterize() {
        ArrayList<TransferSystem> evaluated = new ArrayList<>();
        for(TransferSystem sys : subsystems) 
            evaluated.addAll(sys.trySplit());
        subsystems.clear();
        subsystems.addAll(evaluated);
    }

    public Pair<Integer, TransferSystem> getSystemContaining(SystemNode node) {
        int x = 0;
        for(TransferSystem sys : subsystems) {
            if(sys.containsNode(node)) return Pair.of(x, sys);
            x++;
        }
        return null;
    }

    public Pair<Integer, TransferSystem> getSystemContaining(BlockPos pos) {
        int x = 0;
        for(TransferSystem sys : subsystems) {
            if(sys.getNode(pos) != null) return Pair.of(x, sys);
            x++;
        }
        return null;
    }

    public boolean isIndexValid(int index) {
        return -1 < index && index < subsystems.size();
    }
    

    public String toString() {
        String head = "GlobalTransferNetwork: [";
        if(subsystems.isEmpty()) return head + "BLANK";

        String systems = "";
        for(int x = 0; x < subsystems.size(); x++) {
            systems += "System " + x + ": " + subsystems.get(x);
            if(x != subsystems.size() - 1)
                systems += "\t\n";
        }

        return head + "\n" + systems + "]";
    }
}