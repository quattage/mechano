
package com.quattage.mechano.api.grid;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.topology.CircuitProvider;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.foundation.WorldlyObject;
import com.quattage.mechano.foundation.tracking.GridIdentifiable;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.TrackedObject;
import com.quattage.mechano.foundation.tracking.UUIDSourceType;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Indicates that an implementing subclass represents some object, be that an Entity or BlockEntity,
 * provides a Circuit and participates in the {@link Grid power grid}. <p>
 * Implementations are expected to provide:
 * <ul>
 *  <li> a {@link CircuitProvider#getCircuit() circuit component} which describes this Griddable's internal circuit configuration </li>
 *  <li> a {@link GridUUID uuid} pointing to the in-world location of the provided circuit - see {@link UUIDSourceType data sources} for more info</li>
 *  <li> a {@link GriddableTerminus} describing all outside access points so that this Griddable<?>can attach to others to form part of a larger whole in the {@link Grid power grid}
 *</ul>
 * Implementations of this class should expect to handle both server and client sided logic
 * in the same object. Methods that can't be called on the server are marked with the cooresponding
 * <code>@OnlyIn</code> annotation.
 */
public interface Griddable<T extends GridUUID> extends CircuitProvider, TrackedObject, WorldlyObject, GridIdentifiable<T> {

    Vector3d getSourcePos();
    Quaternionf getSourceRotation();
    BlockPos getBlockPos(); 

    /**
     * Overridden by subclasses to provide a {@link GriddableTerminus} instance. 
     * This instance is not guaranteed to contain up-to-date information about
     * this Griddable<?>and this method contains no checks to verify its validity.
     * @return A (new or pre-existing) {@link GriddableTerminus}
     * @see #getTerminus() For callers: Use getTerminus() this method instead
     */
    GriddableTerminus provideTerminus();

    @Override
    default Griddable<?> getTargetSource(LevelReader world) {
        return this;
    }

    @Override default boolean isDynamic() { return true; };
    default void forEachNeighbor(Consumer<Griddable<T>> cons) {}

    /**
     * Allows grid-sided access to this Griddable's {@link GriddableTerminus terminus},
     * which contains a bakeable acceleration structure for getting all
     * {@link AncillaryNode ancillaries} involving this Griddable's 
     * {@link #getCircuit circuit}. This method contains validity checks and will
     * initialize the terminus if needed. The initialized terminus can be 
     * {@link GriddableTerminus#invalidate invalidated later} if the baked data is
     * out of date.
     * @return The instance returned by {@link #provideTerminus() the provider}
     * @see #provideTerminus()
     */
    default GriddableTerminus getTerminus() {
        provideTerminus().initializeFrom(getCircuit());
        return provideTerminus();
    }

    @Override
    default void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet) {
        MinecraftServer server = world.getServer();
        if(server == null)
            server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        for(ServerPlayer player : server.getPlayerList().getPlayers()) {
            if(isBeingTrackedBy(player))
                CatnipServices.NETWORK.sendToClient(player, packet);
        }
    }

    @OnlyIn(Dist.CLIENT)
    default void showAllAncillaries() {
        provideTerminus().showAll(getSourcePos());
    }
    
    @Override
    default boolean isInsideOf(LevelReader world, SectionPos section) {
        return SectionPos.of(getBlockPos()).equals(section);
    }

    @Override
    default boolean isInsideOf(LevelReader world, ChunkPos chunk) {
        BlockPos pos = getBlockPos();
        return SectionPos.blockToSectionCoord(pos.getX()) == chunk.x && SectionPos.blockToSectionCoord(pos.getZ()) == chunk.z;
    }


    default int getSectionX() {
        return SectionPos.blockToSectionCoord(getBlockPos().getX());
    }

    @Override
    default int getSectionY(LevelReader world) {
        return SectionPos.blockToSectionCoord(getBlockPos().getY());
    }

    default int getSectionZ() {
        return SectionPos.blockToSectionCoord(getBlockPos().getZ());
    }

    default void drawGUILabel(List<Component> tooltip, float posX, float posY, GuiGraphics graphics) {}

    default Vector3d getPositionOf(AncillaryNode joint) {
        Objects.requireNonNull(joint);
        Vector3f local = joint.getRotatedOffset(getSourceRotation());
        return getSourcePos().add(local);
    }
}
