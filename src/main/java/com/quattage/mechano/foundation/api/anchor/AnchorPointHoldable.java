
package com.quattage.mechano.foundation.api.anchor;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.landmark.GridLink;
import com.quattage.mechano.foundation.api.landmark.uuid.GridUUID;
import com.quattage.mechano.foundation.api.transmitter.Transmitable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface AnchorPointHoldable {

    public void constructAnchors(AnchorArray.Builder anchors);

    public AnchorArray getAnchors();

    public default AnchorPoint getAnchor(int index) {
        return getAnchors().getByIndex(index);
    }

    public default void refreshAnchors(BlockState newState) {
        if(newState == null) return;
        getAnchors().updateOrientation(newState);
    }

    public default CompoundTag writeTo(CompoundTag tag) {
        ListTag list = new ListTag();
        for(int x = 0; x < getAnchors().size(); x++) {
            AnchorPoint anchor = getAnchor(x);
            list.add(anchor.writeTo(new CompoundTag()));
        }
        tag.put("anchors", list);
        return tag;
    }

    public default void readFrom(CompoundTag tag) {
        ListTag list = tag.getList("anchors", Tag.TAG_COMPOUND);
        for(int x = 0; x < list.size(); x++)
            getAnchor(x).initializeFrom(list.getCompound(x));
    }

    public Level getWorld();
    public DispatchedAnchorNode getSurrogate();
    public GridUUID createAddress();

    public default boolean containsAnchor(AnchorPoint anchor) {
        if(anchor == null || anchor.getAddress().getIndex() < 0 || anchor.getAddress().getIndex() >= getAnchors().size()) 
            return false;
        for(int x = 0; x < getAnchors().size(); x++) {
            AnchorPoint other = getAnchor(x);
            if(other == null) continue;
            if(anchor.getAddress().equals(other.getAddress()))
                return true;
        }
        return false;
    }

    /**
     * A decorative string used for debugging for display in the 
     * {@link com.quattage.mechano.infrastructure.manifest.GridManifestGenerator manifest}
     * @return
     */
    public default String describeState() {
        return null;
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
     * Called when this holder is initially registered within a {@link PowerGrid}.
     * 
     * @param world World to operate within
     * @param grid The grid that this holder was added to
     */
    public default void onAddedToGrid(Level world, PowerGrid grid) {
        
    }

    /**
     * Called whenever a connection is made to/from this holder
     * @param world World to operate within
     * @param connection The GridLink representing the connection that was added
     */
    public default void onConnectionMade(Level world, GridLink connection) {
        
    }

    /**
     * Called whenever a connection is removed to/from this holder
     * @param world World to operate within
     * @param connection The connection that was destroyed. Note that this method is called AFTER the GridLink is removed from the network, so this connection's reference is stale and should't be stored.
     */
    public default void onConnectionBroken(Level world, GridLink connection) {
        
    }

    /**
     * This method returns an arbitrary ItemStack which can be 
     * drawn to GUIs. It's used in the {@link com.quattage.mechano.foundation.api.anchor.AnchorGUILayer}
     * to label the highlight tab.
     * @return An ItemStack that contains at least one item. This item can be drawn to GUI elements.
     */
    public default ItemStack getVisualStack() {
        return null;
    }
}
