
package com.quattage.mechano.foundation.gridapi;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorArray;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.anchor.SurrogateNode;
import com.quattage.mechano.foundation.gridapi.landmark.GridLink;
import com.quattage.mechano.foundation.gridapi.landmark.GridNode;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.gridapi.transmitter.Transmitable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Griddable grants implementations the ability to utilize the {@link SidedGridDispatcher GridAPI}
 * to store and use {@link GridNode GridNodes,} {@link GridLink GridLinks,}
 * and {@link AnchorPoint AnchorPoints}. 
 * Implementations of this class should expect to handle both server and client sided logic
 * in the same object. Methods that can't be called on the server are marked with the cooresponding
 * <code>@OnlyIn</code> annotation.
 */
public interface Griddable<T> {

    @OnlyIn(Dist.CLIENT)
    public void constructAnchors(AnchorArray.Builder anchors);

    @OnlyIn(Dist.CLIENT)
    public AnchorArray getAnchors();

    @OnlyIn(Dist.CLIENT)
    public default AnchorPoint getAnchor(int index) {
        return getAnchors().getByIndex(index);
    }

    @OnlyIn(Dist.CLIENT)
    public default AnchorPoint getAnchor() {
        return getAnchors().getByIndex(0);
    }

    @OnlyIn(Dist.CLIENT)
    public default void refreshAnchors(BlockState newState) {
        if(newState == null) return;
        getAnchors().updateOrientation(newState);
    }


    public static boolean assertPairExists(Griddable<?> startPoints, Griddable<?> endPoints, GridUUID start, GridUUID end) {
        if(startPoints == null && endPoints == null) {
            Mechano.LOGGER.error("An error occured while peforming an operation on a pair: " + start + ", and " + end 
                + " - No valid Griddables could be found at both sides!");
            return false;
        }
        if(startPoints == null) {
            Mechano.LOGGER.error("An error occured while peforming an operation on a pair: " + start + ", and " + end 
                + " - No valid Griddable could be found at the starting address!");
            return false;
        } else if(endPoints == null) {
            Mechano.LOGGER.error("An error occured while peforming an operation on a pair: " + start + ", and " + end 
                + " - No valid Griddable could be found at the ending address!");
            return false;
        }
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public default boolean containsAnchor(AnchorPoint anchor) {
        if(anchor == null || anchor.getAddress().getIndex() < 0 || anchor.getAddress().getIndex() >= getAnchors().size()) 
            return false;

        return createSupplementaryAddress().isVeryApproximately(getWorld(), anchor.getAddress());
        // for(int x = 0; x < getAnchors().size(); x++) {
        //     AnchorPoint other = getAnchor(x);
        //     if(other == null) continue;
        //     if(anchor.getAddress().equals(other.getAddress()))
        //         return true;
        // }
        // return false;
    }

    public default CompoundTag writeAnchorData(CompoundTag tag) {
        ListTag list = new ListTag();
        for(int x = 0; x < getAnchors().size(); x++) {
            AnchorPoint anchor = getAnchor(x);
            list.add(anchor.writeTo(new CompoundTag()));
        }
        tag.put("anchors", list);
        return tag;
    }

    public default void readAnchorData(CompoundTag tag) {
        ListTag list = tag.getList("anchors", Tag.TAG_COMPOUND);
        for(int x = 0; x < list.size(); x++)
            getAnchor(x).initializeFrom(list.getCompound(x));
    }

    public Level getWorld();
    public SurrogateNode getSurrogate();

    /**
     * Provides a supplementary {@link GridUUID}. This UUID
     * should always have an index of 1.
     * It is reccomended to avoid caching behaviours when returning
     * UUIDs from this method, since such behaviour is already 
     * implemented by the {@link SurrogateNode}.
     * @return A new GridUUID instance describing the non-indexed 
     * location of this Griddable.
     */
    public GridUUID createSupplementaryAddress();

    /**
     * A decorative string used for debugging for display in the 
     * {@link com.quattage.mechano.infrastructure.manifest.GridManifestGenerator manifest}
     * @return
     */
    public default String describeState() {
        return null;
    }

    /**
     * Destroys this holder's surrogate, which 
     * removes worldly references in the {@link ServerGrid}.
     * Calls to this method will destroy connections made to this
     * holder.
     */
    public default void destroySurrogate() {
        getSurrogate().destroy();
    }

    /**
     * Called while the player is targeting this holder or an {@link AnchorPoint} that belongs to it.
     * @param tooltip
     * @param player
     * @param held
     * @param targetedAnchor The anchor the player is looking at. Can be <code>null</code>
     * @return <code>true</code> if the tooltip should be sent. If <code>false</code>, no tooltip will be displayed to the player.
     */
    public default boolean writeTooltip(List<Component> tooltip, Transmitable.HoldingSummary held, @Nullable AnchorPoint targetedAnchor) {
        if(targetedAnchor == null) return true;
        targetedAnchor.writeInfoToTooltip(tooltip);
        return true;
    }

    /**
     * Called when this holder is initially registered within a {@link ServerMatrix}.
     * 
     * @param world World to operate within
     * @param grid The grid that this holder was added to
     */
    public default void onAddedToMatrix(Level world, ServerMatrix grid) {
        
    }

    /**
     * Called whenever a connection is made to/from this holder
     * @param world World to operate within
     * @param connection The GridLink representing the connection that was added
     */
    public default void onConnectionCreated(Level world, GridLink connection) {
        
    }

    /**
     * Called whenever a connection is removed to/from this holder
     * @param world World to operate within
     * @param connection The connection that was destroyed. Note that this method is called AFTER the GridLink is removed from the network, so this connection's reference is stale and should't be stored.
     */
    public default void onConnectionDestroyed(Level world, GridLink connection) {
        
    }

    /**
     * This method can be used as a way to tack on additional logic to the
     * {@link SidedGridDispatcher GridAPI} syncing cycle whenever 
     * connections are made or {@link GridNode GridNodes} are updated. 
     * Called on both logical sides.
     * @param world World to operate within
     * @param index The index of the AnchorPoint that was updated.
     */
    public default void onAnchorSynced(Level world, int index) {
        
    }

    public abstract T getSource();

    public default boolean isInteractable() { return true; }
    public default boolean isVisible() { return true; }
    public default boolean isMovable() { return false; }

    /**
     * This method returns an arbitrary expression (a {@link Visial} which is executed when drawing 
     * {@link AnchorPoint AnchorPoints} belonging to this Griddable to the overlay. It's used in 
     * the {@link AnchorGUILayer} to add an image of anything you want to the highlighted tooltip.
     * This image is used in place of the goggle item that appears in standard Create google tooltips.
     * @return An Item that contains at least one item. This item can be drawn to GUI elements.
     */
    public default @Nullable Visual getVisual() {
        return null;
    }

    @FunctionalInterface
    public interface Visual {
        public abstract void draw(AnchorPoint selected, ArrayList<Component> tooltip, int posX, int posY, GuiGraphics graphics);
    }
}
