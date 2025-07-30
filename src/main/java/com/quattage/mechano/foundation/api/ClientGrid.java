package com.quattage.mechano.foundation.api;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSyncHolder;
import com.quattage.mechano.foundation.api.switchboard.LinkRequestPacket;
import com.quattage.mechano.foundation.api.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.WindManager;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.ListTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientGrid extends SidedGridDispatcher {

    private final CatenaryMesher mesher;
    public LinkDataTracker tracker = new LinkDataTracker();

    public static ClientGrid loadFrom(ListTag serializedGlobals, ClientLevel world) {
        return new ClientGrid(world);
    }

    public ClientGrid(ClientLevel world) {
        super(world);
        this.mesher = CatenaryMesher.asEmpty();
        tracker.enable().withLogging(LOGGER);
    }

    @Override
    public ClientLevel getWorld() {
        return (ClientLevel)super.getWorld();
    }

    @Override
    protected void onLoad() {}

    @Override
    protected void onUnload() {
        mesher.reset();
        WindManager.INSTANCE.reset();
    }

    @Override
    public LinkDataTracker getDebugTracker() {
        return tracker;
    }

    /**
     * Request that a link is made. This method does some simple
     * client-sided sanity checks and sents a packet to the 
     * {@link ServerGrid server-sided} version of this 
     * instance. 
     * @param startAnchor The starting point
     * @param endAnchor The ending point
     * @param type The type of transmitter that the resulting link will host
     * @param validate <code>true</code> if this method should verify the existence of <code>startAnchor</code> and <code>endAnchor</code>
     * before proceeding
     * @return {@link UpdateResponser}
     */
    public GridResponse requestLinkCreation(AnchorPoint startAnchor, AnchorPoint endAnchor, TransmitterType<?> type, boolean verify) {

        if(verify) {
            if(!startAnchor.existsIn(world)) {
                Mechano.LOGGER.warn("Failed to create link between " + startAnchor.getAddress() + " and " 
                    + endAnchor.getAddress() + " - No valid PGBE could be found at the starting address");
                return GridResponse.FAIL_OUTDATED;
            }
            if(!endAnchor.existsIn(world)) {
                Mechano.LOGGER.warn("Failed to create link between " + startAnchor.getAddress() + " and " 
                    + endAnchor.getAddress() + " - No valid PGBE could be found at the ending address");
                return GridResponse.FAIL_OUTDATED;
            }
        }

        if(!type.ignoresLimits()) {
            if(!endAnchor.hasRoom()) return GridResponse.FAIL_DESTINATION_FULL;
            if(!endAnchor.isCompatableWith(type)) return GridResponse.FAIL_DESTINATION_UNSUPPORTED;
        }

        if(!type.supportsSameBlockConnections()) {
            if(endAnchor.equals(startAnchor)) 
                return GridResponse.FAIL_DUPLICATE;    
        } else if(startAnchor.getAddress().isApproximately(world, endAnchor.getAddress())) 
            return GridResponse.FAIL_DUPLICATE;

        float linkDistance = startAnchor.distanceTo(world, endAnchor);
        if(linkDistance < type.getMinDistance()) return GridResponse.FAIL_TOO_CLOSE;
        if(linkDistance > type.getMaximumSpan()) return GridResponse.FAIL_TOO_FAR;

        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(startAnchor.getAddress(), endAnchor.getAddress(), type, GridResponse.TASK_CREATE_LINK));
        return GridResponse.TASK_CREATE_LINK;
    }

    public GridResponse requestLinkDestruction(AnchorPoint startAnchor, AnchorPoint endAnchor, boolean verify) {
        if(startAnchor == null || endAnchor == null) return GridResponse.FAIL_GENERIC;
        if(verify) {
            if(!startAnchor.existsIn(world)) {
                Mechano.LOGGER.warn("Failed to destroy link between " + startAnchor.getAddress() + " and " 
                    + endAnchor.getAddress() + " - No valid PGBE could be found at the starting address");
                return GridResponse.FAIL_OUTDATED;
            }

            if(!endAnchor.existsIn(world)) {
                Mechano.LOGGER.warn("Failed to destroy link between " + startAnchor.getAddress() + " and " 
                    + endAnchor.getAddress() + " - No valid PGBE could be found at the ending address");
                return GridResponse.FAIL_OUTDATED;
            }
        }
        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(startAnchor.getAddress(), endAnchor.getAddress(), MechanoTransmissionTypes.PERFECT_CONDUCTOR, GridResponse.TASK_DESTROY_LINK));
        return GridResponse.TASK_DESTROY_LINK;
    }

    /**
     * Creates a new {@link GridCatenary} from <code>start</code> to <code>end</code>
     * and store that catenary in the relevent {@link DataScope scope} for rendering
     * into LevelChunk, BlockEntity, or LivingEntity geometry. Calls to this method
     * will sendBlockUpdated when necessary to ensure that chunks get refreshed.
     * @see #handleCatenaryDestruction
     * @param start
     * @param end
     * @param trns
     * @return The {@link GridCatenary} that was created
     */
    public GridCatenary handleCatenaryCreation(AnchorSyncHolder start, AnchorSyncHolder end, TransmitterType<?> trns) {
        TrackedStreamable[] ends = TrackedStreamable.orderedByRenderPriority(world, start.applyAndGet(world), end.applyAndGet(world));
        GridCatenary cat = new GridCatenary(world, (AnchorPoint)ends[0], (AnchorPoint)ends[1], trns);
        LinkDataStorable.put(world, cat);
        cat.sendLevelUpdates(world);
        return cat;
    }



    /**
     * Destroys any {@link GridCatenary} instances that span between <code>start</code> 
     * and <code>end</code>, while ensuring all associated data gets removed properly.
     * @see #handleCatenaryCreation
     * @param start
     * @param end
     */
    public void handleCatenaryDestruction(AnchorSyncHolder start, AnchorSyncHolder end) {
        start.applyAndGet(world, true);
        end.applyAndGet(world, true);
        GridCatenary cat = LinkDataStorable.popAsClient(world, new ConnectionKey(start, end));
        if(cat != null) cat.sendLevelUpdates(world);
    }

    /**
     * Indicates neither the creation of a new link, nor the destruction of an old one.
     * Callls to this method are used to reinitialize any pre-existing 
     * @param start
     * @param end
     * @param trns
     */
    public void handleCatenarySync(AnchorSyncHolder start, AnchorSyncHolder end, TransmitterType<?> trns) {
        AnchorPoint startAnchor = start.applyAndGet(world);
        AnchorPoint endAnchor = end.applyAndGet(world);
        GridCatenary cat = LinkDataStorable.getAsClient(world, new ConnectionKey(start, end));
        if(cat == null) {
            if(!AnchorSyncHolder.assertAnchorsExist(startAnchor, endAnchor)) return;
            cat = new GridCatenary(world, startAnchor, endAnchor, trns);
            LinkDataStorable.put(world, cat);
            cat.sendLevelUpdates(world);
        }
        else cat.reinitializeModel(world, CatenaryAttributes.Initializer.RESTING_SIMULATION);

    }

    @Override
    protected ListTag writeAll() {
        return new ListTag();
    }

    public CatenaryMesher getMesher() {
        return mesher;
    }

    @Override
    protected String getDistPrefix() {
        return "CLIENT";
    }

    @Override
    public String toString() {
        return "ClientGrid(" + getDimensionName() + ", 0 members)";
    }
}
