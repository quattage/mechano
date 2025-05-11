package com.quattage.mechano.foundation.api;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.MechanoDataAttachments;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

public abstract class SidedGridDispatcher {

    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final SidedGridDispatcher.Serializer SERIALIZER = new SidedGridDispatcher.Serializer();

    // these will be GCd immediately if they aren't actively reachable
    private static WorldlyReference<GlobalServerGrid> weakServerGrid = new WorldlyReference<>(null);
    private static WorldlyReference<GlobalClientGrid> weakClientGrid = new WorldlyReference<>(null);

    protected final Level world;

    /**
     * To be called by internal registries to populate the world with an initial data attachment
     * @param holder The world to attach to
     * @return The sided dispatcher instance that was attached
     * @throws IllegalArgumentException if the provided holder isn't compatable (for now, grids can only be attached to levels)
     */
    public static SidedGridDispatcher createNew(IAttachmentHolder holder) {
        SidedGridDispatcher freshInstance = null;
        if(holder instanceof ClientLevel cl)
            freshInstance = new GlobalClientGrid(cl);
        else if(holder instanceof ServerLevel sl)
            freshInstance = new GlobalServerGrid(sl, new ObjectArrayList<>());
        else throw new IllegalArgumentException("Mechano Grid Data can only be attached to levels, got " + holder + "!");
        freshInstance.log("Created new grid data");
        return freshInstance;
    }

    /**
     * Gets the sided grid data attachment from the provided level or player
     * @param world World to get data from
     * @return The GlobalServerGrid attached to the provided level
     * @throws IllegalArgumentException if the provided world is not server-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static @Nullable GlobalServerGrid server(LevelReader world) {
        Objects.requireNonNull(world);
        if(!(world instanceof ServerLevel sl)) throw new IllegalArgumentException("Can't acquire a server-sided dispatcher from non-server world " + world);
        if(weakServerGrid != null && weakServerGrid.isAttachedTo(sl))
            return weakServerGrid.get();
        SidedGridDispatcher attachment = sl.getData(MechanoDataAttachments.GRID_ATTACHMENT.get());
        weakServerGrid = new WorldlyReference<GlobalServerGrid>(attachment.asServer());
        return weakServerGrid.get();
    }

    /**
     * Gets the sided grid data attachment from the provided level or player
     * @param player Player whose world will be used to look up the data attachment
     * @return The GlobalServerGrid attached to the provided level
     * @throws IllegalArgumentException if the provided player's level is not server-sided
     */
    public static @Nullable GlobalServerGrid server(Player player) {
        return player == null ? null : server(player.level());
    }

    /**
     * Runs the provided consumer on the GlobalServerGrid attached to the provided world
     * @param world World to get data from
     * @param cons Consumer to run with the GlobalServerGrid instance
     * @throws IllegalArgumentException if the provided world is not server-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnServer(Level world, Consumer<GlobalServerGrid> cons) {
        GlobalServerGrid grid = SidedGridDispatcher.server(world);
        if(grid == null) return false;
        cons.accept(grid);
        return true;
    }

    /**
     * Runs the provided consumer on the GlobalServerGrid attached to the provided world
     * @param world World to get data from
     * @param cons Consumer to run with the GlobalServerGrid instance
     * @throws IllegalArgumentException if the provided world is not server-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnServer(LevelReader world, Consumer<GlobalServerGrid> cons) {
        GlobalServerGrid grid = SidedGridDispatcher.server(world);
        if(grid == null) return false;
        cons.accept(grid);
        return true;
    }

    /**
     * Runs the provided consumer on the GlobalServerGrid attached to the provided world
     * @param player Player whose world will be used to look up the data attachment
     * @param cons Consumer to run with the GlobalServerGrid instance
     * @throws IllegalArgumentException if the provided player's level is not server-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnServer(Player player, Consumer<GlobalServerGrid> cons) {
        GlobalServerGrid grid = SidedGridDispatcher.server(player);
        if(grid == null) return false;
        cons.accept(grid);
        return true;
    }

    /**
     * Gets the sided grid data attachment from the provided level or player
     * @param world World to get data from
     * @return The GlobalClientGrid attached to the provided level
     * @throws IllegalArgumentException if the provided world is not client-sided
     */
    public static @Nullable GlobalClientGrid client(LevelReader world) {
        Objects.requireNonNull(world);
        if(!(world instanceof ClientLevel cl)) throw new IllegalArgumentException("Can't acquire a client-sided dispatcher from server-sided world " + world);
        if(weakClientGrid != null && weakClientGrid.isAttachedTo(cl))
            return weakClientGrid.get();
        SidedGridDispatcher attachment = cl.getData(MechanoDataAttachments.GRID_ATTACHMENT.get());
        if(attachment == null) throw new IllegalStateException("Failed to acquire a client-sided dispatcher in" + world);
        weakClientGrid = new WorldlyReference<GlobalClientGrid>(attachment.asClient());
        return weakClientGrid.get();
    }

