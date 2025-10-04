package com.quattage.mechano.foundation.api;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.catenary.CatenaryModel;
import com.quattage.mechano.foundation.api.entity.GriddableEntityAttachment;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection.InsertionPolicy;
import com.quattage.mechano.foundation.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.api.switchboard.LinkResponsePacket;
import com.quattage.mechano.foundation.helper.Worldly;
import com.quattage.mechano.infrastructure.manifest.GridManifestGenerator;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * This class represents the link between the Minecraft world and the grid.
 * The Client and Server grids are instantiated as Data Attachments that belong
 * to the level.
 */
@EventBusSubscriber
public abstract sealed class SidedGridDispatcher implements Worldly permits ClientGrid, ServerGrid {
// these words aren't in the bible
    
    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final GridManifestGenerator MANIFEST = new GridManifestGenerator();

    private static WorldlyReference<ServerGrid> weakServerGrid = null;
    private static WorldlyReference<ClientGrid> weakClientGrid = null;

    public static final IAttachmentSerializer<ListTag, SidedGridDispatcher> 
        SERIALIZER = new IAttachmentSerializer<>() {
            @Override
            public SidedGridDispatcher read(IAttachmentHolder holder, ListTag list, HolderLookup.Provider provider) {
                SidedGridDispatcher deserializedInstance = null;
                if(holder instanceof ClientLevel cl) deserializedInstance = ClientGrid.loadFrom(list, cl);
                else if(holder instanceof ServerLevel sl) deserializedInstance = ServerGrid.loadFrom(list, sl);
                else throw new IllegalArgumentException("Mechano Grid Data can only be attached to levels, got '" + holder.getClass().getSimpleName() + "!'");
                deserializedInstance.info("Loaded pre-existing grid data");
                return deserializedInstance;
            }
            @Override
            public ListTag write(SidedGridDispatcher attachment, HolderLookup.Provider provider) {
                return attachment.writeAll();
            }
        };

