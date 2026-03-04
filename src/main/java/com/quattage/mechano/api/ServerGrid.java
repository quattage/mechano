package com.quattage.mechano.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.solver.BiCGStabRandom;
import com.quattage.mechano.api.grid.solver.NodalSolver;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatus;
import com.quattage.mechano.api.grid.solver.NodalSolver.ConvergenceStatusHolder;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.NetlistLookup.ServerNetlistLookup;
import com.quattage.mechano.api.grid.topology.landmark.link.AncillaryPair;
import com.quattage.mechano.api.switchboard.TopologyProcessQueue;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.infrastructure.EnqueuedGridManifest;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ServerGrid extends Grid {

    private @Nullable EnqueuedGridManifest activeManifest;
    private final ServerNetlistLookup lookup = new ServerNetlistLookup();
    private NodalSolver solver = new BiCGStabRandom();
    protected final ConvergenceStatusHolder status = new ConvergenceStatusHolder();
    private final TopologyProcessQueue processQueue = new TopologyProcessQueue();
    private ArrayList<GridDomain> domains = new ArrayList<>();

    protected ServerGrid(Level world) {
        super(world);
    }

    @Override
    protected void read(LevelReader world, CompoundTag contents, Provider provider) {
        if(contents.isEmpty()) return;
        ListTag writtenGrid = contents.getList("grid", Tag.TAG_COMPOUND);
        domains.ensureCapacity(writtenGrid.size());
        for(int x = 0; x < writtenGrid.size(); x++) {
            CompoundTag writtenDomain = writtenGrid.getCompound(x);
            GridDomain newDomain = new GridDomain();
            newDomain.read(this, writtenDomain, provider);
            if(!newDomain.netlist().isEmpty())
                domains.add(newDomain);
        }
    }

    @Override
    protected void write(LevelReader world, CompoundTag contents, Provider provider) {
        ListTag writtenGrid = new ListTag(domains.size());
        for(GridDomain domain : domains) {
            CompoundTag writtenDomain = domain.write(this, provider);
            if(writtenDomain != null) writtenGrid.add(writtenDomain);
        }
        contents.put("grid", writtenGrid);
    }

    @ApiStatus.Internal
    public GridDomain makeFreshDomain() {
        GridDomain output = new GridDomain();
        this.domains.add(output);
        return output;
    }

    @Override
    public void load() {
        status.set(ConvergenceStatus.IDLE);
    }

    @Override
    public void unload() {
        status.set(ConvergenceStatus.REFRESHING_TOPOLOGY);
        for(GridDomain domain : domains)
            domain.clear();
        domains = new ArrayList<>();
        if(activeManifest != null)
            activeManifest.dispose();
        solver.reset();
        lookup.reset();
        status.set(ConvergenceStatus.UNLOADED);
    }

    @Override
    public void tick() {
        tickManifest();
        if(status.isUnloaded()) return;
        if(processQueue.containsChanges()) {
            status.set(ConvergenceStatus.REFRESHING_TOPOLOGY);
            processQueue.applyTo(this);
            preProcessDomains();
        }
        status.set(ConvergenceStatus.LIVE);
        for(GridDomain domain : domains) {
            if(domain.indexer().hasStampers()) {
                domain.indexer().stampDynamic(this, domain);
                domain.solve(this, solver);
                domain.postSolve(this);
            } else domain.idle();
        }
    }

    private void preProcessDomains() {
        for(int x = 0; x < domains.size(); x++) {
            GridDomain domain = domains.get(x);
            if(!domain.isTopologyOutdated()) continue;
            domain.preSolve(this, x);
        }
    }

    public GridDomain getDomain(int index) {
        if(index < 0 || index >= domains.size()) 
            throw new ArrayIndexOutOfBoundsException("Index " + index + " is out of bounds for a grid with " + domains.size() + " domains");
        return domains.get(index);
    }

    public GridDomain getDomain(CircuitComponent component) {
        Objects.requireNonNull(component);
        return getDomain(component.getDomainIndex());
    }

    public GridAction removeLinkDeferred(AncillaryPair link, @Nullable Entity modifier) {
        Objects.requireNonNull(link);
        processQueue.add(this, GridAction.TASK_LINK_DESTROY, link, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction removeComponent(CircuitComponent component, @Nullable Entity modifier) {
        Objects.requireNonNull(component);
        processQueue.add(this, GridAction.TASK_COMPONENT_DESTROY, component, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    public GridAction addLinkDeferred(AncillaryPair link, @Nullable Entity modifier) {
        Objects.requireNonNull(link);
        processQueue.add(this, GridAction.TASK_LINK_CREATE, link, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    public ServerNetlistLookup lookup() {
        return lookup;
    }

    public List<GridDomain> domains() {
        return domains;
    }

    /**
     * Indicates whether or not the matrix is solved. This method is
     * useful to ensure that logic running on Minecraft's render thread
     * doesn't ingest matrix values that are outdated.
     * @return {@link ConvergenceStatus}
     */
    public ConvergenceStatusHolder statusHolder() {
        return status;
    }

    public NodalSolver getSolver() {
        return solver;
    }

    public void enqueueManifest(Entity requester) {
        Objects.requireNonNull(requester);
        activeManifest = new EnqueuedGridManifest(this, requester);
    }

    private void tickManifest() {
        if(activeManifest != null) { 
            if(activeManifest.isConsumed())
                activeManifest = null;
            else activeManifest.tick();
        };
    }

    public MinecraftServer getServer() {
        MinecraftServer server = ((ServerLevel)getWorld()).getServer();
        if(server == null)
            server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        return server;
    }
}