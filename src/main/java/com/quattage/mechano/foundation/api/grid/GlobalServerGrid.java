package com.quattage.mechano.foundation.api.grid;

import java.util.Iterator;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;


/**
 * A GlobalGrid is the top-level manager for {@link PowerGrid PowerGrids}.
 * Data Attachments are leveraged to add one GlobalGrid to each level/dimension.
 */
public class GlobalServerGrid extends SidedGridDispatcher {

    private ObjectArrayList<PowerGrid> subgrids;

    public static GlobalServerGrid loadFrom(ListTag serializedGlobals, ServerLevel world) {
        GlobalServerGrid freshInstance = new GlobalServerGrid(world);
        ObjectArrayList<PowerGrid> deserialized = new ObjectArrayList<>(serializedGlobals.size());
        for(int x = 0; x < serializedGlobals.size(); x++) {
            ListTag serializedSubgrid = serializedGlobals.getList(x);
            PowerGrid grid = PowerGrid.loadFrom(freshInstance, freshInstance.getLevelReader(), serializedSubgrid);
            deserialized.add(grid);
        }
        freshInstance.subgrids = deserialized;
        return freshInstance;
    }

    protected GlobalServerGrid(ServerLevel world) {
        super(world);
        subgrids = new ObjectArrayList<>();
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

    @Override
    public ServerLevel getWorld() {
        return (ServerLevel)super.getWorld();
    }

    @Override
    protected @Nullable ListTag writeAll() {
        ListTag output = new ListTag();
        for(PowerGrid grid : subgrids) {
            output.add(grid.nodes.write());
        }
        return output;
    }

    @Override
    protected String getDistPrefix() {
        return "SERVER";
    }

    @Override
    public String toString() {
        return "GlobalServerGrid(" + getDimensionName() + ", " + subgrids.size() + " members)";
    }
}
