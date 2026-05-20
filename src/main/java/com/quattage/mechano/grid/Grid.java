    package com.quattage.mechano.grid;

    import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.Disposable;
import com.quattage.mechano.foundation.WorldlyObject;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.AncillaryPair;
import com.quattage.mechano.grid.topology.ComponentLink;
import com.quattage.mechano.switchboard.action.GridAction;
import com.quattage.mechano.switchboard.action.GridAction.ActionSync;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

    /**
     * This class represents the link between the Minecraft world and the grid.
     * The Client and Server grids are instantiated as Data Attachments that belong
     * to the level.
     */
    @EventBusSubscriber
    public abstract sealed class Grid<T> implements WorldlyObject, Disposable permits ClientGrid, ServerGrid {
    // these words aren't in the bible

        protected static final Logger LOGGER = LogUtils.getLogger();

        // weakly referenced singletons are stored to skip hash capability lookups
        private static WorldlyReference<ServerGrid> weakServerGrid = null;
        private static WorldlyReference<ClientGrid> weakClientGrid = null;

        public static final IAttachmentSerializer<CompoundTag, Grid<?>> SERIALIZER = new IAttachmentSerializer<>() {
            @Override
            public Grid<?> read(IAttachmentHolder holder, CompoundTag tag, Provider provider) {
                if(!(holder instanceof Level world)) {
                    throw new IllegalStateException("Attempted to de-serialize grid attachment from non-level source (" 
                        + (holder == null ? "null" : holder.getClass().getSimpleName()) + ")");
                }
                Grid<?> newGrid = Grid.unsided(world);
                newGrid.read(world, tag, provider);
                return newGrid;
            }
            @Override
            public @Nullable CompoundTag write(Grid<?> attachment, Provider provider) {
                CompoundTag written = new CompoundTag();
                attachment.write(attachment.getWorld(), written, provider);
                return written;
            }
        };

        @SubscribeEvent
        public static void onWorldLoad(LevelEvent.Load evt) {
            LevelAccessor world = evt.getLevel();
            Grid.unsided(world).load();
        }

        @SubscribeEvent
        public static void onWorldUnload(LevelEvent.Unload evt) {
            LevelAccessor world = evt.getLevel();
            Grid.unsided(world).dispose();
        }

        @SubscribeEvent
        public static void onWorldTick(LevelTickEvent.Post evt) {
            Level world = evt.getLevel();
            Grid.unsided(world).tick();
        }

        @SubscribeEvent
        public static void onChunkWatch(ChunkWatchEvent.Sent evt) {
            Level world = evt.getLevel();
            if(world.isClientSide) return;
            ServerGrid grid = Grid.server(world);
            grid.getLinksByChunk(evt.getPos()).forEach(pair -> {
                TransmitterType trns = pair instanceof ComponentLink<?> link ? link.getTransmitter() : null;
                GridAction.TASK_LINK_SYNC.broadcast(grid, new Object[] { pair.getStartID(), pair.getEndID(), trns });
            });
        }

        /**
         * Instantiates a new Grid instance based on the holder provided.
         * This method won't attach the instance yet, but it will choose
         * the type based on the logical side of the provided holder. <p>
         * API users don't need to call this method since its used 
         * internally to populate the data attachment. For accessing
         * or lazily populating the grid data attachment after the world 
         * has loaded, use the {@link Grid#client client-sided} and 
         * {@link Grid#server server-sided} getters.
         * @param holder The holder that the grid will be attached to (Must be a {@link Level} instance)
         * @return The new grid instance instance that was attached
         * @throws IllegalArgumentException if the provided holder isn't compatable (grids may only be attached to levels)
         */
        @ApiStatus.Internal
        public static Grid<?> attachTo(IAttachmentHolder holder) {
            Grid<?> freshInstance = null;
            if(holder instanceof Level world) {
                if(world.isClientSide()) freshInstance = new ClientGrid(world);
                else freshInstance = new ServerGrid(world);
            }
            else throw new IllegalArgumentException("Mechano Grid Data can only be attached to levels, got " + holder + "!");
            return freshInstance;
        }

        /**
         * Gets the grid data attachment instance attached to the provided 
         * {@link LevelReader} regardless of logical side.
         * This grid may either be a {@link ServerGrid server-sided} or 
         * {@link ClientGrid client-sided} instance. If you need to
         * guarantee side-specific access without extra effort, use the
         * {@link Grid#client client-sided} or {@link Grid#server server-sided} 
         * getter.
         * @param world The LevelReader to pull the data attachment from
         * @return A (new or preexisting) Grid instance
         */
        public static @NotNull Grid<?> unsided(LevelReader world) {
            if(world.isClientSide()) return Grid.client(world);
            return Grid.server(world);
        }


        /**
         * Gets the server-sided grid data attachment from the provided LevelReader.
         * If no grid instance exists attached to the provided LevelReader, a new one
         * will be created and returned. Subsequent calls to this method will avoid
         * hash lookups where possible.
         * @param world World to get data from
         * @return A (new or preexisting) {@link ServerGrid} attached to the provided level
         * @throws NullPointerException if <code>world</code> is null
         * @throws IllegalArgumentException if method is called from the wrong logical side
         */
        public static ServerGrid server(LevelReader world) {
            Objects.requireNonNull(world);
            if(!(world instanceof ServerLevel sl)) throw new IllegalArgumentException("Can't acquire a server-sided grid from non-server world " + world);
            if(Grid.weakServerGrid != null && Grid.weakServerGrid.isAttachedTo(sl)) {
                ServerGrid precache = Grid.weakServerGrid.get();
                if(!precache.hasBeenDisposed()) return precache;
                sl.removeData(MechanoData.GRID.get());
            }
            Grid<?> attachment = sl.getData(MechanoData.GRID.get());
            Grid.weakServerGrid = new WorldlyReference<ServerGrid>((ServerGrid)attachment);
            return Grid.weakServerGrid.get();
        }

        /**
         * Gets the client-sided grid data attachment from the provided LevelReader.
         * If no grid instance exists attached to the provided LevelReader, a new one
         * will be created and returned. Subsequent calls to this method will avoid
         * hash lookups where possible.
         * @param world World to get data from
         * @return A (new or preexisting) {@link ClientGrid} attached to the provided level
         * @throws NullPointerException if <code>world</code> is null
         * @throws IllegalArgumentException if method is called from the wrong logical side
         */
        @OnlyIn(Dist.CLIENT)
        public static ClientGrid client(LevelReader world) {
            Objects.requireNonNull(world);
            if(!(world instanceof ClientLevel cl)) throw new IllegalArgumentException("Can't acquire a client-sided grid from server-sided world " + world);
            if(Grid.weakClientGrid != null && Grid.weakClientGrid.isAttachedTo(cl)) {
                ClientGrid precache = Grid.weakClientGrid.get();
                if(!precache.hasBeenDisposed()) return precache;
                cl.removeData(MechanoData.GRID.get());
            }
            Grid<?> attachment = cl.getData(MechanoData.GRID.get());
            Grid.weakClientGrid = new WorldlyReference<ClientGrid>((ClientGrid)attachment);
            return Grid.weakClientGrid.get();
        }

        protected final Level world;
        @Nullable protected Object2ObjectOpenHashMap<T, List<AncillaryPair>> links;

        protected Grid(Level world) {
            Objects.requireNonNull(world);
            this.world = world;
            this.links = new Object2ObjectOpenHashMap<>();
        }

        protected abstract void read(LevelReader world, CompoundTag contents, Provider provider);
        protected abstract void write(LevelReader world, CompoundTag contents, Provider provider);
        protected abstract void load();
        public abstract void tick();

        public abstract GridAction addLink(AncillaryPair link, @Nullable Entity modifier);
        public abstract GridAction removeLink(AncillaryPair link, @Nullable Entity modifier);
        public abstract GridAction removeLink(T a, T b, @Nullable Entity modifier);
        protected abstract Stream<AncillaryPair> getLinksByChunk(ChunkPos pos);

        public abstract int netlistCount();
        public abstract int linkCount();

        public @Nullable List<AncillaryPair> getLinksBelongingTo(T lookup) {
            return links.get(lookup);
        }

        public @Nullable AncillaryPair getLinkMatching(T start, T end) {
            List<AncillaryPair> linksAt = getLinksBelongingTo(start);
            if(linksAt == null || linksAt.isEmpty()) return null;
            for(int x = 0; x < linksAt.size(); x++) {
                AncillaryPair link = linksAt.get(x);
                if(link.endsWith(end)) return link;
            }
            return null;
        }

        protected GridAction addLinkAsymmetric(T hash, AncillaryPair link, boolean limit) {
            List<AncillaryPair> linksAt = getLinksBelongingTo(hash);
            if(linksAt == null) {
                linksAt = new ArrayList<AncillaryPair>();
                linksAt.add(link);
                link.onAddedToGrid(this);
                links.put(hash, linksAt);
                return GridAction.RESPONSE_SUCCESS;
            }
            if(linksAt.contains(link)) return GridAction.RESPONSE_FAIL_DUPLICATE_ELEMENT;
            if(limit && linksAt.size() >= AncillaryNode.MAX_SHARED_OCCUPANCY)
                return GridAction.RESPONSE_FAIL_ELEMENT_FULL;
            linksAt.add(link);
            link.onAddedToGrid(this);
            return GridAction.RESPONSE_SUCCESS;
        }

        protected GridAction removeLinkAsymmetric(T start, T end) {
            List<AncillaryPair> linksAt = getLinksBelongingTo(start);
            if(linksAt == null) return GridAction.RESPONSE_FAIL_START_MISSING;
            int toRemove = -1;
            for(int x = 0; x < linksAt.size(); x++) {
                AncillaryPair link = linksAt.get(x);
                if(link != null && link.endsWith(end)) {
                    toRemove = x;
                    break;
                }
            }
            if(toRemove < 0) return GridAction.RESPONSE_FAIL_END_MISSING;
            AncillaryPair removed = linksAt.remove(toRemove);
            if(removed != null) {
                if(linksAt.isEmpty()) links.remove(start);
                removed.onRemovedFromGrid(this);
                return GridAction.RESPONSE_SUCCESS;
            }
            return GridAction.RESPONSE_FAIL_MISSING;
        }

        protected @Nullable AncillaryPair popLinkAsymmetric(T start, T end) {
            List<AncillaryPair> linksAt = getLinksBelongingTo(start);
            if(linksAt == null) return null;
            int toRemove = -1;
            for(int x = 0; x < linksAt.size(); x++) {
                AncillaryPair link = linksAt.get(x);
                if(link != null && link.endsWith(end)) {
                    toRemove = x;
                    break;
                }
            }
            if(toRemove < 0) return null;
            AncillaryPair removed = linksAt.remove(toRemove);
            if(removed != null) {
                if(linksAt.isEmpty()) links.remove(start);
                removed.onRemovedFromGrid(this);
                return removed;
            }
            return null;
        }

        public void forEachLink(Consumer<AncillaryPair> cons) {
            if(links.isEmpty()) return;
            for(Map.Entry<T, List<AncillaryPair>> entry : links.entrySet()) {
                List<AncillaryPair> links = entry.getValue();
                if(links == null || links.isEmpty())
                    continue;
                for(AncillaryPair link : links)
                    if(link != null) cons.accept(link);
            }
        }

        /**
         * Creates a new {@link ActionSync} bound to this grid
         * @param action The action to assign to the runner
         * @return A new ActionRunner instance
         */
        public ActionSync initiateTask(GridAction action) {
            if(action == null) throw new NullPointerException("Error running task from " + this + " - The provided task is null!");
            if(!action.isTask()) throw new IllegalArgumentException("Error running task from " + this + " - The provided action is not a task type!");
            return new ActionSync(this, action);
        }

        @Override
        public Level getWorld() {
            return world;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + getDimensionName() + "]";
        }

        @Override
        public boolean hasBeenDisposed() {
            return links == null;
        }

        @Override
        public String getDimensionName() {
            return world == null ? "n/a" : world.dimension().location().toString();
        }

        public void info(String msg) {
            Grid.LOGGER.info("(" + getDimensionName() + ") " + msg);
        }

        public void warn(String msg) {
            Grid.LOGGER.warn("(" + getDimensionName() + ") " + msg);
        }

        public void debug(String msg) {
            Grid.LOGGER.debug("(" + getDimensionName() + ") " + msg);
        }

        public void error(String msg) {
            Grid.LOGGER.error("(" + getDimensionName() + ") " + msg);
        }
    }
