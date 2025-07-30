package com.quattage.mechano.foundation.api.switchboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.GridConnection;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.VoxelUUID;
import com.quattage.mechano.foundation.catenary.model.CatenaryModel;
import com.simibubi.create.foundation.mixin.accessor.LevelRendererAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * Handler for LOD-adjacent functionality on both the client and the server.
 * On the client, this interface provides barebones functionality for frustum culling.
 * On the server, provisions are made to assist with entity and chunk-based tracking.
 * 

 * 
 * Note that any of these methods may throw if called from the wrong side.
 * Implementations can be anonymous but not polymorphic.
 */
public interface TrackedStreamable {

    /**
     * Uses an accessor injected by Create to grab the frustum from the LevelRenderer
     */
    public static @Nullable Frustum getFrustum() {
        if(Minecraft.getInstance().levelRenderer == null) return null;
        LevelRendererAccessor accessor = ((LevelRendererAccessor)(Minecraft.getInstance().levelRenderer));
        return accessor.create$getCapturedFrustum() != null ?
			accessor.create$getCapturedFrustum() :
			accessor.create$getCullingFrustum();
    }
    
    /**
     * Sends the provided packet to all clients that can see or are loading this
     * tracked object. 
     * Generally, the cost incurred by iterating over the entire list of ServerPlayers
     * is worth it when determining visibility, since it vastly improves the stability
     * and performance of the {@link ServerGrid ServerGrid's} packet handling.
     * @param packet
     */
    public abstract void sendToClientsTracking(CustomPacketPayload packet);
    public abstract boolean isBeingTrackedBy(ServerPlayer player);
    public abstract boolean isInsideOf(LevelReader world, ChunkPos chunk);
    public abstract boolean isInsideOf(LevelReader world, SectionPos section);
    public abstract int getSectionY(LevelReader world);

    public abstract void sendLevelUpdates(Level world);

    @OnlyIn(Dist.CLIENT)
    public abstract boolean isInFrustum(LevelReader world, @NotNull Frustum view);

    @OnlyIn(Dist.CLIENT)
    public default boolean isVisibleOnScreen(LevelReader world) {
        return isVisibleOnScreen(world, getFrustum());
    }

    @OnlyIn(Dist.CLIENT)
    public default boolean isVisibleOnScreen(LevelReader world, Frustum view) {
        return view == null ? false : isInFrustum(world, view);
    }

    /**
     * Gets the {@Link IAttachmentHolder Attachment Holder} that cooresponds
     * this streamer's associated {@Link DataScope}.
     * @param world
     * @return
     */
    public abstract IAttachmentHolder getDataStorageHolder(LevelReader world);

    /**
     * Used by the {@Link GridManifestGenerator} to create a human-readible 
     * string containing information about the data holder returned by 
     * {@link #getDataStorageHolder} for debugging purposes.
     * @param world World to operate within
     */
    public abstract String describeDataScope(LevelReader world);

    /**
     * @return The {@link DataScope} for this tracked object.
     */
    public abstract DataScope getDataScope(LevelReader world);

    /**
     * Sets the {@link DataScope} for this tracked object, which 
     * is optionally supported by some subclasses that allow changing
     * DataScope at runtime.
     * @param scope
     */
    public default void setDataScope(DataScope scope) {}

    /**
     * Determines whether or not this tracked object represents
     * some kind of movable construct or a if a {@link CatenaryModel}
     * interacting with this tracked object is actively moving.
     * This method is used to determine whether or not a 
     * {@link GridCatenary} is able to bake itself to the LevelChunk
     * or not.
     * @return <code>true</code> if this tracked object can move without 
     * causing LevelChunk remeshing
     */
    public default boolean canMoveDynamically(LevelReader world) {
        return getDataScope(world) != DataScope.STATIC_CHUNK;
    }

    /**
     * Broadcasts a {@link GridResponse} pertaining to this tracked
     * object, which may send packets, update chunks, and modify world
     * data, depending on the implementing subclass. Will log errors
     * if <code>response</code> is not supported by this subclass.
     * @throws UnsupportedOperationException if this method is called by a 
     * subclass where this functionality  is irrelevent. All client-sided 
     * implementations will throw here.
     * @param response GridResponse to broadcast, which will change
     * what kind of packet gets sent as a result of this call.
     */
    public default void broadast() { 
        broadcast(GridResponse.values()[0]); 
    }