    /**
     * Gets the sided grid data attachment from the provided level or player
     * @param player Player whose world will be used to look up the data attachment
     * @return The GlobalClientGrid attached to the provided level
     * @throws IllegalArgumentException if the provided world is not client-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static @Nullable GlobalClientGrid client(Player player) {
        return player == null ? null : client(player.level());
    }

    /**
     * Runs the provided consumer on the GlobalClientGrid attached to the provided world
     * @param player Player whose world will be used to look up the data attachment
     * @param cons Consumer to run with the GlobalClientGrid instance
     * @throws IllegalArgumentException if the provided player's level is not client-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnClient(Level world, Consumer<GlobalClientGrid> cons) {
        GlobalClientGrid grid = client(world);
        if(grid == null) return false;
        cons.accept(grid);
        return true;
    }

    /**
     * Runs the provided consumer on the GlobalClientGrid attached to the provided world
     * @param player Player whose world will be used to look up the data attachment
     * @param cons Consumer to run with the GlobalClientGrid instance
     * @throws IllegalArgumentException if the provided player's level is not client-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnClient(LevelReader world, Consumer<GlobalClientGrid> cons) {
        GlobalClientGrid grid = client(world);
        if(grid == null) return false;
        cons.accept(grid);
        return true;
    }
    
    /**
     * Runs the provided consumer on the GlobalClientGrid attached to the provided world
     * @param world World to get data from
     * @param cons Consumer to run with the GlobalClientGrid instance
     * @throws IllegalArgumentException if the provided world is not client-sided
     * @throws IllegalStateException if for whatever reason the data attachment's builder fails
     */
    public static boolean runOnClient(Player player, Consumer<GlobalClientGrid> cons) {
        GlobalClientGrid grid = client(player);
        if(grid == null) return false;
        cons.accept(grid);
        return true;
    }

    protected SidedGridDispatcher(Level world) {
        this.world = world;
    }

    public Level getWorld() {
        return world;
    }

    public void log(String msg) {
        LOGGER.info("(" + getDistPrefix() + ", " + getDimensionName() + ") " + msg);
    }

    public String getDimensionName() {
        return world.dimension().location().toString();
    }

    public LevelReader getLevelReader() {
        return (LevelReader) world;
    }

    public GlobalServerGrid asServer() {
        if(this instanceof GlobalServerGrid grid) return grid;
        throw new RuntimeException("Can't get the GlobalServerGrid instance as a client!");
    }

    public GlobalClientGrid asClient() {
        if(this instanceof GlobalClientGrid grid) return grid;
        throw new RuntimeException("Can't get the GlobalClientGrid instance as a server!");
    }

    protected abstract @Nullable ListTag writeAll();

    protected abstract String getDistPrefix();

    public static class Serializer implements IAttachmentSerializer<ListTag, SidedGridDispatcher> {

        @Override
        public SidedGridDispatcher read(IAttachmentHolder holder, ListTag list, Provider provider) {
            SidedGridDispatcher deserializedInstance = null;
            if(holder instanceof ClientLevel cl)
                deserializedInstance = GlobalClientGrid.loadFrom(list, cl);
            else if(holder instanceof ServerLevel sl)
                deserializedInstance = GlobalServerGrid.loadFrom(list, sl);
            else throw new IllegalArgumentException("Mechano Grid Data can only be attached to levels, got " + holder + "!");
            deserializedInstance.log("Loaded pre-existing grid data");
            return deserializedInstance;
        }

        @Override
        public ListTag write(SidedGridDispatcher attachment, Provider provider) {
            return attachment.writeAll();
        }
    }


    protected static class WorldlyReference<T extends SidedGridDispatcher> extends WeakReference<T> {

        public WorldlyReference(T referent) {
            super(referent);
        }

        protected boolean isAttachedTo(Level world) {
            if(refersTo(null)) return false;
            if(get().world == world) return true;
            return (world.isClientSide == get().world.isClientSide) && world.dimension().compareTo(get().world.dimension()) == 0;
        }
    }
}
