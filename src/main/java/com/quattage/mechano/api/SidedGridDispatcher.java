package com.quattage.mechano.api;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.foundation.WorldReturnable;

import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * This class represents the link between the Minecraft world and the grid.
 * The Client and Server grids are instantiated as Data Attachments that belong
 * to the level.
 */
// @EventBusSubscriber
public abstract sealed class SidedGridDispatcher implements WorldReturnable permits ClientGrid, ServerGrid {
// these words aren't in the bible
    
    protected static final Logger LOGGER = LogUtils.getLogger();

    private static WorldlyReference<ServerGrid> weakServerGrid = null;
    private static WorldlyReference<ClientGrid> weakClientGrid = null;

    // /**
    //  * To be called by internal registries to populate the world with an initial data attachment
    //  * @param holder The world to attach to
    //  * @return The sided dispatcher instance that was attached
    //  * @throws IllegalArgumentException if the provided holder isn't compatable (for now, grids can only be attached to levels)
    //  */
    // @ApiStatus.Internal
    // public static SidedGridDispatcher createNew(IAttachmentHolder holder) {
    //     SidedGridDispatcher freshInstance = null;
    //     if(holder instanceof Level world) {
    //         if(world.isClientSide()) freshInstance = new ClientGrid(world);
    //         else freshInstance = new ServerGrid(world);
    //     }
    //     else throw new IllegalArgumentException("Mechano Grid Data can only be attached to levels, got " + holder + "!");
    //     freshInstance.info("Created new grid data");
    //     return freshInstance;
    // }

    // // /**
    // //  * Gets the sided grid data attachment from the provided level or player
    // //  * @param world World to get data from
    // //  * @return The ServerGrid attached to the provided level
    // //  * @throws IllegalArgumentException if the provided world is not server-sided
    // //  * @throws IllegalStateException if for whatever reason the data attachment's builder fails
    // //  */
    // // public static @Nullable ServerGrid server(LevelReader world) {
    // //     Objects.requireNonNull(world);
    // //     if(!(world instanceof ServerLevel sl)) throw new IllegalArgumentException("Can't acquire a server-sided dispatcher from non-server world " + world);
    // //     if(SidedGridDispatcher.weakServerGrid != null && SidedGridDispatcher.weakServerGrid.isAttachedTo(sl))
    // //         return SidedGridDispatcher.weakServerGrid.get();
    // //     SidedGridDispatcher attachment = sl.getData(MechanoData.GRID_ATTACHMENT.get());
    // //     SidedGridDispatcher.weakServerGrid = new WorldlyReference<ServerGrid>(attachment.asServer());
    // //     return SidedGridDispatcher.weakServerGrid.get();
    // // }

    // /**
    //  * Gets the sided grid data attachment from the provided level or player
    //  * @param player Player whose world will be used to look up the data attachment
    //  * @return The ServerGrid attached to the provided level
    //  * @throws IllegalArgumentException if the provided player's level is not server-sided
    //  */
    // public static @Nullable ServerGrid server(Player player) {
    //     return player == null ? null : SidedGridDispatcher.server(player.level());
    // }

    // /**
    //  * Gets the sided grid data attachment from the provided level or player
    //  * @param world World to get data from
    //  * @return The ClientGrid attached to the provided level
    //  * @throws IllegalArgumentException if the provided world is not client-sided
    //  */
    // @OnlyIn(Dist.CLIENT)
    // public static @Nullable ClientGrid client(LevelReader world) {
    //     Objects.requireNonNull(world);
    //     if(!(world instanceof ClientLevel cl)) throw new IllegalArgumentException("Can't acquire a client-sided dispatcher from server-sided world " + world);
    //     if(SidedGridDispatcher.weakClientGrid != null && SidedGridDispatcher.weakClientGrid.isAttachedTo(cl))
    //         return SidedGridDispatcher.weakClientGrid.get();
    //     SidedGridDispatcher attachment = cl.getData(MechanoData.GRID_ATTACHMENT.get());
    //     if(attachment == null) throw new IllegalStateException("Failed to acquire a client-sided dispatcher in" + world);
    //     SidedGridDispatcher.weakClientGrid = new WorldlyReference<ClientGrid>(attachment.asClient());
    //     return SidedGridDispatcher.weakClientGrid.get();
    // }

    // /**
    //  * Gets the sided grid data attachment from the provided level or player
    //  * @param player Player whose world will be used to look up the data attachment
    //  * @return The ClientGrid attached to the provided level
    //  * @throws IllegalArgumentException if the provided player's world is not client-sided
    //  */
    // @OnlyIn(Dist.CLIENT)
    // public static @Nullable ClientGrid client(Player player) {
    //     return player == null ? null : SidedGridDispatcher.client(player.level());
    // }

    protected final Level world;

    protected SidedGridDispatcher(Level world) {
        this.world = world;
    }

    @Override
    public Level getWorld() {
        return world;
    }

    public void info(String msg) {
        SidedGridDispatcher.LOGGER.info("(" + getDistPrefix() + ", " + getDimensionName() + ") " + msg);
    }

    public void warn(String msg) {
        SidedGridDispatcher.LOGGER.warn("(" + getDistPrefix() + ", " + getDimensionName() + ") " + msg);
    }

    @Override
    public String getDimensionName() {
        return world.dimension().location().toString();
    }

    public ServerGrid asServer() {
        if(this instanceof ServerGrid grid) return grid;
        throw new RuntimeException("Can't get the ServerGrid instance as a client!");
    }

    @OnlyIn(Dist.CLIENT)
    public ClientGrid asClient() {
        if(this instanceof ClientGrid grid) return grid;
        throw new RuntimeException("Can't get the ClientGrid instance as a server!");
    }
    
    protected abstract @Nullable ListTag writeAll();
    protected abstract String getDistPrefix();
    protected abstract void onLoad();
    protected abstract void onUnload();
    protected abstract void tick();

}
