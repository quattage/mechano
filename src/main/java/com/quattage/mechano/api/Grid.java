    package com.quattage.mechano.api;

    import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.GriddableTerminus;
import com.quattage.mechano.api.grid.topology.NetlistLookup;
import com.quattage.mechano.api.grid.topology.landmark.ComponentLink;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridAction.ActionRunner;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.WorldlyObject;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
    public abstract sealed class Grid implements WorldlyObject permits ClientGrid, ServerGrid {
    // these words aren't in the bible

        protected static final Logger LOGGER = LogUtils.getLogger();

        // weakly referenced singletons are stored to skip hash capability lookups
        private static WorldlyReference<ServerGrid> weakServerGrid = null;
        private static WorldlyReference<ClientGrid> weakClientGrid = null;

        public static final IAttachmentSerializer<CompoundTag, Grid> SERIALIZER = new IAttachmentSerializer<>() {
            @Override
            public Grid read(IAttachmentHolder holder, CompoundTag tag, Provider provider) {
                if(!(holder instanceof Level world)) {
                    throw new IllegalStateException("Attempted to de-serialize grid attachment from non-level source (" 
                        + (holder == null ? "null" : holder.getClass().getSimpleName()) + ")");
                }
                Grid newGrid = Grid.getUnsided(world);
                newGrid.read(world, tag, provider);
                return newGrid;
            }
            @Override
            public @Nullable CompoundTag write(Grid attachment, Provider provider) {
                CompoundTag written = new CompoundTag();
                attachment.write(attachment.getWorld(), written, provider);
                return written;
            }
        };

        /**
         * To be called by internal registries to populate the world with an initial data attachment
         * @param holder The world to attach to
         * @return The sided dispatcher instance that was attached
         * @throws IllegalArgumentException if the provided holder isn't compatable (for now, grids can only be attached to levels)
         */
        @ApiStatus.Internal
        public static Grid createNew(IAttachmentHolder holder) {
            Grid freshInstance = null;
            if(holder instanceof Level world) {
                if(world.isClientSide()) freshInstance = new ClientGrid(world);
                else freshInstance = new ServerGrid(world);
            }
            else throw new IllegalArgumentException("Mechano Grid Data can only be attached to levels, got " + holder + "!");
            freshInstance.info("Created new grid data");
            return freshInstance;
        }

        @SubscribeEvent
        public static void loadGrid(LevelEvent.Load evt) {
            LevelAccessor world = evt.getLevel();
            Grid.getUnsided(world).load();
        }

        @SubscribeEvent
        public static void tickGrid(LevelTickEvent.Post evt) {
            Level world = evt.getLevel();
            Grid.getUnsided(world).tick();
        }

        @SubscribeEvent
        public static void onChunkWatch(ChunkWatchEvent.Sent evt) {
            Level world = evt.getLevel();
            if(world.isClientSide) return;
            ServerGrid grid = Grid.server(world);
            grid.lookup().byChunk(evt.getPos()).forEach(pair -> {
                TransmitterType trns = pair instanceof ComponentLink<?> link ? link.getTransmitter() : null;
                GridAction.TASK_LINK_SYNC.broadcast(grid, new Object[] { pair.getStartID(), pair.getEndID(), trns });
            });
        }

        @SubscribeEvent
        public static void unloadGrid(LevelEvent.Unload evt) {
            LevelAccessor world = evt.getLevel();
            Grid.getUnsided(world).unload();
        }

        protected abstract void read(LevelReader world, CompoundTag contents, Provider provider);
        protected abstract void write(LevelReader world, CompoundTag contents, Provider provider);

        public static @NotNull Grid getUnsided(LevelReader world) {
            if(world.isClientSide()) return Grid.client(world);
            return Grid.server(world);
        }

        /**
         * Gets the sided grid data attachment from the provided level or player
         * @param world World to get data from
         * @return The ServerGrid attached to the provided level
         * @throws IllegalArgumentException if the provided world is not server-sided
         * @throws IllegalStateException if for whatever reason the data attachment's builder fails
         */
        public static ServerGrid server(LevelReader world) {
            Objects.requireNonNull(world);
            if(!(world instanceof ServerLevel sl)) throw new IllegalArgumentException("Can't acquire a server-sided dispatcher from non-server world " + world);
            if(Grid.weakServerGrid != null && Grid.weakServerGrid.isAttachedTo(sl))
                return Grid.weakServerGrid.get();
            Grid attachment = sl.getData(MechanoData.GRID.get());
            Grid.weakServerGrid = new WorldlyReference<ServerGrid>((ServerGrid)attachment);
            return Grid.weakServerGrid.get();
        }

        /**
         * Gets the sided grid data attachment from the provided level or player
         * @param player Player whose world will be used to look up the data attachment
         * @return The ServerGrid attached to the provided level
         * @throws IllegalArgumentException if the provided player's level is not server-sided
         */
        public static ServerGrid server(Player player) {
            return player == null ? null : Grid.server(player.level());
        }

        /**
         * Gets the sided grid data attachment from the provided level or player
         * @param world World to get data from
         * @return The ClientGrid attached to the provided level
         * @throws IllegalArgumentException if the provided world is not client-sided
         */
        @OnlyIn(Dist.CLIENT)
        public static ClientGrid client(LevelReader world) {
            Objects.requireNonNull(world);
            if(!(world instanceof ClientLevel cl)) throw new IllegalArgumentException("Can't acquire a client-sided dispatcher from server-sided world " + world);
            if(Grid.weakClientGrid != null && Grid.weakClientGrid.isAttachedTo(cl))
                return Grid.weakClientGrid.get();
            Grid attachment = cl.getData(MechanoData.GRID.get());
            if(attachment == null) throw new IllegalStateException("Failed to acquire a client-sided dispatcher in" + world);
            Grid.weakClientGrid = new WorldlyReference<ClientGrid>((ClientGrid)attachment);
            return Grid.weakClientGrid.get();
        }

        /**
         * Gets the sided grid data attachment from the provided level or player
         * @param player Player whose world will be used to look up the data attachment
         * @return The ClientGrid attached to the provided level
         * @throws IllegalArgumentException if the provided player's world is not client-sided
         */
        @OnlyIn(Dist.CLIENT)
        public static ClientGrid client(Player player) {
            return player == null ? null : Grid.client(player.level());
        }

        protected final Level world;

        protected Grid(Level world) {
            this.world = world;
        }

        protected abstract void load();
        protected abstract void unload();
        public abstract NetlistLookup<?> lookup();
        public abstract void tick();

        /**
         * Creates a new {@link ActionRunner} bound to this grid
         * @param action The action to assign to the runner
         * @return A new ActionRunner instance
         */
        public ActionRunner initiateTask(GridAction action) {
            if(action == null) throw new NullPointerException("Error running task from " + this + " - The provided task is null!");
            if(!action.isTask()) throw new IllegalArgumentException("Error running task from " + this + " - The provided action is not a task type!");
            return new ActionRunner(this, action);
        }

        // TODO replace with onAddedToGrid
        protected void markTerminus(@Nullable Griddable<?> source, boolean connections) {
            if(source == null) return;
            GriddableTerminus gt = source.getTerminus();
            if(gt == null) return;
            gt.setHasConnections(connections);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + getDimensionName() + "]";
        }

        @Override
        public Level getWorld() {
            return world;
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
