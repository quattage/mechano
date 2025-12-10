package com.quattage.mechano.foundation.tracking;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.catenary.model.CatenaryModel;
import com.quattage.mechano.api.switchboard.action.GridAction;
import com.quattage.mechano.foundation.numeric.Duo;
import com.quattage.mechano.foundation.tracking.UUIDSourceDiscriminator.ScopeSpecifier;
import com.simibubi.create.foundation.mixin.accessor.LevelRendererAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * The TrackedObject interface indicates that implementing
 * subclasses require the ability to dynamically associate
 * with an {@link IAttachmentHolder attachment holder} and
 * determine at any time whether or not said attachment holder
 * is loaded by any given {@link ServerPlayer}. If an object
 * implements this interface, that means that said object
 * has some kind of link to a physical object in the world,
 * be that an entity, block entity, levelchunk, or the world itself.
 * <p>
 * Subclasses also implement methods to automatically send packets 
 * targeting relevent players and for comparing multiple instances 
 * by arbitrary priority or by approximate mass. 
 */
public interface TrackedObject extends ScopeSpecifier {

    float DEFAULT_MASS = 65535f;

    

    static Set<ServerPlayer> collectTrackers(ServerLevel world, Collection<TrackedObject> objs) {
        Set<ServerPlayer> senders = new HashSet<>();
        for(ServerPlayer sp : world.getServer().getPlayerList().getPlayers()) {
            for(TrackedObject obj : objs) {
                if(obj == null) continue;
                if(obj.isBeingTrackedBy(sp)) {
                    senders.add(sp);
                    break;
                }
            }
        }
        return senders;
    }

    static Set<ServerPlayer> collectTrackers(ServerLevel world, TrackedObject[] objs) {
        Set<ServerPlayer> senders = new HashSet<>();
        for(ServerPlayer sp : world.getServer().getPlayerList().getPlayers()) {
            for(TrackedObject obj : objs) {
                if(obj == null) continue;
                if(obj.isBeingTrackedBy(sp)) {
                    senders.add(sp);
                    break;
                }
            }
        }
        return senders;
    }

