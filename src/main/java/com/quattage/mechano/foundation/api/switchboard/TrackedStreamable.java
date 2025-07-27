package com.quattage.mechano.foundation.api.switchboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
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
    public abstract DataScope getDataScope();

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
    public default boolean canMoveDynamically() {
        return getDataScope() != DataScope.STATIC_CHUNK;
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
}
