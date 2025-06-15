package com.quattage.mechano.foundation.api.landmark;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.jetbrains.annotations.ApiStatus;

import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.anchor.AnchorPointHoldable;
import com.quattage.mechano.foundation.api.landmark.uuid.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.AnchorPointSyncPacket;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/**
 * A GridNode is a functional implementation of {@link GridIdentifier} and provides 
 * access to the Y axis of an adjacency list defined by the {@link PowerGrid}.
 * Nodes are hashed by <code>X, Y, Z, and I</code>, where <code>XYZ</code> describes the 
 * position in the world, and <code>I</code> is the index of the node at that block 
 * position. Multiple nodes may occupy the same block.
 */
public class GridNode {

    // all fields are null if this GridNode has been destroyed
    private @Nullable PowerGrid owner;
    private @Nullable AnchorPointHoldable holder;
    private @Nullable GridUUID address;

    /**
     * A list of links to other nodes.
     * This should never be modified directly.
     * Make any changes you need through the 
     * {@link com.quattage.mechano.foundation.api.GlobalServerGrid GlobalServerGrid}
     */
    @ApiStatus.Internal
    public @Nullable ObjectArrayList<GridLink> links = new ObjectArrayList<>();


    public GridNode(PowerGrid owner, AnchorPointHoldable holder, GridUUID address) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(holder);
        Objects.requireNonNull(address);
        this.owner = owner;
        this.holder = holder;
        this.address = address;
    }

    public GridNode(PowerGrid owner, AnchorPointHoldable holder) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(holder);
        Objects.requireNonNull(address);
        this.owner = owner;
        this.holder = holder;
        this.address = holder.createAddress();
    }

    /**
     * Removes all links from his GridNode that point to the given
     * ending address
     * @param address
     */
    public void removeLinksInvolving(GridUUID address) {
        assertNotDestroyed();
        Iterator<GridLink> linksIterator = links.iterator();
        while(linksIterator.hasNext()) {
            GridLink link = linksIterator.next();
            if(link.endsWith(address))
                linksIterator.remove();
        }
        if(links.isEmpty())
            holder.getSurrogate().severAndForget();
    }

    public void wipeLinks(boolean notify) {
        assertNotDestroyed();
        Iterator<GridLink> it = links.iterator();
        while(it.hasNext()) {
            GridLink thisLink = it.next();
            it.remove();
            if(!notify) continue;
            thisLink.transmitter.onConnectionDestroyed(owner.getWorld(), null, thisLink);
            holder.onConnectionBroken(owner.getWorld(), thisLink);
        }
    }

    public CompoundTag writeTo(CompoundTag in) {
        assertNotDestroyed();
        address.writeTo(in);
        ListTag serializedLinks = new ListTag();
        for(GridLink link : links)
            serializedLinks.add(link.writeTo(new CompoundTag()));
        in.put("links", serializedLinks);
        return in;
    }

    public void forEachLink(Consumer<GridLink> cons) {
        assertNotDestroyed();
        for(int x = 0; x < links.size(); x++) {
            cons.accept(links.get(x));
        }
    }

    public boolean isValid() {
        return true;
    }

    public PowerGrid getOwner() {
        return owner;
    }

    public AnchorPointHoldable getHolder() {
        return holder;
    }

    public GridUUID getAddress() {
        return address;
    }

    public boolean hasLinks() {
        return links.size() > 0;
    }

    private void assertNotDestroyed() {
        if(owner == null || holder == null || links == null)
            throw new IllegalStateException("An operation attempted to run on a GridNode that has already been destroyed. (This Node has potentially leaked!)");
    }

    public void nullify() {
        this.owner = null;
        this.holder = null;
        this.links = null;
    }

    public void notifyHost() {
        if(holder != null)
            CatnipServices.NETWORK.sendToAllClients(new AnchorPointSyncPacket(address, (byte)(links.size() - 128), true));
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof GridNode that)) return false;
        return this.address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }




    
}
