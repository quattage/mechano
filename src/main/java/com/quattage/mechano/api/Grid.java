    package com.quattage.mechano.api;

    import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.Circuit;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.ComponentLink;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryNode;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.api.switchboard.action.GridAction.ActionRunner;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.MechanoRegistrate;
import com.quattage.mechano.foundation.WorldlyObject;
import com.quattage.mechano.foundation.tracking.GridIdentifiable;
import com.quattage.mechano.foundation.tracking.GridUUID;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

    /**
     * This class represents the link between the Minecraft world and the grid.
     * The Client and Server grids are instantiated as Data Attachments that belong
     * to the level.
     */
    // @EventBusSubscriber
    public abstract sealed class Grid implements WorldlyObject permits ClientGrid, ServerGrid {
    // these words aren't in the bible
        
        protected static final Logger LOGGER = LogUtils.getLogger();

        // weakly referenced singletons are stored to skip hash capability lookups
        private static WorldlyReference<ServerGrid> weakServerGrid = null;
        private static WorldlyReference<ClientGrid> weakClientGrid = null;

        protected final Object2ObjectOpenHashMap<GridUUID, List<ComponentLink<?>>> links = new Object2ObjectOpenHashMap<>();

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

        @Override
        public Level getWorld() {
            return world;
        }

        public void info(String msg) {
            Grid.LOGGER.info("(" + getDistPrefix() + ", " + getDimensionName() + ") " + msg);
        }

        public void warn(String msg) {
            Grid.LOGGER.warn("(" + getDistPrefix() + ", " + getDimensionName() + ") " + msg);
        }

        @Override
        public String getDimensionName() {
            return world.dimension().location().toString();
        }

        protected abstract String getDistPrefix();
        protected abstract void onLoad();
        protected abstract void onUnload();
        protected abstract void tick();

        public abstract void addComponent(Griddable<?> source);
        public abstract CircuitComponent popComponent(GridIdentifiable<?> obj);
        public abstract int getComponentCount();

        public boolean removeLinksBelongingTo(Griddable<?> source) {
            Objects.requireNonNull(source);
            if(source.getTerminus().isEmpty()) return false;
            int preSize = getLinkCount();
            source.getTerminus().forEach(node -> { removeLinksBelongingTo(node); });
            return preSize > getLinkCount();
        }

        public boolean addLink(ComponentLink<?> link) {
            Objects.requireNonNull(link);
            if(!link.hasUUIDs()) throw new IllegalArgumentException("Failed while adding link - The provided link doesn't have a start and/or end ID configured!");
            link.findJacksSafe(this);
            List<ComponentLink<?>> linksAt = links.get(link.getStart());
            if(linksAt == null || linksAt.isEmpty()) {
                linksAt = new ArrayList<ComponentLink<?>>();
                linksAt.add(link);
                links.put(link.getStart(), linksAt);
                return true;
            }
            if(linksAt.contains(link)) return false;
            if(linksAt.size() >= AncillaryNode.MAX_SHARED_OCCUPANCY) {
                warn("Skipped adding " + link + " because this UUID has already reached the maximum number of shared components.");
                return false;
            }
            linksAt.add(link);
            return true;
        }

        public boolean removeLink(ComponentLink<?> link) {
            Objects.requireNonNull(link);
            if(!link.hasUUIDs()) throw new IllegalArgumentException("Failed while removing link - The provided link doesn't have a start and/or end ID configured!");
            List<ComponentLink<?>> linksAt = links.get(link.getStart());
            if(linksAt == null || linksAt.isEmpty()) return false;
            boolean removed = linksAt.remove(link);
            if(!removed) return false;
            if(linksAt.isEmpty()) {
                links.remove(link.getStart());
                links.trim();
            }
            return true;
        }

        public boolean removeLinksBelongingTo(AncillaryNode node) {
            Objects.requireNonNull(node);
            if(node.getSource() == null) {
                Mechano.LOGGER.warn("Skipped removing links belonging to " + node + " - This node is not bound to a valid griddable.");
                return false;
            }
            GridUUID addr = getAddressFor(node.getSource(), node);
            List<ComponentLink<?>> linksAt = links.remove(addr);
            if(linksAt == null) return false;
            links.trim();
            return true;
        }

        public List<ComponentLink<?>> getLinksBelongingTo(GridIdentifiable<?> obj) {
            Objects.requireNonNull(obj);
            return links.get(obj);
        }

        public int getLinkCount() {
            return links.size();
        }

        /**
         * Acquires a {@link GridUUID} instance pointing to <code>source</code>
         * and bound to the default {@link CircuitComponent} returned by 
         * the provided {@link Griddable}
         * @param source
         * @return A (newly instantiated or cachced) GridUUID instance. 
         * While modification is allowed, it is not reccomended.
         * @see {@link Griddable#getAddress()}
         */
        public GridUUID getAddressFor(Griddable<?> obj) {
            Objects.requireNonNull(obj);
            CircuitComponent component = obj.getCircuit();
            return getAddressFor(obj, component);
        }

        /**
         * Acquires a {@link GridUUID} instance pointing to <code>source</code>
         * and bound to the supplied {@link CircuitComponent} <code>component</code>.
         * <h3>There is no error checking to ensure that the supplied <code>component</code>
         * belongs to some construct attached to <code>source</code>. For the UUID to be useful,
         * you need to garantee this yourself.</h3>
         * @param source The {@link Griddable} to pull a UUID instance from
         * @param component The particular {@link CircuitComponent} that the returned UUID will be bound to, 
         * provided it belongs to <code>source</code>
         * @return A (newly instantiated or cachced) GridUUID instance. 
         * While modification is allowed, it is not reccomended.
         * @see {@link Griddable#getAddress()}
         * @see {@link CircuitComponent#bindUUID}
         */
        public GridUUID getAddressFor(GridIdentifiable<?> obj, CircuitComponent component) {
            Objects.requireNonNull(obj);
            Objects.requireNonNull(component);
            GridUUID id = obj.getUUIDSafe();
            if(id == null) {
                throw new NullPointerException("Couldn't get address for " + obj
                    + " - This source Griddable<?>instance returned a null address!");
            }
            GridUUID boundID = component.bindUUID(id);
            if(boundID == null) {
                throw new NullPointerException("Couldn't bind address to " + component
                    + " - This component didn't return a modified UUID instance!");
            }
            if(boundID != id) {
                throw new IllegalStateException("Couldn't bind address to " + component
                    + " - This component returned a new ID instance!");
            }
            return boundID;
        }

        public @Nullable CircuitComponent findComponent(GridUUID address) {
            Objects.requireNonNull(address);
            Griddable<?>source = address.getTargetSource(this);
            if(source == null) {
                warn("Couldn't acquire component from " + address + " No source at this address could be found!");
                return null;
            }
            CircuitComponent component = source.getCircuit();
            if(component == null)
                throw new NullPointerException("Failed while querying for component at " + address + " - The source (" + source + ") failed to provide a valid CircuitComponent instance!");
            if(component.getType() == address.getType()) {
                if(address.hasBindings()) {
                    warn("Acquired CircuitComponent, but " + address + " has extraneous bindings that were ignored.");
                    address.withBinding(address.getType(), -1, -1);
                }
                return component;
            }
            if(!(component instanceof Circuit circuit)) {
                warn("Couldn't acqiure component from " + address + " - The source (" 
                    + source.getClass().getSimpleName() + ") supplied a " + component.getClass().getSimpleName() 
                    + ", which doesn't contain any sub-components with the correct binding.");
                return null;
            }
            return address.getType().findTarget(this, circuit, address);
        }

        public boolean isLoaded(GridIdentifiable<?> obj) {
            Griddable<?> source = obj.getUUIDSafe().getTargetSource(this);
            return source != null && source.getCircuit() != null;
        }

        public TransmitterType<?> getTransmitterAt(ResourceLocation loc) {
            Registry<TransmitterType<?>> registry = world.registryAccess().registryOrThrow(MechanoRegistrate.TRANSMITTER_KEY);
            TransmitterType<?> trns = registry.get(loc); 
            if(trns == null) throw new IllegalArgumentException("Couldn't find TransmitterType entry at " + loc);
            return trns;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + getDistPrefix() + ", " + getDimensionName() + "]";
        }

        public ActionRunner initiateTask(GridAction action) {
            if(action == null) throw new NullPointerException("Error running task from " + this + " - The provided task is null!");
            if(!action.isTask()) throw new IllegalArgumentException("Error running task from " + this + " - The provided action is not a task type!");
            return new ActionRunner(this, action);
        }
    }
