package com.quattage.mechano.api;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.numeric.EsoMath;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.Griddable;
import com.quattage.mechano.grid.api.component.CircuitComponent;
import com.quattage.mechano.grid.solver.BiCGStabStochastic;
import com.quattage.mechano.grid.solver.ConvergenceStatus;
import com.quattage.mechano.grid.solver.SolverMethod;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.link.AncillaryPair;
import com.quattage.mechano.infrastructure.EnqueuedGridManifest;
import com.quattage.mechano.switchboard.RemovalLedger;
import com.quattage.mechano.switchboard.action.GridAction;
import com.quattage.mechano.switchboard.task.TaskWrapper;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class ServerGrid extends Grid<AncillaryNode<?>> {

    private final SolverMethod solver = new BiCGStabStochastic();
    private @Nullable EnqueuedGridManifest activeManifest;
    private @Nullable Object2ObjectOpenHashMap<ChunkPos, Set<AncillaryNode<?>>> chunkOccupation;
    private @Nullable ObjectArrayList<GridDomain> domains = new ObjectArrayList<>();
    private @Nullable PriorityQueue<TaskWrapper> taskQueue;
    private boolean hasUnsavedChanges = false;
    private boolean hasLoadedDomains = false;

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
            GridDomain newDomain = new GridDomain(this);
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
    public GridDomain createNewNetlist() {
        GridDomain output = new GridDomain(this);
        this.domains.add(output);
        return output;
    }

    @Override
    public void load() {
        assertNotDisposed();
        for(GridDomain domain : domains)
            domain.solver().setStatus(ConvergenceStatus.IDLE);
    }

    @Override
    public void dispose() {
        assertNotDisposed();
        for(GridDomain domain : domains)
            domain.dispose();
        domains = null;
        if(activeManifest != null)
            activeManifest.dispose();
        solver.dispose();
        taskQueue = null;
        links = null;
    }

    @Override
    public void tick() {
        if(hasBeenDisposed()) return;
        tickManifest();
        processQueuedTasks();
        if(!hasLoadedDomains) return;
        for(int x = 0; x < domains.size(); x++) {
            GridDomain domain = domains.get(x);
            domain.preSolve(this, x);
            if(domain.indexer().hasStampers()) {
                domain.indexer().stampDynamic(domain);
                domain.solver().run(solver);
                domain.postSolve(this);
            } else if(!domain.hasBeenDisposed()) 
                Disposable.disposeOf(domain);
        }
    }

    public GridDomain getDomain(int index) {
        if(index < 0 || index >= domains.size()) 
            throw new ArrayIndexOutOfBoundsException("Index " + index + " is out of bounds for a grid with " + domains.size() + " domains");
        return domains.get(index);
    }

    public GridAction removeComponent(CircuitComponent component, @Nullable Entity modifier) {
        queueTask(GridAction.TASK_COMPONENT_DESTROY, component, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    @Override
    protected GridAction addLink(AncillaryPair link, @Nullable Entity modifier) {
        queueTask(GridAction.TASK_LINK_CREATE, link, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    private GridAction addLinkDeferred(AncillaryPair link) {
        GridAction output = super.addLinkAsymmetric(link.getStartAncillary(), link, true);
        if(output.getActionType().indicatesSuccess())
            super.addLinkAsymmetric(link.getEndAncillary(), link.flippedCopy(), true);
        markChunkly(link.getStartAncillary());
        markChunkly(link.getEndAncillary());
        return output;
    }

    private void markChunkly(AncillaryNode<?> node) {
        Griddable<?> source = GridTracking.getReferentOrThrow(node);
        if(source.canMoveDynamically()) return;
        ChunkPos cp = source.getChunkPos();
        if(cp == null) return;
        Set<AncillaryNode<?>> preexisting = chunkOccupation.get(cp);
        if(preexisting == null) {
            preexisting = new HashSet<>();
            chunkOccupation.put(cp, preexisting);
        }
        preexisting.add(node);
    }

    @Override
    protected GridAction removeLink(AncillaryPair link, @Nullable Entity modifier) {
        queueTask(GridAction.TASK_LINK_DESTROY, link, modifier);
        return GridAction.RESPONSE_SUCCESS;
    }

    private GridAction removeLinkDeferred(AncillaryPair link) {
        GridAction output = super.removeLinkAsymmetric(link.getStartAncillary(), link.getEndAncillary());
        super.removeLinkAsymmetric(link.getEndAncillary(), link.getStartAncillary());
        tryUnmark(link.getStartAncillary());
        tryUnmark(link.getEndAncillary());
        return output;
    }

    private void tryUnmark(AncillaryNode<?> node) {
        List<AncillaryPair> links = getLinksBelongingTo(node);
        if(links == null || links.isEmpty()) unmarkChunkly(node);
    }

    private void unmarkChunkly(AncillaryNode<?> node) {
        Griddable<?> source = GridTracking.getReferentOrThrow(node);
        if(source == null || source.canMoveDynamically()) return;
        ChunkPos cp = source.getChunkPos();
        if(cp == null) return;
        Set<AncillaryNode<?>> preexisting = chunkOccupation.get(cp);
        if(preexisting == null) return;
        preexisting.remove(node);
        if(preexisting.isEmpty()) chunkOccupation.remove(cp);
    }

    public void queueTask(GridAction task, Object... args) {
        Objects.requireNonNull(task);
        if(!task.isTask()) {
            error("Attempted to queue action '" + task + "' but this action is not a task type.");
            return;
        }
        if(!task.isWrappable()) {
            error("Attempted to queue action '" + task + "' but this task is forbidden from being queued.");
            return;
        }
        if(taskQueue == null) taskQueue = new PriorityQueue<>(16);
        TaskWrapper toAdd = new TaskWrapper(task, taskQueue.size(), args);
        taskQueue.add(toAdd);
        this.hasUnsavedChanges = true;
    }

    /**
     * Used for testing
     * TODO move this out of this class cuz its utility is extremely limited
     */
    public void queueRandomTask(RandomSource random) {
        Objects.requireNonNull(random);
        int idx = EsoMath.randomInt(random, 0, 3);
        GridAction task = GridAction.values()[idx];
        queueTask(task, new Object[0]);
    }

    private void processQueuedTasks() {
        if(taskQueue == null || taskQueue.isEmpty()) return;
        final RemovalLedger removals = new RemovalLedger();
        boolean brokeEarly = false;
        TaskWrapper working = null;
        while(!taskQueue.isEmpty()) {
            working = taskQueue.poll();
            if(working.creates()) {
                brokeEarly = true;
                break;
            }
            working.run(this, removals);
        }
        removals.apply(this);
        if(brokeEarly) working.run(this, removals);
        while(!taskQueue.isEmpty()) {
            working = taskQueue.poll();
            working.run(this, removals);
        }
        taskQueue = null;
    }

    @Override
    protected Stream<AncillaryPair> getLinksByChunk(ChunkPos pos) {
        return Optional.ofNullable(chunkOccupation.get(pos))
            .stream()
            .flatMap(Set::stream)
            .flatMap(node -> 
                Optional.ofNullable(links.get(node))
                    .stream().flatMap(List::stream)
            );
    }

    public List<GridDomain> domains() {
        return domains;
    }

    public SolverMethod solver() {
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
        if(server == null) server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Can't aquire a server instance as a client!");
        return server;
    }
}