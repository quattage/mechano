package com.quattage.mechano.foundation.api.grid;

import java.lang.ref.WeakReference;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.MechanoDataAttachments;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

public abstract class SidedGridDispatcher {

    protected static final Logger LOGGER = LogUtils.getLogger();
    public static final SidedGridDispatcher.Serializer SERIALIZER = new SidedGridDispatcher.Serializer();
    public static final TransferProtocolRegistry PROTOCOLS = new TransferProtocolRegistry();

    private static @Nullable WorldlyReference<GlobalServerGrid> weakServerGrid;
    private static @Nullable WorldlyReference<GlobalClientGrid> weakClientGrid;

    protected final Level world;

    public static GlobalServerGrid getServerGrid(LevelReader world) {
        if(!(world instanceof ServerLevel sl)) return null;
        if(weakServerGrid != null && (!weakServerGrid.refersTo(null)) && weakServerGrid.is(sl))
            return weakServerGrid.get();
        SidedGridDispatcher attachment = sl.getData(MechanoDataAttachments.GRID_DATA.get());
        if(attachment == null) return null;
        weakServerGrid = new WorldlyReference<GlobalServerGrid>(attachment.asServer());
        return weakServerGrid.get();
    }

    public static GlobalClientGrid getClientGrid(LevelReader world) {
        if(!(world instanceof ClientLevel cl)) return null;
        if(weakClientGrid != null && (!weakClientGrid.refersTo(null)) && weakClientGrid.is(cl))
            return weakClientGrid.get();
        SidedGridDispatcher attachment = cl.getData(MechanoDataAttachments.GRID_DATA.get());
        if(attachment == null) return null;
        weakClientGrid = new WorldlyReference<GlobalClientGrid>(attachment.asClient());
        return weakClientGrid.get();
    }

    public static SidedGridDispatcher createNew(IAttachmentHolder holder) {
        SidedGridDispatcher freshInstance = null;
        if(holder instanceof ClientLevel cl)
            freshInstance = new GlobalClientGrid(cl);
        else if(holder instanceof ServerLevel sl)
            freshInstance = new GlobalServerGrid(sl);
        else throw new IllegalArgumentException("Mechano Grid Data can only be attached to levels, got " + holder + "!");
        freshInstance.log("Created new grid data");
        return freshInstance;
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
        throw new RuntimeException("Can't get the server GlobalServerGrid instance as a client!");
    }

    public GlobalClientGrid asClient() {
        if(this instanceof GlobalClientGrid grid) return grid;
        throw new RuntimeException("Can't get the server GlobalClientGrid instance as a server!");
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

        protected boolean is(Level world) {
            return (world.isClientSide == get().world.isClientSide) && world.dimension().compareTo(get().world.dimension()) == 0;
        }
    }
}