    /**
     * Broadcasts a {@link GridResponse} pertaining to this tracked
     * object, which may send packets, update chunks, and modify world
     * data, depending on the implementing subclass. Will log errors
     * if <code>response</code> is not supported by this subclass.
     * @throws UnsupportedOperationException if this method is called by a 
     * subclass where this functionality  is irrelevent. All client-sided 
     * implementations will throw here.
     * @param response GridResponse to broadcast, which will change
     * what kind of packet gets sent as a result of this call.
     */
    public default void broadcast(GridResponse response) { 
        throw new UnsupportedOperationException("'" + this.getClass().getSimpleName() + "' can't broadcast!"); 
    }

    /**
     * Enforces a deterministic (if somewhat arbitrary) renderer
     * priority between any two {@link TrackedStreamable streamable constructs} 
     * (Usually just {@link GridUUID GridUUIDs})
     * This method is primarily used to decide which end of a {@link GridConnection}
     * should take render priority when drawing {@link CatenaryModel catenary meshes}, 
     * but it is also used for enforcing server-sided {@link GridLink} assertion order
     * in a deterministic way. The code that does this can be found in the 
     * {@link LinkDataStorable polymorphic data store}
     * @param world World to operate within.
     * @param start The first TrackedStreamable to check
     * @param end The second TrackedStreamable to check (order is completely arbitrary here)
     * @param useFrustum (Optional, defaults to false) - If <code>true</code>,
     * the render priority will additionally use frustum culling when necessary 
     * to distinguish render priority. Frustum culling can only occur on the client,
     * so if this is passed as <code>true</code> on the server, it will be ignored.
     * @return The {@link TrackedStreamable} that takes priority over the other out 
     * of the two provided. Will never be null unless something goes horribly wrong.
     */
    public static TrackedStreamable[] orderedByRenderPriority(LevelReader world, TrackedStreamable start, TrackedStreamable end) {
        return orderedByRenderPriority(world, start, end, false);
    }

    /**
     * Enforces a deterministic (if somewhat arbitrary) renderer
     * priority between any two {@link TrackedStreamable streamable constructs} 
     * (Usually just {@link GridUUID GridUUIDs})
     * This method is primarily used to decide which end of a {@link GridConnection}
     * should take render priority when drawing {@link CatenaryModel catenary meshes}, 
     * but it is also used for enforcing server-sided {@link GridLink} assertion order
     * in a deterministic way. The code that does this can be found in the 
     * {@link LinkDataStorable polymorphic data store}
     * @param world World to operate within.
     * @param start The first TrackedStreamable to check
     * @param end The second TrackedStreamable to check (order is completely arbitrary here)
     * @param useFrustum (Optional, defaults to false) - If <code>true</code>,
     * the render priority will additionally use frustum culling when necessary 
     * to distinguish render priority. Frustum culling can only occur on the client,
     * so if this is passed as <code>true</code> on the server, it will be ignored.
     * @return The {@link TrackedStreamable} that takes priority over the other out 
     * of the two provided. Will never be null unless something goes horribly wrong.
     */
    public static TrackedStreamable[] orderedByRenderPriority(LevelReader world, TrackedStreamable start, TrackedStreamable end, boolean useFrustum) {

        // welcome to spaghettiville
        TrackedStreamable[] out = new TrackedStreamable[2];

        final boolean canStartMove = start.canMoveDynamically(world);
        final boolean canEndMove = end.canMoveDynamically(world);
        if(canStartMove && !canEndMove) {
            out[0] = start;
            out[1] = end;
            return out;
        }
        if(canEndMove && !canStartMove) {
            out[0] = end;
            out[1] = start;
            return out;
        }

        if(world.isClientSide() && useFrustum) {
            final boolean isStartVisible = start.isVisibleOnScreen(world);
            final boolean isEndVisible = end.isVisibleOnScreen(world);
            if(isStartVisible && !isEndVisible) {
                out[0] = start;
                out[1] = end;
                return out;
            }
            if(isEndVisible && !isStartVisible) {
                out[0] = end;
                out[1] = start;
                return out;
            }
        }

        /**
         * this edge case handling is here as a "temporary" measure to ensure
         * that we don't try to inject dynamic wire geometry to the level chunk,
         * since that would be stupid.
         */
        if(!(start instanceof VoxelUUID) && (end instanceof VoxelUUID)) {
            out[0] = start;
            out[1] = end;
            return out;
        }
        if((start instanceof VoxelUUID) && !(end instanceof VoxelUUID)) {
            out[0] = end;
            out[1] = start;
            return out;
        }

        /**
         * this comparison has no logical significance other than
         * to fall back on something deterministic.
         */
        if(start.hashCode() > end.hashCode()) {
            out[0] = start;
            out[1] = end;
            return out;
        }

        out[0] = end;
        out[1] = start;
        return out;
    }

}
