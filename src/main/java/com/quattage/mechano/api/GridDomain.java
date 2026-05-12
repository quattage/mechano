package com.quattage.mechano.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.WorldlyObject;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.GridUUID;
import com.quattage.mechano.grid.Griddable;
import com.quattage.mechano.grid.api.component.StampingComponent;
import com.quattage.mechano.grid.api.component.StampingComponent.NeedsPostProcessing;
import com.quattage.mechano.grid.solver.ConvergenceStatus;
import com.quattage.mechano.grid.solver.NodalSolver;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.NetlistIndexer;
import com.quattage.mechano.grid.topology.NodeUnionSet;
import com.quattage.mechano.grid.topology.link.AncillaryPair;
import com.quattage.mechano.grid.topology.link.NodePair;
import com.quattage.mechano.switchboard.action.GridAction;
import com.quattage.mechano.switchboard.action.GridAction.ActionRunner;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class GridDomain implements WorldlyObject, Disposable {

    private @Nullable ServerGrid host; // only null if this domain has been disposed 
    private NetlistIndexer indexer = new NetlistIndexer();
    private NodeUnionSet netlist = new NodeUnionSet();
    private final NodalSolver solver = new NodalSolver();

    public GridDomain(ServerGrid host) {
        this.host = host;
    }

    public GridDomain(GridDomain original) {
        original.assertNotDisposed();
        this.host = original.host;
    }

    public void mergeWith(GridDomain other) {
        assertNotDisposed();
        Objects.requireNonNull(other);
        if(this == other) return;
        this.netlist = NodeUnionSet.concatenate(this.netlist, other.netlist);
        this.indexer = NetlistIndexer.concatenate(this.indexer, other.indexer);
        other.dispose();
    }

    /**
     * Creates a list of fresh GridDomain instances that contain the contents
     * of this one, but split along domain-level discontinuities.
     * @param world
     * @return
     */
    public @Nullable List<GridDomain> deriveFromSplits() {
        assertNotDisposed();
        if(netlist.isEmpty()) return null;
        List<NodeUnionSet> clusters = netlist.splitByDomain(this);
        List<GridDomain> outputDomains = new ArrayList<>(clusters.size());
        if(outputDomains.size() == 1) return Collections.emptyList();
        if(outputDomains.size() <= 0) return null;
        for(NodeUnionSet cluster : clusters) {
            // GridDomain newDomain = cluster.spawnFrom(this);
            // outputDomains.add(newDomain);
        }
        return outputDomains;
    }

    public void deleteLink(ActionRunner runner, NodePair pair) {
        // if the pair is a link it is deleted straight away
        if(pair instanceof AncillaryPair link) {
            GridAction removal = getHostGrid().remove(link);
            link.MNADeallocate(domain);
            AncillaryNode<?> start = link.getStartAncillary(), end = link.getEndAncillary();
            start.setDomainIndex(-2);
            end.setDomainIndex(-2);
            Griddable<?> ss = GridTracking.getReferentOrThrow(start), es = GridTracking.getReferentOrThrow(end);
            runner.targeting(ss, es)
                .withArguments(
                    GridTracking.getAddress(ss, start), 
                    GridTracking.getAddress(es, end)
                ).executeOnClients();
            if(!removal.getActionType().indicatesSuccess()) {
                domain.getHostGrid().error("Failed to delete link via direct acquisition (" + pair 
                    + ") - No active ancillary pair sharing these mappings could be located in the grid.");
            }
            Disposable.disposeOf(link);
            return;
        }
        // if the pair isnt a link the closest match is searched for
        List<AncillaryNode<?>> aAnc = pair.getNodeA().getAncillaries();
        List<AncillaryNode<?>> bAnc = pair.getNodeB().getAncillaries();
        if(aAnc == null || aAnc.isEmpty()) {
            domain.getHostGrid().error("Skipped deleting link " + pair + " - The starting node in this pair had no ancillaries to search from.");
            return;
        }
        if(bAnc == null || bAnc.isEmpty()) {
            domain.getHostGrid().error("Skipped deleting link " + pair + " - The ending node in this pair had no ancillaries to search from.");
            return;
        }
        for(AncillaryNode<?> an : aAnc) {
            Griddable<?> aSource = GridTracking.getReferentOrThrow(an);
            GridUUID<?> aID = GridTracking.getAddress(aSource, an);
            for(AncillaryNode<?> bn : bAnc) {
                AncillaryPair removed = domain.getHostGrid().lookup().pop(domain.getHostGrid(), an, bn);
                if(removed == null) continue;
                removed.MNADeallocate(domain);
                removed.getStartAncillary().setDomainIndex(-2);
                removed.getEndAncillary().setDomainIndex(-2);
                Griddable<?> bSource = GridTracking.getReferentOrThrow(bn);
                GridUUID<?> bID = GridTracking.getAddress(bSource, bn);
                runner.targeting(aSource, bSource)
                    .withArguments(aID, bID)
                    .executeOnClients();
                Disposable.disposeOf(removed);
            }
        }
    }

    /**
     * Updates this domain's matrix dimensions and 
     * finalizes the netlist's indices for each node.
     * This method only needs to be called when the 
     * shape of this domain changes.
     * @param grid {@link ServerGrid} that this domain belongs to
     * @param idx
     */
    public void preSolve(ServerGrid grid, int idx) {
        assertNotDisposed();
        if(solver.getStatus() != ConvergenceStatus.CHANGES_QUEUED) 
            return;
        solver.setStatus(ConvergenceStatus.PREPASSING);
        netlist.finalizeTopology(this, idx);
        int size = indexer.sourceCount() + netlist.size();
        solver.prepareWithSize(size);
        solver.setStatus(ConvergenceStatus.STAMPING);
        indexer.stamp(this);
    }

    /**
     * Handle components within this domain that require
     * {@link NeedsPostProcessing post processing}. This 
     * method is intended to be run after a call to 
     * {@link #solve}.
     * @param grid {@link ServerGrid} that this domain belongs to
     */
    public void postSolve(ServerGrid grid) {
        assertNotDisposed();
        for(StampingComponent sc : indexer.getStampers())
            if(sc instanceof NeedsPostProcessing pp) pp.postProcess(this, solver);
    }

    public NodeUnionSet netlist() {
        assertNotDisposed();
        return netlist;
    }

    public NetlistIndexer indexer() {
        assertNotDisposed();
        return indexer;
    }

    public NodalSolver solver() {
        return solver;
    }

    public String stateAsString() {
        String out = "domain state:\n";
        out += "A:\n"  + solver.matrixAsString() + "\n\n";
        out += "B:\n" + solver.termsAsString() + "\n\n";
        out += "X:\n" + solver.solutionAsString() + "\n\n";
        out += "idxr:\n" + indexer.toString() + "\n\n";
        return out;
    }

    public CompoundTag write(ServerGrid instantiator, Provider provider) {
        // ListTag output = new ListTag(netlist.size());
        // contents.put("netlist", writeLinks());
        return null;
    }


    public void read(ServerGrid instantiator, CompoundTag contents, Provider provider) {

    }

    // private ListTag writeLinks() {
    //     ListTag output = new ListTag(netlist.size());
    //     if(netlist.isEmpty()) return output;
    //     for(Map.Entry<AncillaryNode<?>, List<AncillaryPair>> branch : lookup.all()) {
    //         AncillaryNode<?> key = branch.getKey();
    //         List<AncillaryPair> contents = branch.getValue();
    //         if(contents.isEmpty()) continue;
    //         CompoundTag label = new CompoundTag();
    //         GridUUID<?> id = GridTracking.getAddress(key.getProviderSource(), key);
    //         ListTag connections = new ListTag(contents.size());
    //         for(AncillaryPair link : contents) {
    //             CompoundTag newTag = new CompoundTag();
    //             AncillaryNode<?> node = link.getEndAncillary();
    //             GridUUID<?> newID = GridTracking.getAddress(node.getProviderSource(), node);
    //             newID.write(newTag);
    //             int trnsType = -1;
    //             if(link instanceof ComponentLink<?> cl)
    //                 trnsType = Mechano.REGISTRATE.getTransmitterRegistry().getId(cl.getTransmitter());
    //             newTag.putInt("trns", trnsType);
    //             connections.add(newTag);
    //         }
    //         id.write(label);
    //         label.put("conns", connections);    
    //         output.add(label);
    //     }
    //     return output;
    // }

    @Override
    public @Nullable Level getWorld() {
        assertNotDisposed();
        return host.getWorld();
    }

    public ServerGrid getHostGrid() {
        assertNotDisposed();
        return host;
    }


    @Override
    public void dispose() {
        this.host = null;
        solver.dispose();
        Disposable.disposeOf(indexer);
        Disposable.disposeOf(netlist);
    }

    @Override
    public boolean hasBeenDisposed() {
        return host != null;
    }
}
