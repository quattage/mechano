
package com.quattage.mechano.api.grid;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.topology.AncillaryPair;
import com.quattage.mechano.api.grid.topology.CircuitProvider;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.BlockJack;
import com.quattage.mechano.api.switchboard.JackSelector;
import com.quattage.mechano.foundation.WorldlyObject;
import com.quattage.mechano.foundation.tracking.GridIdentifiable;
import com.quattage.mechano.foundation.tracking.GridUUID;
import com.quattage.mechano.foundation.tracking.TrackedObject;
import com.quattage.mechano.foundation.tracking.UUIDSourceType;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
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
import net.minecraft.world.level.block.entity.BlockEntity;
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

    /**
     * Gets the blocks at <code>pos</code> and <code>adjacentPos</code>
     * and checks to see if there are {@link BlockJack jacks} facing each other
     * between the shared block face of these adjacent blocks.
     * @param world World to operate within
     * @param pos position of arbitrary point A
     * @param adjacentPos position of arbitrary point B
     * @return <code>true</code> if there are matching {@link BlockJack} instances
     * at two adjacent positions <code>pos</code> and <code>adjacentPos</code>
     */
    static boolean isVoxelInteractionOccuring(LevelReader world, BlockPos pos, BlockPos adjacentPos) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(pos);
        Objects.requireNonNull(adjacentPos);
        if(!(world instanceof ServerLevel sl)) return false;
        BlockEntity be = sl.getBlockEntity(pos);
        if(!(be instanceof Griddable<?> thisG)) return false;
        BlockEntity beo = sl.getBlockEntity(adjacentPos);
        if(!(beo instanceof Griddable<?> thatG)) return false;
        return thisG.isInteractingWith(thatG);
    }

    Vector3d getSourcePos();
    Quaternionf getSourceRotation();
    BlockPos getBlockPos(); 

    /**
     * Overridden by subclasses to provide a {@link GriddableTerminus} instance. 
     * This instance is not guaranteed to contain up-to-date information about
     * this Griddable<?>and this method contains no checks to verify its validity.
     * @return A (new or pre-existing) {@link GriddableTerminus}
     * @see #getTerminus() use this method instead
     */
    @ApiStatus.Internal
    GriddableTerminus provideTerminus();

    @Override
    default Griddable<?> getTargetSource(LevelReader world) {
        return this;
    }

    @Override default boolean isDynamic() { return true; };
    default void forEachNeighbor(Consumer<Griddable<T>> cons) {}

    /**
     * Allows grid-sided access to this Griddable's {@link GriddableTerminus accelerator},
     * which contains a bakeable acceleration structure for getting all
     * {@link AncillaryNode ancillaries} involving this Griddable's 
     * {@link #getCircuit circuit}. This method contains validity checks and will
     * initialize the accelerator if needed. The initialized accelerator can be 
     * {@link GriddableTerminus#invalidate invalidated later} if the baked data is
     * out of date.
     * @return The instance returned by {@link #provideTerminus() the provider}
     * @see #provideTerminus()
     */
    @ApiStatus.NonExtendable
    default GriddableTerminus getTerminus() {
        GriddableTerminus terminus = provideTerminus().initializeFrom(getCircuit());
        return terminus;
    }

    /**
     * Collects a set of all direct voxel-voxel connections
     * that this Griddable can produce. This is determined
     * by the {@link BlockJack block jacks} contained by
     * this Griddable and whether or not an adjacent
     * griddable has a block jack facing the opposite direction.
     * @param world World to operate within
     * @return A set of {@link AncillaryPair pairs}
     */
    default Set<AncillaryPair> analyzeAdjacents() {
        ObjectOpenHashSet<AncillaryPair> output = new ObjectOpenHashSet<>(4);
        GriddableTerminus thisTerminus = getTerminus();
        final BlockPos pos = getBlockPos();
        if(thisTerminus.isEmpty()) return Collections.emptySet();
        thisTerminus.forEach(ancillary -> {
            if(!(ancillary instanceof BlockJack bj)) return;
            BlockJack opposite = bj.getOpposing(getWorld(), pos);
            if(opposite != null && opposite.isSignificant()) 
                output.add(new AncillaryPair(ancillary, opposite).validateSelf());
        });
        output.trim();
        return output;
    }

    default boolean isInteractingWith(Griddable<?> other) {
        Objects.requireNonNull(other);
        if(this == other) return false;
        BlockPos thisPos = getBlockPos();
        BlockPos thatPos = other.getBlockPos();
        Objects.requireNonNull(thisPos);
        Objects.requireNonNull(thatPos);
        if(thisPos == thatPos || thisPos.distManhattan(thatPos) > 1) 
            return false;
        for(AncillaryPair ancs : analyzeAdjacents())
            if(ancs.getEndNode().getSource() == other) return true;
        return false;
    }

    /**
     * Gets the default {@link AncillaryNode} for instances
     * where the {@link JackSelector} is not accessible (like
     * in gametests)
     * @return The first reachable ancillary in this griddable's {@link #getTerminus() terminus}
     */
    default AncillaryNode getDefaultAncillary() {
        GriddableTerminus acc = getTerminus();
        return acc.getFirstAncillary();
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
