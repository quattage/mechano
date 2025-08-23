package com.quattage.mechano.foundation.gridapi;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.gridapi.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.gridapi.catenary.WindManager;
import com.quattage.mechano.foundation.gridapi.landmark.GridCatenary;
import com.quattage.mechano.foundation.gridapi.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.gridapi.switchboard.AnchorSurrogateDestroyPacket;
import com.quattage.mechano.foundation.gridapi.switchboard.AwaitingLinkBuffer;
import com.quattage.mechano.foundation.gridapi.switchboard.GridResponse;
import com.quattage.mechano.foundation.gridapi.switchboard.GridResponse.AnchorSynchronizer;
import com.quattage.mechano.foundation.gridapi.switchboard.LinkRequestPacket;
import com.quattage.mechano.foundation.gridapi.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.gridapi.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.gridapi.transmitter.TransmitterRegistry.TransmitterType;
import com.simibubi.create.content.contraptions.Contraption;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.ListTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientGrid extends SidedGridDispatcher {

    private static @Nullable Griddable<?> cachedPoints = null;

    public static Griddable<?> getCachedPoints(LocalPlayer lp) {
        if(cachedPoints == null)
            cachedPoints = lp.getData(MechanoData.ANCHOR_ATTACHMENT);
        return cachedPoints;
    }

    public static void destroyCachedPoints() {
        LocalPlayer lp = Minecraft.getInstance().player;
        if(lp == null) return;
        destroyCachedPoints(lp);
    }

    public static void destroyCachedPoints(LocalPlayer lp) {
        GridUUID addr = getCachedPoints(lp).createSupplementaryAddress();
        if(addr == null) return;
        CatnipServices.NETWORK.sendToServer(new AnchorSurrogateDestroyPacket(addr));
    }

    private final AwaitingLinkBuffer buffer = new AwaitingLinkBuffer();
    private LinkDataTracker tracker = null;
    private boolean isLoaded = false;

    public static ClientGrid loadFrom(ListTag serializedGlobals, ClientLevel world) {
        return new ClientGrid(world);
    }

    public ClientGrid(ClientLevel world) {
        super(world);
        if(Mechano.USE_VERBOSE_LINK_TRACKING)
            tracker = new LinkDataTracker().enable().withLogging(LOGGER);
    }

    @Override 
    protected void onLoad() {
        buffer.wakeUp();
    }

    private void tryLoad() {
        if(!isLoaded) onLoad();
        isLoaded = true;
    }

    @Override
    protected void onUnload() {
        WindManager.INSTANCE.reset();
        buffer.shutdown();
        cachedPoints = null;
    }

    @Override
    protected void tick() {
        buffer.tryTickFrom(this);
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

        Objects.requireNonNull(startAnchor);
        Objects.requireNonNull(endAnchor);
        tryLoad();

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

        if(endAnchor.equals(startAnchor) || (!type.supportsUnindexedConnections() 
            && startAnchor.getAddress().isUnindexed(endAnchor.getAddress()))) 
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
     * @param schedule <code>true</code> if this action should be deferred for 
     * later in the {@link AwaitingLinkBuffer buffer} in the event of failure
     */
    public void handleCatenaryCreation(AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, boolean schedule) {
        tryLoad();
        AnchorPoint startAnchor = start.applyAndGet(world);
        AnchorPoint endAnchor = end.applyAndGet(world);
        if(startAnchor == null || endAnchor == null) {
            if(schedule) buffer.deferForLater(this, start, end, trns, GridResponse.TASK_CREATE_LINK);
            return;
        }
        TrackedStreamable[] ordered = TrackedStreamable.orderedByAssertionPriority(world, start.applyAndGet(world), end.applyAndGet(world));
        GridCatenary cat = new GridCatenary(world, (AnchorPoint)ordered[0], (AnchorPoint)ordered[1], trns);
        LinkDataStorable.put(world, cat);
        cat.sendLevelUpdates(world);
    }

    /**
     * Destroys any {@link GridCatenary} instances that span between <code>start</code> 
     * and <code>end</code>, while ensuring all associated data gets removed properly.
     * @see #handleCatenaryCreation
     * @param start
     * @param end
     * @param schedule <code>true</code> if this action should be deferred for 
     * later in the {@link AwaitingLinkBuffer buffer} in the event of failure
     */
    public void handleCatenaryDestruction(AnchorSynchronizer start, AnchorSynchronizer end) {
        tryLoad();
        start.applyAndGet(world);
        end.applyAndGet(world);
        GridCatenary cat = LinkDataStorable.popAsClient(world, new ConnectionKey(start, end));
        if(cat != null) cat.sendLevelUpdates(world);
    }

    /**
     * Indicates neither the creation of a new link, nor the destruction of an old one.
     * Callls to this method are used to reinitialize any pre-existing 
     * @param start
     * @param end
     * @param trns
     * @param schedule <code>true</code> if this action should be deferred for 
     * later in the {@link AwaitingLinkBuffer buffer} in the event of failure
     */
    public void handleCatenarySync(AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, boolean schedule) {
        tryLoad();
        GridCatenary cat = LinkDataStorable.getAsClient(world, new ConnectionKey(start, end));
        if(cat == null) {
            AnchorPoint startAnchor = start.applyAndGet(world);
            AnchorPoint endAnchor = end.applyAndGet(world);
            if(startAnchor == null || endAnchor == null) {
                if(schedule) buffer.deferForLater(this, start, end, trns, GridResponse.TASK_SYNC_ANCHORS);
                return;
            }
            TrackedStreamable[] ordered = TrackedStreamable.orderedByAssertionPriority(world, start.applyAndGet(world), end.applyAndGet(world));
            cat = new GridCatenary(world, (AnchorPoint)ordered[0], (AnchorPoint)ordered[1], trns);
            LinkDataStorable.put(world, cat);
            cat.sendLevelUpdates(world);
            return;
        }
        cat.reinitializeModel(world, CatenaryAttributes.Initializer.RESTING_SIMULATION);
    }

    /**
     * Swaps the starting point of the {@link GridCatenaary} spanning 
     * between <code>start</code> and <code>end</code>, replacing it 
     * with <code>newStart</code>. This method can be used to rebind a 
     * {@link GridUUID} to reflect a change of ownership when a
     * {@link GridCatenary} transitions between states (like when an 
     * {@link GriddableBlockEntity implementing BlockEntity} gets 
     * picked up or put down by a {@link Contraption})
     * @see #swapEndingPoint
     * @param start 
     * @param end
     * @param newStart
     * @param schedule <code>true</code> if this action should be deferred for 
     * later in the {@link AwaitingLinkBuffer buffer} in the event of failure
     */
    public void swapStartingPoint(AnchorSynchronizer start, AnchorSynchronizer end, GridUUID newStart, boolean schedule) {
        AnchorPoint startAnchor = start.applyAndGet(world);
        AnchorPoint endAnchor = end.applyAndGet(world);
        if(startAnchor == null || endAnchor == null || newStart.getDataStorageHolder(world) == null) {
            if(schedule) buffer.deferForLater(this, start, end, newStart, GridResponse.TASK_SWAP_START);
            return;
        }
        ConnectionKey key = new ConnectionKey(startAnchor.getAddress(), endAnchor.getAddress());
        GridCatenary cat = LinkDataStorable.getAsClient(world, key, true);
        if(cat == null) cat = LinkDataStorable.getAsClient(world, key.inverseCopy(), true);
        if(cat == null) { 
            if(schedule) buffer.deferForLater(this, start, end, newStart, GridResponse.TASK_SWAP_START);
            return;
        }
        cat.replaceAddresses((ClientLevel)world, newStart, cat.getEnd(), null);
    }

    /**
     * Swaps the ending point of the {@link GridCatenaary} spanning 
     * between <code>start</code> and <code>end</code>, replacing it 
     * with <code>newEnd</code>. This method can be used to rebind a 
     * {@link GridUUID} to reflect a change of ownership when a
     * {@link GridCatenary} transitions between states (like when an 
     * {@link GriddableBlockEntity implementing BlockEntity} gets 
     * picked up or put down by a {@link Contraption})
     * @see #swapStartingPoint
     * @param start 
     * @param end
     * @param newEnd
     * @param schedule <code>true</code> if this action should be deferred for 
     * later in the {@link AwaitingLinkBuffer buffer} in the event of failure
     */
    public void swapEndingPoint(AnchorSynchronizer start, AnchorSynchronizer end, GridUUID newEnd, boolean schedule) {
        AnchorPoint startAnchor = start.applyAndGet(world);
        AnchorPoint endAnchor = end.applyAndGet(world);
        if(startAnchor == null || endAnchor == null) {
            if(schedule) buffer.deferForLater(this, start, end, newEnd, GridResponse.TASK_SWAP_END);
            return;
        }
        ConnectionKey key = new ConnectionKey(startAnchor.getAddress(), endAnchor.getAddress());
        GridCatenary cat = LinkDataStorable.getAsClient(world, key, true);
        if(cat == null) cat = LinkDataStorable.getAsClient(world, key.inverseCopy(), true);
        if(cat == null) { 
            if(schedule) buffer.deferForLater(this, start, end, newEnd, GridResponse.TASK_SWAP_END);
            return;
        }
        cat.replaceAddresses((ClientLevel)world, cat.getStart(), newEnd, null);
    }

    @Override
    public ClientLevel getWorld() {
        return (ClientLevel)super.getWorld();
    }

    @Override
    public LinkDataTracker getDebugTracker() {
        return tracker;
    }

    @Override
    protected ListTag writeAll() {
        Mechano.LOGGER.error("Attempted to write server-sided NBT data from " + this);
        return new ListTag();
    }

    @Override
    protected String getDistPrefix() {
        return "CLIENT";
    }

    @Override
    public String toString() {
        return "ClientGrid(" + getDimensionName() + ", " + buffer.size() + " tasks)";
    }

}
