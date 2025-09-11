package com.quattage.mechano.foundation.api;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.api.catenary.CatenaryModel;
import com.quattage.mechano.foundation.api.catenary.WindManager;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection;
import com.quattage.mechano.foundation.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.AnchorSurrogateDestroyPacket;
import com.quattage.mechano.foundation.api.switchboard.AwaitingLinkBuffer;
import com.quattage.mechano.foundation.api.switchboard.AwaitingLinkBuffer.ProcessMode;
import com.quattage.mechano.foundation.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSynchronizer;
import com.quattage.mechano.foundation.api.switchboard.LinkRequestPacket;
import com.quattage.mechano.foundation.api.transmitter.MechanoTransmissionTypes;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.item.SpoolItem;
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
        SpoolItem.wipeFromInventory(lp, true);
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
            GridResponse response = failIfMissing(startAnchor, endAnchor, null);
            if(!response.indicatesCompletion()) return response;
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

        GridConnection preexisting = LinkDataStorable.getAsClient(world, new ConnectionKey(startAnchor.getAddress(), endAnchor.getAddress()));
        if(preexisting != null) return GridResponse.FAIL_DUPLICATE;

        CatnipServices.NETWORK.sendToServer(new LinkRequestPacket(startAnchor.getAddress(), endAnchor.getAddress(), type, GridResponse.TASK_CREATE_LINK));
        return GridResponse.TASK_CREATE_LINK;
    }


    public GridResponse requestLinkDestruction(AnchorPoint startAnchor, AnchorPoint endAnchor, boolean verify) {
        if(startAnchor == null || endAnchor == null) return GridResponse.FAIL_GENERIC;
        if(verify) {
            GridResponse response = failIfMissing(startAnchor, endAnchor, null);
            if(!response.indicatesCompletion()) return response;
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
    public GridResponse handleCatenaryCreation(AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, ProcessMode mode) {
        tryLoad();
        if(mode == ProcessMode.SCHEDULE) {
            buffer.deferForLater(this, start, end, trns, GridResponse.TASK_CREATE_LINK);
            return GridResponse.TASK_COMPLETED;
        }
        AnchorPoint startAnchor = start.applyAndGet(world);
        AnchorPoint endAnchor = end.applyAndGet(world);
        GridResponse response = failIfMissing(startAnchor, endAnchor, null);
        if(!response.indicatesCompletion()) {
            if(mode == ProcessMode.IMMEDIATE) return response;
            buffer.deferForLater(this, start, end, trns, GridResponse.TASK_CREATE_LINK);
            return GridResponse.TASK_COMPLETED;
        }
        GridCatenary cat = new GridCatenary(world, startAnchor, endAnchor, trns);

        LinkDataStorable.put(world, cat);
        cat.sendLevelUpdates(world);
        return GridResponse.TASK_COMPLETED;
    }

    /**
     * Destroys any {@link GridCatenary} instances that span between <code>start</code> 
     * and <code>end</code>, while ensuring all associated data gets removed properly.
     * @see #handleCatenaryCreation
     * @param start starting {@link AnchorSyncHolder}
     * @param end ending {@link AnchorSyncHolder}
     * @param mode {@link ProcessMode} to determine {@link AwaitingLinkBuffer scheduling} behaviour
     */
    public GridResponse handleCatenaryDestruction(AnchorSynchronizer start, AnchorSynchronizer end, ProcessMode mode) {
        tryLoad();
        if(mode == ProcessMode.SCHEDULE) {
            buffer.deferForLater(this, start, end);
            return GridResponse.TASK_COMPLETED;
        }
        start.applyAndGet(world);
        end.applyAndGet(world);
        GridCatenary cat = LinkDataStorable.popAsClient(world, new ConnectionKey(start.getAddress(), end.getAddress()).fixDataScopes(world));
        if(cat == null) {
            if(mode == ProcessMode.IMMEDIATE) return GridResponse.FAIL_CATENARY_NOT_FOUND;
            buffer.deferForLater(this, start, end);
            return GridResponse.TASK_COMPLETED; 
        }
        cat.sendLevelUpdates(world);
        return GridResponse.TASK_COMPLETED;
    }

    /**
     * Similar to {@link #handleCatenaryDestruction}, but this method is allowed
     * to fail silently without any logging statements or buffer usage. This method
     * is designed to be used in cases where a server-sided link failure may have been
     * the result of packet loss or lag. In any case where it may be (expectedly) 
     * ambiguous whether or not <code>start</code> or <code>end</code> exist at the 
     * time of invocation, this method is preferable over {@link #handleCatenaryDestruction()}.
     * @return {@link GridResponse#TASK_COMPLETED}
     */
    public GridResponse ensureCatenaryDestroyed(AnchorSynchronizer start, AnchorSynchronizer end) {
        start.applyAndGet(world); 
        end.applyAndGet(world);
        GridCatenary cat = LinkDataStorable.popAsClient(world, new ConnectionKey(start.getAddress(), end.getAddress()).fixDataScopes(world));
        if(cat != null) cat.sendLevelUpdates(world);
        return GridResponse.TASK_COMPLETED;
    }

    /**
     * Re-initialize {@link AnchorPoint} and associated {@link GridCatenary} instances, 
     * which will refresh client-sided data and {@link CatenaryModel model state} <p>
     * If no catenary could be found between <code>start</code> and <code>end</code>, 
     * this method will instead create a new one. This happens most often when the client
     * loads new chunks that contain catenaries.
     * @param start starting {@link AnchorSyncHolder}
     * @param end ending {@link AnchorSyncHolder}
     * @param trns {@link TransmitterType} to use for creating a new catenary if none coudl be found
     * @param mode {@link ProcessMode} to determine {@link AwaitingLinkBuffer scheduling} behaviour
     */
    public GridResponse handleCatenarySync(AnchorSynchronizer start, AnchorSynchronizer end, TransmitterType<?> trns, ProcessMode mode) {
        tryLoad();
        if(mode == ProcessMode.SCHEDULE) {
            buffer.deferForLater(this, start, end, trns, GridResponse.TASK_SYNC_ANCHORS);
            return GridResponse.TASK_COMPLETED;
        }
        AnchorPoint startAnchor = start.applyAndGet(world);
        AnchorPoint endAnchor = end.applyAndGet(world);
        GridResponse response = failIfMissing(startAnchor, endAnchor, null);
        if(!response.indicatesCompletion()) {
            if(mode == ProcessMode.IMMEDIATE) return response;
            buffer.deferForLater(this, start, end, trns, GridResponse.TASK_SYNC_ANCHORS);
            return GridResponse.TASK_COMPLETED;
        }
        GridCatenary cat = GridCatenary.findLoosely(getWorld(), start.getAddress(), end.getAddress());
        if(cat != null) {
            cat.reinitializeModel(world, CatenaryAttributes.Initializer.RESTING_SIMULATION);
            cat.sendLevelUpdates(world);
            return GridResponse.TASK_COMPLETED;
        }
        cat = new GridCatenary(world, startAnchor, endAnchor, trns);

        ////////////////////////////////////////////////////     
        //// TEMPORARY UNTIL CHUNK RENDERING RE-ENABLED ////
        cat.setDataScope(DataScope.BLOCKENTITY); ///////////      <--- TODO lazy ass
        cat.fixDataScopes(world); //////////////////////////
        ////////////////////////////////////////////////////
        
        cat.reinitializeModel(world, CatenaryAttributes.Initializer.RESTING_SIMULATION);
        LinkDataStorable.put(world, cat);
        cat.sendLevelUpdates(world);
        return GridResponse.TASK_COMPLETED;
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
     * @param start starting {@link AnchorSyncHolder}
     * @param end ending {@link AnchorSyncHolder}
     * @param newStart {@link GridUUID} to replace <code>start</code> with
     * @param mode {@link ProcessMode} to determine {@link AwaitingLinkBuffer scheduling} behaviour
     */
    public GridResponse swapStartingPoint(AnchorSynchronizer start, AnchorSynchronizer end, GridUUID newStart, ProcessMode mode) {
        tryLoad();
        if(mode == ProcessMode.SCHEDULE) {
            buffer.deferForLater(this, start, end, newStart, GridResponse.TASK_SWAP_START);
            return GridResponse.TASK_COMPLETED;
        }
        AnchorPoint startAnchor = start.applyAndGet(world);
        AnchorPoint endAnchor = end.applyAndGet(world);
        if(startAnchor == null && endAnchor != null)
            startAnchor = newStart.getAnchor(getWorld());
        GridResponse response = failIfMissing(startAnchor, endAnchor, null);
        if(!response.indicatesCompletion()) {
            if(mode == ProcessMode.IMMEDIATE) return response;
            buffer.deferForLater(this, start, end, newStart, GridResponse.TASK_SWAP_START);
            return GridResponse.TASK_COMPLETED;
        }
        GridCatenary cat = GridCatenary.findLooselyWithOrphans(getWorld(), startAnchor.getAddress(), endAnchor.getAddress());
        if(cat != null) {
            if(cat.getStart().equals(startAnchor.getAddress()))
                cat.replaceStart(getWorld(), newStart, null);
            else cat.replaceEnd(getWorld(), newStart, null);
            return GridResponse.TASK_COMPLETED;
        }
        if(mode == ProcessMode.IMMEDIATE) return GridResponse.FAIL_CATENARY_NOT_FOUND;
        buffer.deferForLater(this, start, end, newStart, GridResponse.TASK_SWAP_START);
        return GridResponse.TASK_COMPLETED;
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
     * @param start starting {@link AnchorSyncHolder}
     * @param end ending {@link AnchorSyncHolder}
     * @param newStart {@link GridUUID} to replace <code>end</code> with
     * @param mode {@link ProcessMode} to determine {@link AwaitingLinkBuffer scheduling} behaviour
     */
    public GridResponse swapEndingPoint(AnchorSynchronizer start, AnchorSynchronizer end, GridUUID newEnd, ProcessMode mode) {
        tryLoad();
        if(mode == ProcessMode.SCHEDULE) {
            buffer.deferForLater(this, start, end, newEnd, GridResponse.TASK_SWAP_END);
            return GridResponse.TASK_COMPLETED;
        }
        AnchorPoint startAnchor = start.applyAndGet(world);
        AnchorPoint endAnchor = end.applyAndGet(world);
        if(endAnchor == null && startAnchor != null)
            endAnchor = newEnd.getAnchor(getWorld());
        GridResponse response = failIfMissing(startAnchor, endAnchor, null);
        if(!response.indicatesCompletion()) {
            if(mode == ProcessMode.IMMEDIATE) return response;
            buffer.deferForLater(this, start, end, newEnd, GridResponse.TASK_SWAP_END);
            return GridResponse.TASK_COMPLETED;
        }
        GridCatenary cat = GridCatenary.findLooselyWithOrphans(getWorld(), startAnchor.getAddress(), endAnchor.getAddress());
        if(cat != null) {
            if(cat.getEnd().equals(endAnchor.getAddress()))
                cat.replaceEnd(getWorld(), newEnd, null);
            else cat.replaceStart(getWorld(), newEnd, null);
            return GridResponse.TASK_COMPLETED;
        }
        if(mode == ProcessMode.IMMEDIATE) return GridResponse.FAIL_CATENARY_NOT_FOUND;
        buffer.deferForLater(this, start, end, newEnd, GridResponse.TASK_SWAP_END);
        return GridResponse.TASK_COMPLETED;
    }

    /**
     * Re-add a catenary if its removal was temporary. This method is designed
     * for cases where re-use of a catenary is necessary to preserve its model state,
     * but you don't yet have access to one or both {@link AnchorPoint AnchorPoints} 
     * of the catenary itself.
     * @param cat The catenary to reassert
     * @param mode {@link ProcessMode} to determine {@link AwaitingLinkBuffer scheduling} behaviour
     */
    public GridResponse reassertCatenary(GridCatenary cat, ProcessMode mode) {
        tryLoad();
        if(mode == ProcessMode.SCHEDULE) {
            buffer.deferForLater(this, cat);
            return GridResponse.TASK_COMPLETED;
        }
        boolean modified = LinkDataStorable.put(getWorld(), cat);
        if(modified) return GridResponse.TASK_COMPLETED;
        if(mode == ProcessMode.IMMEDIATE) return GridResponse.FAIL_BOTH_ENDS_MISSING;
        buffer.deferForLater(this, cat);
        return GridResponse.TASK_COMPLETED;
    }

    public GridResponse syncSingleAnchor(AnchorSynchronizer synchronizer, ProcessMode mode) {
        if(mode == ProcessMode.SCHEDULE) {
            buffer.deferForLater(this, synchronizer);
            return GridResponse.TASK_COMPLETED;
        }
        AnchorPoint anchor = synchronizer.applyAndGet(world, false);
        if(anchor == null) {
            if(mode == ProcessMode.IMMEDIATE) return GridResponse.FAIL_BOTH_ENDS_MISSING;
            buffer.deferForLater(this, synchronizer);
            return GridResponse.TASK_COMPLETED;
        }
        return GridResponse.TASK_COMPLETED;
    }

    private GridResponse failIfMissing(AnchorPoint startAnchor, AnchorPoint endAnchor, @Nullable GridResponse operation) {
        boolean startExists = startAnchor != null;
        boolean endExists = endAnchor != null;
        if(!startExists && !endExists) {
            if(operation != null) {
                LOGGER.warn("Failed to perform operation '" + operation + "' on link between " + (startAnchor == null ? "NULL" : startAnchor.getAddress()) + " and " 
                    + (endAnchor == null ? "NULL" : endAnchor.getAddress()) + " - No valid anchors could be found at either address!");
            }
            return GridResponse.FAIL_BOTH_ENDS_MISSING;
        }
        if(!startExists) {
            if(operation != null) {
                LOGGER.warn("Failed to perform operation '" + operation + "'on link between " + (startAnchor == null ? "NULL" : startAnchor.getAddress()) + " and " 
                    + (endAnchor == null ? "NULL" : endAnchor.getAddress()) + " - No valid anchor could be found at the starting address");
            }
            return GridResponse.FAIL_START_MISSING;
        }
        if(!endExists) {
            if(operation != null) {
                LOGGER.warn("Failed to perform operation '" + operation + "' on link between " + (startAnchor == null ? "NULL" : startAnchor.getAddress()) + " and " 
                    + (endAnchor == null ? "NULL" : endAnchor.getAddress()) + " - No valid anchor could be found at the ending address");
            }
            return GridResponse.FAIL_END_MISSING;
        }
        return GridResponse.TASK_COMPLETED;
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
        LOGGER.error("Attempted to write server-sided NBT data from " + this);
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
