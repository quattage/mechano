package com.quattage.mechano.foundation.api.switchboard;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.ServerGrid;
import com.simibubi.create.foundation.mixin.accessor.LevelRendererAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Handler for LOD-adjacent functionality on both the client and the server.
 * On the client, this interface provides barebones functionality for frustum culling.
 * On the server, provisions are made to assist with entity and chunk-based tracking.
 * 
 * Generally, the cost incurred by iterating over the entire list of ServerPlayers
 * is worth it when determining visibility, since it vastly improves the stability
 * and performance of the {@link ServerGrid ServerGrid's} packet handling.
 * 
 * Note that any of these methods may throw if called from the wrong side.
 * Implementations can be anonymous but not polymorphic.
 */
public interface TrackableStreamer {

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
    
    public abstract void sendToClientsTracking(CustomPacketPayload packet);
    public abstract boolean isBeingTrackedBy(ServerPlayer player);
    public abstract boolean isInsideOf(LevelReader world, ChunkPos chunk);
    public abstract boolean isInsideOf(LevelReader world, SectionPos section);

    @OnlyIn(Dist.CLIENT)
    public abstract boolean isInFrustum(LevelReader world, @NotNull Frustum view);

    @OnlyIn(Dist.CLIENT)
    public default boolean isVisibleOnScreen(LevelReader world) {
        Frustum view = getFrustum();
        return view == null ? false : isInFrustum(world, view);
    }

    @OnlyIn(Dist.CLIENT)
    public default boolean isVisibleOnScreen(LevelReader world, Frustum view) {
        return view == null ? false : isInFrustum(world, view);
    }
}