    /**
     * Sends the provided packet to all clients that can see or are loading this
     * TrackedObject. 
     * Generally, the cost incurred by iterating over the entire list of ServerPlayers
     * is worth it when determining visibility, since it vastly improves the stability
     * and performance of the {@link ServerGrid ServerGrid's} packet handling.
     * @param packet
     */
    void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet);

    /**
     * Determines whether or not this object is being tracked by the given ServerPlayer.
     * "Tracking" refers to when an Entity, BlockEntity, or LevelChunk is visible or otherwise
     * loaded by a player in a given instant.
     * @param player
     * @return <code>True</code> if the given {@Link ServerPlayer} is tracking this object.
     */
    boolean isBeingTrackedBy(ServerPlayer player);

    boolean isInsideOf(LevelReader world, ChunkPos chunk);
    boolean isInsideOf(LevelReader world, SectionPos section);
    int getSectionY(LevelReader world);

    /**
     * Broadcasts chunk updates. If this object doesn't belong to a chunk, this method
     * will not do anything.
     * @param world
     */
    void sendLevelUpdates(Level world);

    @OnlyIn(Dist.CLIENT) 
    boolean isInFrustum(LevelReader world, @NotNull Frustum view);

    @OnlyIn(Dist.CLIENT)
    default boolean isVisibleOnScreen(LevelReader world) {
        return isVisibleOnScreen(world, TrackedObject.getFrustum());
    }

    @OnlyIn(Dist.CLIENT)
    default boolean isVisibleOnScreen(LevelReader world, Frustum view) {
        return view == null ? false : isInFrustum(world, view);
    }

    /**
     * Gets the {@link IAttachmentHolder NeoForge data attachment holder} 
     * associated with this TrackedObject, if one exists.
     * @param world
     * @return
     */
    @Nullable IAttachmentHolder getDataStorageHolder();

    /**
     * The priority of a streamable object is a loose approximation of
     * its standing when compared to others. Where necessary, two
     * streamable constructs are compared based on this integer
     * to determine which one should take priority when determining
     * {@link #orderedByAssertionPriority() assertion priority}.
     * @return An integer (higher = more preferential treatment)
     */
    int getPriority();

    /**
     * The effective "mass" of a streamable object is an 
     * approximated value derived (in most cases) from the 
     * size of the associated object's hitbox. Non-movable
     * constructs (e.g. BlockEntities) are indicated by a
     * return value of <code>65535.</code>
     * Used when applying forces to attached objects if those
     * objects can move.
     * @param world world to operate within
     * @return approximate mass (of no particular unit or guaranteed precision) of this TrackedObject
     */
    float getMass(LevelReader world);

    default boolean canReceiveVelocity() {
        return false;
    }

    /**
     * Determines whether or not this TrackedObject refers to 
     * an object that is rendered continuously. <p>
     * Entities and BlockEntities are examples of objects that
     * are remeshed each frame, and so are logically distinct from
     * a Griddable<?>that is attached to a LevelChunk. This method is 
     * used to determine whether or not attached Catenaries need to 
     * be simulated on the client.
     * @return <code>true</code> if this TrackedObject can move without 
     */
    boolean isDynamic();

    /**
     * Broadcasts a {@link GridAction} pertaining to this tracked
     * object, which may send packets, update chunks, and modify world
     * data, depending on the implementing subclass. Will log errors
     * if <code>response</code> is not supported by this subclass.
     * @throws UnsupportedOperationException if this method is called by a 
     * subclass where this functionality  is irrelevent. All client-sided 
     * implementations will throw here.
     * @param response GridResponse to broadcast, which will change
     * what kind of packet gets sent as a result of this call.
     */
    default void broadast(ServerLevel world) { 
        broadcast(world, GridAction.values()[0]); 
    }

    /**
     * Broadcasts a {@link GridAction} pertaining to this tracked
     * object, which may send packets, update chunks, and modify world
     * data, depending on the implementing subclass. Will log errors
     * if <code>response</code> is not supported by this subclass.
     * @throws UnsupportedOperationException if this method is called by a 
     * subclass where this functionality  is irrelevent. All client-sided 
     * implementations will throw here.
     * @param response GridResponse to broadcast, which will change
     * what kind of packet gets sent as a result of this call.
     */
    default void broadcast(ServerLevel world, GridAction response) { 
        throw new UnsupportedOperationException("'" + this.getClass().getSimpleName() + "' can't broadcast!"); 
    }

    /**
     * Enforces a deterministic (if somewhat arbitrary) insertion order
     * between two {@link TrackedObject streamable constructs} 
     * <p>
     * This method is primarily used to decide which {@link GridUUID end} 
     * of a {@link GridConnection} should take render priority when drawing 
     * {@link CatenaryModel catenary meshes}. The code that does this can be 
     * found in the {@link LinkDataStorage polymorphic data store
     * @param world World to operate within.
     * @param start The first TrackedObject to check
     * @param end The second TrackedObject to check (order is completely arbitrary here)
     * @param useFrustum (Optional, defaults to false) - If <code>true</code>,
     * the render priority will additionally use frustum culling when necessary 
     * to distinguish render priority. Frustum culling can only occur on the client,
     * so if this is passed as <code>true</code> on the server, it will be ignored.
     * @return The {@link TrackedObject} that takes priority over the other out 
     * of the two provided. Will never be null.
     */
    static <T extends TrackedObject> Duo<T> orderedByAssertionPriority(LevelReader world, T start, T end) {
        return TrackedObject.orderedByAssertionPriority(world, start, end, false);
    }

    /**
     * Enforces a deterministic (if somewhat arbitrary) renderer
     * priority between any two {@link TrackedObject streamable constructs} 
     * (Usually just {@link GridUUID GridUUIDs})
     * This method is primarily used to decide which end of a {@link GridConnection}
     * should take render priority when drawing {@link CatenaryModel catenary meshes}, 
     * but it is also used for enforcing server-sided {@link GridLink} assertion order
     * in a deterministic way. The code that does this can be found in the 
     * {@link LinkDataStorage polymorphic data store}
     * @param <T>
     * @param world World to operate within.
     * @param start The first TrackedObject to check
     * @param end The second TrackedObject to check (order is completely arbitrary here)
     * @param useFrustum (Optional, defaults to false) - If <code>true</code>,
     * the render priority will additionally use frustum culling when necessary 
     * to distinguish render priority. Frustum culling can only occur on the client,
     * so if this is passed as <code>true</code> on the server, it will be ignored.
     * @return The {@link TrackedObject} that takes priority over the other out 
     * of the two provided. Will never be null.
     */
    static <T extends TrackedObject> Duo<T> orderedByAssertionPriority(LevelReader world, T start, T end, boolean useFrustum) {
        if(start == null && end != null) { 
            Mechano.LOGGER.warn("Potential issue encountered while ordering TrackedObject - The provided starting streamable was null.");
            return Duo.of(end, start);
        } if(start != null && end == null) { 
            Mechano.LOGGER.warn("Potential issue encountered while ordering TrackedObject - The provided ending streamable was null.");
            return Duo.of(start, end);
        } if(start == null && end == null)
            throw new IllegalStateException("Can't assert priority between two null TrackedObject instances!");

        if(world != null) {
            final boolean canStartMove = start.isDynamic();
            final boolean canEndMove = end.isDynamic();
            if(canStartMove && !canEndMove)
                return Duo.of(start, end);
            if(canEndMove && !canStartMove)
                return Duo.of(end, start);
            if(world.isClientSide() && useFrustum) {
                final boolean isStartVisible = start.isVisibleOnScreen(world);
                final boolean isEndVisible = end.isVisibleOnScreen(world);
                if(isStartVisible && !isEndVisible)
                    return Duo.of(start, end);
                if(isEndVisible && !isStartVisible)
                    return Duo.of(end, start);
            }
        }
        if(start.getPriority() > end.getPriority())
            return Duo.of(start, end);
        if(end.getPriority() > start.getPriority())
            return Duo.of(end, start);

        // hashcode fallback just to enforce determinism
        if(start.hashCode() > end.hashCode())
            return Duo.of(start, end);
        return Duo.of(end, start);
    }

    /**
     * Enfores a sorting order when distinguishing between two TrackedObject objects based on their {@link #getMass mass}.
     * This method will return <code>null</code> in cases where neither construct is movable.
     * @param world
     * @return {@link Duo} containing both input TrackedObject instances, where the first is heavier than the second
     */
    static <T extends TrackedObject> @Nullable Duo<T> orderedByMass(LevelReader world, @Nullable T start, @Nullable T end) {
        if(!start.canReceiveVelocity() && !end.canReceiveVelocity()) return null;
        if(!start.isDynamic() && !end.isDynamic()) return null;
        float startWeight = start == null ? TrackedObject.DEFAULT_MASS : (start.isDynamic() && start.canReceiveVelocity() ? start.getMass(world) : TrackedObject.DEFAULT_MASS);
        float endWeight = end == null ? TrackedObject.DEFAULT_MASS : (end.isDynamic() && end.canReceiveVelocity() ? end.getMass(world) : TrackedObject.DEFAULT_MASS);
        if(startWeight - endWeight < 0.05f) return Duo.of(end, start);
        // if(startWeight - endWeight > 0.05f) {
        // }
        return Duo.of(start, end);
    }

    /**
     * Uses an accessor injected by Create to grab the frustum from the LevelRenderer
     */
    static @Nullable Frustum getFrustum() {
        if(Minecraft.getInstance().levelRenderer == null) return null;
        LevelRendererAccessor accessor = ((LevelRendererAccessor)(Minecraft.getInstance().levelRenderer));
        return accessor.create$getCapturedFrustum() != null ?
			accessor.create$getCapturedFrustum() :
			accessor.create$getCullingFrustum();
    }
}
