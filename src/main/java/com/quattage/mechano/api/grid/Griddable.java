
package com.quattage.mechano.api.grid;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.quattage.mechano.api.SidedGridDispatcher;
import com.quattage.mechano.api.grid.topology.CircuitProvider;
import com.quattage.mechano.api.grid.topology.ancillary.AncillaryJack;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.TrackedObject;

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
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Griddable grants implementations the ability to utilize the {@link SidedGridDispatcher GridAPI}
 * to store and use {@link GridNode GridNodes,} {@link GridLink GridLinks,}
 * and {@link AnchorPoint AnchorPoints}. 
 * Implementations of this class should expect to handle both server and client sided logic
 * in the same object. Methods that can't be called on the server are marked with the cooresponding
 * <code>@OnlyIn</code> annotation.
 */
public interface Griddable extends CircuitProvider, TrackedObject {

    LazyJointHolder getExposedAncillaries();
    Vector3d getSourcePos();
    Quaternionf getSourceRotation();
    BlockPos getBlockPos(); 
    GridUUID getAddress();

    @Override default boolean isDynamic() { return true; };
    default void forEachNeighbor(Consumer<Griddable> cons) {}

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

    default Vector3d getPositionOf(AncillaryJack joint) {
        if(joint == null) return new Vector3d();
        Vector3f local = joint.getRotatedOffset(getSourceRotation());
        return getSourcePos().add(local);
    }

    /**
     * @return a decorative string used for debugging
     */
    default String describeState() {
        return "No state descriptor";
    }
}
