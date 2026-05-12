
package com.quattage.mechano.grid;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.quattage.mechano.api.ClientGrid;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.foundation.WorldlyObject;
import com.quattage.mechano.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.grid.HierarchicalConstruct.GridReferent;
import com.quattage.mechano.grid.api.component.CircuitComponent;
import com.quattage.mechano.grid.topology.AncillaryNode;
import com.quattage.mechano.grid.topology.BlockJack;
import com.quattage.mechano.grid.topology.link.AncillaryPair;
import com.quattage.mechano.switchboard.JackSelector;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Indicates that an implementing subclass represents some object, be that an Entity or BlockEntity,
 * provides a Circuit and participates in the {@link Grid power grid}. <p>
 * Implementations are expected to provide:
 * <ul>
 *  <li> a {@link #getCircuit() circuit component} which describes this Griddable's internal circuit configuration </li>
 *  <li> a {@link GridUUID uuid} pointing to the in-world location of the provided circuit - see {@link GridTracking data sources} for more info</li>
 *  <li> a {@link GriddableTerminus} describing all outside access points so that this Griddable<?>can attach to others to form part of a larger whole in the {@link Grid power grid}
 *</ul>
 * Implementations of this class should expect to handle both server and client sided logic
 * in the same object. Methods that can't be called on the server are marked with the cooresponding
 * <code>@OnlyIn</code> annotation.
 */
public interface Griddable<T extends GridUUID<T>> extends WorldlyObject, GridReferent<T>, HierarchicalConstruct {

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

    @Override
    @Nullable CircuitComponent getComponent(UUIDComposite binding);

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
    default GridReferent<?> getReferent() {
        return this;
    }

    @Override
    default GridReferent<?> getReferent(LevelReader world) {
        return this;
    }

    default void forEachNeighbor(Consumer<Griddable<T>> cons) {}

    /**
     * Allows user-facing access to this Griddable's {@link GriddableTerminus terminus},
     * which contains a bakeable acceleration structure for getting all
     * {@link AncillaryNode ancillaries} involving this Griddable's 
     * {@link #getComponent internal component}. This method contains validity checks 
     * and will initialize the terminus if needed. The initialized terminus can be 
     * {@link GriddableTerminus#invalidate invalidated later} if the baked data is
     * out of date.
     * @return The instance returned by {@link #provideTerminus() the provider}
     * @see #provideTerminus()
     */
    @ApiStatus.NonExtendable
    default GriddableTerminus getTerminus() {
        GriddableTerminus terminus = provideTerminus().initializeFrom(getComponent());
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
        final ObjectOpenHashSet<AncillaryPair> output = new ObjectOpenHashSet<>(4);
        GriddableTerminus thisTerminus = getTerminus();
        BlockPos pos = getBlockPos();
        if(thisTerminus.isEmpty()) {
            thisTerminus.invalidate();
            return Collections.emptySet();
        }
        thisTerminus.forEach(ancillary -> {
            if(!(ancillary instanceof BlockJack bj)) return;
            BlockJack opposite = bj.getOpposing(getWorld(), pos);
            if(opposite != null) 
                output.add(new AncillaryPair(ancillary, opposite).validateSelf());
        });
        output.trim();
        return output;
    }

    /**
     * Returns <code>true</code> if this {@link Griddable}
     * and <code>other</code> both share adjacent
     * {@link BlockJack block jacks} facing each other.
     * This is used to determine whether or not two blocks 
     * that are touching each other are in the proper 
     * orientation to form an electrical conneciton.
     * @param other
     * @return <code>true</code> if this griddable has a BlockJack
     * currently facing the opposite direction as any BlockJack
     * in <code>other</code>
     */
    default boolean isInteractingWith(Griddable<?> other) {
        Objects.requireNonNull(other);
        if(this == other) return false;
        BlockPos thisPos = getBlockPos();
        BlockPos thatPos = other.getBlockPos();
        Objects.requireNonNull(thisPos);
        Objects.requireNonNull(thatPos);
        if(thisPos == thatPos || thisPos.distManhattan(thatPos) > 1) 
            return false;
        for(AncillaryPair ancs : analyzeAdjacents()) {
            Griddable<?> source = GridTracking.getReferentOrThrow(ancs.getEndAncillary());
            if(source == other) return true;
        }
        return false;
    }

    /**
     * Gets the default {@link AncillaryNode} for this Griddable.
     * Returned when the {@link JackSelector} is inaccessible or 
     * cannot discern a selection when requested.
     * <h5>* at the moment this is only used in gametests</h5>
     * @return The first reachable ancillary in this griddable's {@link #getTerminus() terminus}
     */
    default AncillaryNode<?> getDefaultAncillary() {
        GriddableTerminus acc = getTerminus();
        return acc.getAncillary();
    }

    @OnlyIn(Dist.CLIENT)
    default void showAllAncillaries() {
        provideTerminus().showAll(getSourcePos());
    }

    default int getSectionX() {
        return SectionPos.blockToSectionCoord(getBlockPos().getX());
    }

    default int getSectionZ() {
        return SectionPos.blockToSectionCoord(getBlockPos().getZ());
    }

    default void drawGUILabel(List<Component> tooltip, float posX, float posY, GuiGraphics graphics) {}

    default Vector3d getPositionOf(AncillaryNode<?> joint) {
        Objects.requireNonNull(joint);
        Vector3f local = joint.getRotatedOffset(getSourceRotation());
        return getSourcePos().add(local);
    }

    @OnlyIn(Dist.CLIENT)
    default void forEachExternalLink(Consumer<AncillaryPair> cons) {
        Level world = getWorld();
        if(world == null) return;
        ClientGrid grid = Grid.client(world);
        List<AncillaryPair> links = grid.lookup().getLinksBelongingTo(getUUID());
        if(links == null || links.isEmpty()) return;
        for(int x = 0; x < links.size(); x++) {
            AncillaryPair link = links.get(x);
            if(link == null) continue;
            GridReferent<?> primary = GridReferent.choosePrimary(this, GridTracking.getReferentOrThrow(link.getEndAncillary()));
            if(primary == null || primary != this)
                continue;
            cons.accept(link);
        }
    }

    @Override
    default ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.STRANGER;
    }

    @Override
    default int indexOfChild(HierarchicalConstruct child) {
        return child == getComponent() ? 0 : -1;
    }

    @Override
    default @Nullable HierarchicalConstruct getParentConstruct() {
        return null;
    }

    default void onAddedToGrid(Grid grid) {}
    default void onRemovedFromGrid(Grid grid) {}
}
