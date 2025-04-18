package com.quattage.mechano.foundation.api.grid;

import java.util.Iterator;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;


/**
 * A GridDispatcher is the top-level manager for {@link PowerGrid PowerGrids}.
 * Data Attachments are leveraged to add one GridDispatcher to each level/dimension.
 */
public class GridDispatcher {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final TransferProtocolRegistry PROTOCOLS = new TransferProtocolRegistry();
    public static final GridDispatcher.Serializer SERIALIZER = new GridDispatcher.Serializer();

    private final ServerLevel world;
    private ObjectArrayList<PowerGrid> subgrids;

    public static GridDispatcher loadFrom(ListTag serializedGlobals, ServerLevel world) {
        GridDispatcher freshInstance = new GridDispatcher(world);
        ObjectArrayList<PowerGrid> deserialized = new ObjectArrayList<>(serializedGlobals.size());
        for(int x = 0; x < serializedGlobals.size(); x++) {
            ListTag serializedSubgrid = serializedGlobals.getList(x);
            PowerGrid grid = PowerGrid.loadFrom(freshInstance, freshInstance.getLevelReader(), serializedSubgrid);
            deserialized.add(grid);
        }
        freshInstance.subgrids = deserialized;
        return freshInstance;
    }

    private GridDispatcher(ServerLevel world) {
        this.world = world;
        this.subgrids = new ObjectArrayList<>();
    }

    public void log(String msg) {
        LOGGER.info("(" + getDimensionName() + ") " + msg);
    }

    public String getDimensionName() {
        return world.dimension().location().toString();
    }

    public ServerLevel getWorld() {
        return world;
    }

    public LevelReader getLevelReader() {
        return world;
    }

    /**
     * Iterates over all {@link PowerGrid} instances
     * and clears them. Broadcasts updates as a result.
     */
    public void clear() {
        Iterator<PowerGrid> it = subgrids.iterator();
        while(it.hasNext()) {
            PowerGrid grid = it.next();
            grid.clear();
            it.remove();
        }
        subgrids.trim(1);
    }

    private ListTag writeAll() {
        ListTag output = new ListTag();
        for(PowerGrid grid : subgrids) {
            output.add(grid.nodes.write());
        }
        return output;
    }

    public static class Serializer implements IAttachmentSerializer<ListTag, GridDispatcher> {

        public GridDispatcher createNew(IAttachmentHolder holder) {
            if(holder instanceof ServerLevel sl) {
                GridDispatcher newInstance = new GridDispatcher(sl);
                newInstance.log("Created new grid data attachment");
                return newInstance;
            }
            return null;
        }

        @Override
        public GridDispatcher read(IAttachmentHolder holder, ListTag list, Provider provider) {
            if(holder instanceof ServerLevel sl) {
                GridDispatcher deserializedInstance = GridDispatcher.loadFrom(list, sl);
                deserializedInstance.log("Loaded pre-existing grid data");
                return deserializedInstance;
            }
            return null;
        }

        @Override
        public @Nullable ListTag write(GridDispatcher attachment, Provider provider) {
            return attachment.writeAll();
        }
    }
}