    protected final Level world;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post evt) {
        MANIFEST.tick(); 
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Pre evt) {
        SidedGridDispatcher grid = evt.getLevel().getExistingDataOrNull(MechanoData.GRID_ATTACHMENT);
        if(grid == null) return;
        grid.tick();
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load evt) {
        if(!(evt.getLevel() instanceof Level world)) return;
        SidedGridDispatcher grid = world.getExistingDataOrNull(MechanoData.GRID_ATTACHMENT);
        if(grid == null) return;
        grid.onLoad();
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload evt) {
        if(evt.getLevel() instanceof ServerLevel sl) {
            if(weakServerGrid != null && weakServerGrid.isAttachedTo(sl)) {
                LOGGER.debug("Dumped ServerGrid belonging to '" + weakServerGrid.get().getDimensionName() + "'");
                weakServerGrid.clear();
            }
        } else if(evt.getLevel() instanceof ClientLevel cl) {
            if(weakClientGrid != null && weakClientGrid.isAttachedTo(cl)) {
                LOGGER.debug("Dumped ClientGrid belonging to '" + weakClientGrid.get().getDimensionName() + "'");
                weakClientGrid.clear();
            } 
        }
        SidedGridDispatcher grid = ((Level)evt.getLevel()).getExistingDataOrNull(MechanoData.GRID_ATTACHMENT);
        if(grid != null) grid.onUnload();
    }

    @SubscribeEvent
    public static void onChunkWatched(ChunkWatchEvent.Sent evt) {
        LinkDataStorage.ServerSectionable storage = LinkDataStorage.getAsServer(evt.getLevel(), evt.getPos(), false);
        if(storage == null) return;
        storage.forEach(link -> {
            if(!link.isBeingTrackedBy(evt.getPlayer(), InsertionPolicy.SINGLE)) return;
            CatnipServices.NETWORK.sendToClient(
                evt.getPlayer(), LinkResponsePacket.of(link, GridResponse.TASK_SYNC_ANCHORS));
        });
    }

    @SubscribeEvent
    public static void onChunkUnwatched(ChunkWatchEvent.UnWatch evt) {
        LinkDataStorage.ServerSectionable storage = LinkDataStorage.getAsServer(evt.getLevel(), evt.getPos(), false);
        if(storage == null) return;
        storage.forEach(link -> {
            if(!link.isBeingTrackedBy(evt.getPlayer(), InsertionPolicy.SINGLE)) return;
            CatnipServices.NETWORK.sendToClient(
                evt.getPlayer(), LinkResponsePacket.of(link, GridResponse.TASK_DESTROY_LINK_LAZY));
        });
    }

    @SubscribeEvent
    public static void onEntityWatched(PlayerEvent.StartTracking evt) {
        LinkDataStorage.Server storage = LinkDataStorage.getAsServer(evt.getTarget(), false);
        if(storage == null) return;
        // storage.forEach(link -> {
        //     if(!link.isBeingTrackedBy((ServerPlayer)evt.getEntity(), InsertionPolicy.SYMMETRIC)) return;
        //     CatnipServices.NETWORK.sendToClient(
        //         (ServerPlayer)evt.getEntity(), LinkResponsePacket.of(
        //             link.getStartNode(), link.getEndNode(), 
        //             link.getTransmitter(),
        //             GridResponse.TASK_SYNC_ANCHORS
        //         ));
        // });
    }

    @SubscribeEvent
    public static void onEntityUnwatched(PlayerEvent.StopTracking evt) {
        LinkDataStorage.Server storage = LinkDataStorage.getAsServer(evt.getTarget(), false);
        if(storage == null) return;
        // storage.forEach(link -> {
        //     CatnipServices.NETWORK.sendToClient(
        //         (ServerPlayer)evt.getEntity(), LinkResponsePacket.of(
        //             link.getStartNode(), link.getEndNode(), 
        //             link.getTransmitter(),
        //             GridResponse.TASK_FORGET_ANCHORS
        //         ));
        // });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent evt) {
        if(evt.getEntity().level().isClientSide()) return;
        Griddable<?> points = GriddableEntityAttachment.of(evt.getEntity(), false);
        if(points == null) return;
        points.destroySurrogate();
    }

    /**
     * This is the hook where {@link CatenaryModel} instances get rendered
     * to chunks if both ends of said model are attached to immovable,
     * voxel-adjacent elements. This event collects links attached to
     * the section via the registered {@link LinkDataStorage data attachment}.
     */
    @SubscribeEvent
    public static void onSectionMeshed(AddSectionGeometryEvent evt) {
        SectionPos pos = SectionPos.of(evt.getSectionOrigin());
        ClientLevel world = (ClientLevel)evt.getLevel(); 
        LinkDataStorage.ClientSectionable storage = LinkDataStorage.getAsClient(world.getChunk(pos.getX(), pos.getZ()), false);
        if(storage == null) return;
        LinkDataStorage.Client section = storage.getStorageInSection(pos.getY());
        if(section == null) return;
        evt.addRenderer(ctx -> GridCatenary.renderToSection(world, pos, evt.getSectionOrigin(), section.getAll(), ctx));
    }

    /**
     * To be called by internal registries to populate the world with an initial data attachment
     * @param holder The world to attach to
     * @return The sided dispatcher instance that was attached
     * @throws IllegalArgumentException if the provided holder isn't compatable (for now, grids can only be attached to levels)
     */
    @ApiStatus.Internal
    public static SidedGridDispatcher createNew(IAttachmentHolder holder) {
        SidedGridDispatcher freshInstance = null;
        if(holder instanceof ClientLevel cl)
            freshInstance = new ClientGrid(cl);
        else if(holder instanceof ServerLevel sl)
            freshInstance = new ServerGrid(sl, new ObjectArrayList<>());
        else throw new IllegalArgumentException("Mechano Grid Data can only be attached to levels, got " + holder + "!");
        freshInstance.info("Created new grid data");
        return freshInstance;
    }

    /**
     * Gets the sided grid data attachment from the provided level or player
     * @param world World to get data from
     * @return The ServerGrid attached to the provided level
     * @throws IllegalArgumentException if the provided world is not server-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static @Nullable ServerGrid server(LevelReader world) {
        Objects.requireNonNull(world);
        if(!(world instanceof ServerLevel sl)) throw new IllegalArgumentException("Can't acquire a server-sided dispatcher from non-server world " + world);
        if(weakServerGrid != null && weakServerGrid.isAttachedTo(sl))
            return weakServerGrid.get();
        SidedGridDispatcher attachment = sl.getData(MechanoData.GRID_ATTACHMENT.get());
        weakServerGrid = new WorldlyReference<ServerGrid>(attachment.asServer());
        return weakServerGrid.get();
    }

    /**
     * Gets the sided grid data attachment from the provided level or player
     * @param player Player whose world will be used to look up the data attachment
     * @return The ServerGrid attached to the provided level
     * @throws IllegalArgumentException if the provided player's level is not server-sided
     */
    public static @Nullable ServerGrid server(Player player) {
        return player == null ? null : server(player.level());
    }

    /**
     * Runs the provided consumer on the ServerGrid attached to the provided world
     * @param world World to get data from
     * @param cons Consumer to run with the ServerGrid instance
     * @throws NullPointerException if any of the provied fields are null
     * @throws IllegalArgumentException if the provided world is not server-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnServer(Level world, Consumer<ServerGrid> cons) {
        ServerGrid grid = SidedGridDispatcher.server(world);
        if(grid == null) return false;
        try { cons.accept(grid); }
        catch(Exception e) {
            Mechano.LOGGER.error("Error encountered while executing server-sided grid action");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Runs the provided consumer on the ServerGrid attached to the provided world
     * @param world World to get data from
     * @param cons Consumer to run with the ServerGrid instance
     * @throws NullPointerException if any of the provied fields are null
     * @throws IllegalArgumentException if the provided world is not server-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnServer(LevelReader world, Consumer<ServerGrid> cons) {
        Objects.requireNonNull(cons);
        ServerGrid grid = SidedGridDispatcher.server(world);
        if(grid == null) return false;
        try { cons.accept(grid); }
        catch(Exception e) {
            Mechano.LOGGER.error("Error encountered while executing server-sided grid action");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Runs the provided consumer on the ServerGrid attached to the provided world
     * @param player Player whose world will be used to look up the data attachment
     * @param cons Consumer to run with the ServerGrid instance
     * @throws NullPointerException if any of the provied fields are null
     * @throws IllegalArgumentException if the provided world is not server-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnServer(Player player, Consumer<ServerGrid> cons) {
        Objects.requireNonNull(cons);
        ServerGrid grid = SidedGridDispatcher.server(player);
        if(grid == null) return false;
        try { cons.accept(grid); }
        catch(Exception e) {
            Mechano.LOGGER.error("Error encountered while executing server-sided grid action");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Gets the sided grid data attachment from the provided level or player
     * @param world World to get data from
     * @return The ClientGrid attached to the provided level
     * @throws IllegalArgumentException if the provided world is not client-sided
     */
    public static @Nullable ClientGrid client(LevelReader world) {
        Objects.requireNonNull(world);
        if(!(world instanceof ClientLevel cl)) throw new IllegalArgumentException("Can't acquire a client-sided dispatcher from server-sided world " + world);
        if(weakClientGrid != null && weakClientGrid.isAttachedTo(cl))
            return weakClientGrid.get();
        SidedGridDispatcher attachment = cl.getData(MechanoData.GRID_ATTACHMENT.get());
        if(attachment == null) throw new IllegalStateException("Failed to acquire a client-sided dispatcher in" + world);
        weakClientGrid = new WorldlyReference<ClientGrid>(attachment.asClient());
        return weakClientGrid.get();
    }

    /**
     * Gets the sided grid data attachment from the provided level or player
     * @param player Player whose world will be used to look up the data attachment
     * @return The ClientGrid attached to the provided level
     * @throws IllegalArgumentException if the provided player's world is not client-sided
     */
    public static @Nullable ClientGrid client(Player player) {
        return player == null ? null : client(player.level());
    }

    /**
     * Runs the provided consumer on the ClientGrid attached to the provided world
     * @param player Player whose world will be used to look up the data attachment
     * @param cons Consumer to run with the ClientGrid instance
     * @throws NullPointerException if any of the provied fields are null
     * @throws IllegalArgumentException if the provided world is not client-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnClient(Level world, Consumer<ClientGrid> cons) {
        Objects.requireNonNull(cons);
        ClientGrid grid = client(world);
        if(grid == null) return false;
        try { cons.accept(grid); }
        catch(Exception e) {
            Mechano.LOGGER.error("Error encountered while executing client-sided grid action");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Runs the provided consumer on the ClientGrid attached to the provided world
     * @param player Player whose world will be used to look up the data attachment
     * @param cons Consumer to run with the ClientGrid instance
     * @throws NullPointerException if any of the provied fields are null
     * @throws IllegalArgumentException if the provided world is not client-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnClient(LevelReader world, Consumer<ClientGrid> cons) {
        Objects.requireNonNull(cons);
        ClientGrid grid = client(world);
        if(grid == null) return false;
        try { cons.accept(grid); }
        catch(Exception e) {
            Mechano.LOGGER.error("Error encountered while executing client-sided grid action");
            e.printStackTrace();
        }
        return true;
    }
    
    /**
     * Runs the provided consumer on the ClientGrid attached to the provided world
     * @param world World to get data from
     * @param cons Consumer to run with the ClientGrid instance
     * @throws NullPointerException if any of the provied fields are null
     * @throws IllegalArgumentException if the provided world is not client-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnClient(Player player, Consumer<ClientGrid> cons) {
        Objects.requireNonNull(cons);
        ClientGrid grid = client(player);
        if(grid == null) return false;
        try { cons.accept(grid); }
        catch(Exception e) {
            Mechano.LOGGER.error("Error encountered while executing client-sided grid action");
            e.printStackTrace();
        }
        return true;
    }

    protected SidedGridDispatcher(Level world) {
        this.world = world;
    }

    @Override
    public Level getWorld() {
        return world;
    }

    public void info(String msg) {
        LOGGER.info("(" + getDistPrefix() + ", " + getDimensionName() + ") " + msg);
    }

    public void warn(String msg) {
        LOGGER.warn("(" + getDistPrefix() + ", " + getDimensionName() + ") " + msg);
    }

    @Override
    public String getDimensionName() {
        return world.dimension().location().toString();
    }

    public ServerGrid asServer() {
        if(this instanceof ServerGrid grid) return grid;
        throw new RuntimeException("Can't get the ServerGrid instance as a client!");
    }

    public ClientGrid asClient() {
        if(this instanceof ClientGrid grid) return grid;
        throw new RuntimeException("Can't get the ClientGrid instance as a server!");
    }
    
    protected LinkDataTracker getDebugTracker() { return null; }
    protected abstract @Nullable ListTag writeAll();
    protected abstract String getDistPrefix();
    protected abstract void onLoad();
    protected abstract void onUnload();
    protected abstract void tick();

}
