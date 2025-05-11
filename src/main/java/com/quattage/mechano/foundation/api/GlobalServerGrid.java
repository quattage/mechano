
package com.quattage.mechano.foundation.api;

import java.util.Iterator;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.landmarks.GridLink;
import com.quattage.mechano.foundation.api.landmarks.GridNode;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifiable;
import com.quattage.mechano.foundation.api.landmarks.NodeIdentifier.Key;
import com.quattage.mechano.foundation.api.transmission.Transmitter;
import com.quattage.mechano.foundation.api.transmission.Transmitable.LinkResponse;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;


/**
 * A GlobalGrid is the top-level manager for {@link PowerGrid PowerGrids}.
 * Data Attachments are leveraged to add one GlobalGrid to each level/dimension.
 */
public class GlobalServerGrid extends SidedGridDispatcher {

    private ObjectArrayList<PowerGrid> subgrids;

    public static GlobalServerGrid loadFrom(ListTag serializedGlobals, ServerLevel world) {
        GlobalServerGrid freshInstance = new GlobalServerGrid(world,  new ObjectArrayList<>(serializedGlobals.size()));
        for(int x = 0; x < serializedGlobals.size(); x++) {
            ListTag writtens = serializedGlobals.getList(x);
            PowerGrid freshGrid = new PowerGrid(freshInstance, serializedGlobals.size());
            for(int y = 0; y < writtens.size(); y++) {
                CompoundTag node = writtens.getCompound(x);
                createNodeAndMakeProvisionalLinks(freshGrid, Key.loadFrom(node), node.getList("links", Tag.TAG_COMPOUND));
            }
            if(!freshGrid.nodes.isEmpty()) freshInstance.subgrids.add(freshGrid);
        }
        return freshInstance;
    }

    /**
     * A breakout method for making the above method easier to read - This method is responsible for de-serializing the GridNode
     * and its links <p>
     * getOrCreate is used here to ensure that only one canonical reference to each GridNode exists after
     * the GlobalServerGrid is loaded. GridNodes may have already been created as links by previous calls
     * to this method.
     * 
     * TODO A recursive approach may reduce iteration count slightly if links are created depth-first rather than re-addressing provisional links
     */
    private static void createNodeAndMakeProvisionalLinks(PowerGrid instantiator, Key address, @Nullable ListTag links) {
        GridNode freshOrigin = instantiator.getOrCreateProvisional(address);
        if(freshOrigin.hasLinks() || links == null) return;
        for(int x = 0; x < links.size(); x++) {
            CompoundTag link = links.getCompound(x);
            Key provisionalLink = Key.loadFrom(link);
            GridNode provisionalTarget = instantiator.getOrCreateProvisional(provisionalLink);
            GridLink freshLink = new GridLink(freshOrigin, provisionalTarget, MechanoTransmissionTypes.REGISTRY.get(link));
            freshOrigin.links.add(freshLink);
        }
    }


    protected GlobalServerGrid(ServerLevel world, ObjectArrayList<PowerGrid> subgrids) {
        super(world);
        this.subgrids = subgrids;
    }

    public GridNode get(NodeIdentifiable<?> address) {
        for(int x = 0; x < subgrids.size(); x++) {
            GridNode get = subgrids.get(x).nodes.get(address);
            if(get != null) return get;
        }
        return null;
    }


    public LinkResponse createLink(NodeIdentifiable<?> source, NodeIdentifiable<?> destination, Transmitter transmitter) {

        

        return LinkResponse.FAIL_GENERIC;
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
