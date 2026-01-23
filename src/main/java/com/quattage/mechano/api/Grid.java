    package com.quattage.mechano.api;

    import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.api.grid.GridComponentTracker;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridAction.ActionRunner;
import com.quattage.mechano.foundation.WorldlyObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
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
        protected final Object2ObjectOpenHashMap<Griddable<?>, List<AncillaryPair>> links = new Object2ObjectOpenHashMap<>();

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

        public static void unloadGrid(LevelEvent.Unload evt) {
            LevelAccessor world = evt.getLevel();
            Grid.getUnsided(world).unload();
        }

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

        public GridAction addLink(AncillaryPair link) {
            Objects.requireNonNull(link);
            link.validateSelf();
            AncillaryPair linkInverted = link.flippedCopy();
            GridAction result = addLinkAsymmetric(link);
            GridAction inverted = addLinkAsymmetric(linkInverted);
            if(result.getActionType().indicatesFailure() || inverted.getActionType().indicatesFailure()) {
                removeLinkAsymmetric(GridComponentTracker.getSource(link.getStartNode()), link.getEndID());
                removeLinkAsymmetric(GridComponentTracker.getSource(linkInverted.getStartNode()), linkInverted.getEndID());
                // always consume the failure case should one exist
                if(!result.getActionType().indicatesFailure())
                    result = inverted;
            } else link.onAddedToGrid(this);
            return result;
        }

        private GridAction addLinkAsymmetric(AncillaryPair link) {
            Griddable<?> owner = GridComponentTracker.getSource(link.getStartNode());
            List<AncillaryPair> linksAt = getLinksBelongingTo(owner);
            if(linksAt == null) {
                linksAt = new ArrayList<AncillaryPair>();
                linksAt.add(link);
                links.put(owner, linksAt);
                return GridAction.RESPONSE_SUCCESS;
            }
            if(linksAt.contains(link)) return GridAction.RESPONSE_FAIL_DUPLICATE_ELEMENT;
            if(linksAt.size() >= AncillaryNode.MAX_SHARED_OCCUPANCY)
                return GridAction.RESPONSE_FAIL_ELEMENT_FULL;
            linksAt.add(link);
            return GridAction.RESPONSE_SUCCESS;
        }

        public GridAction removeLink(AncillaryPair link) {
            Objects.requireNonNull(link);
            link.validateSelf();
            GridAction result = removeLinkAsymmetric(GridComponentTracker.getSource(link.getStartNode()), link.getEndID());
            removeLinkAsymmetric(GridComponentTracker.getSource(link.getEndNode()), link.getStartID());
            return result;
        }

        private GridAction removeLinkAsymmetric(Griddable<?> source, ComponentUUID<?> endID) {
            List<AncillaryPair> linksAt = getLinksBelongingTo(source);
            if(linksAt == null) return GridAction.RESPONSE_FAIL_START_MISSING;
            int toRemove = -1;
            for(int x = 0; x < linksAt.size(); x++) {
                AncillaryPair link = linksAt.get(x);
                if(link.getEndID().equals(endID)) {
                    toRemove = x;
                    break;
                }
            }
            if(toRemove < 0) return GridAction.RESPONSE_FAIL_END_MISSING;
            linksAt.remove(toRemove);
            if(linksAt.isEmpty()) {
                links.remove(source);
                links.trim();
            }
            return GridAction.RESPONSE_SUCCESS;
        }

        public @Nullable List<AncillaryPair> getLinksBelongingTo(Griddable<?> source) {
            List<AncillaryPair> linksAt = links.get(source);
            if(linksAt == null) return null;
            if(linksAt.isEmpty()) {
                links.remove(source);
                return null;
            }
            return linksAt;
        }

        public int getLinkCount() {
            return links.size();
        }

        public String linksAsString() {
            if(links.isEmpty()) return "\n\tEmpty";
            String out = "";
            for(Map.Entry<Griddable<?>, List<AncillaryPair>> entry : links.entrySet()) {
                Griddable<?> source = entry.getKey();
                out += "\t- " + source.getClass().getSimpleName() + ":\n";
                for(AncillaryPair link : entry.getValue())
                    out += "\t\t* " + link + "\n";
                out = out.substring(0, out.length() - 1);
                out += "\n";
            }
            return out;
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
