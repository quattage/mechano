package com.quattage.mechano.foundation.api.landmark;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.api.ServerMatrix;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.classifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.AnchorPointSyncPacket;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;

/**
 * A GridNode is a functional implementation of {@link GridIdentifier} and provides 
 * access to the Y axis of an adjacency list defined by the {@link ServerMatrix}.
 * Nodes are hashed by <code>X, Y, Z, and I</code>, where <code>XYZ</code> describes the 
 * position in the world, and <code>I</code> is the index of the node at that block 
 * position. Multiple nodes may occupy the same block.
 */
public class GridNode implements Iterable<GridLink> {

    // all fields are null if this GridNode has been destroyed
    private @Nullable ServerMatrix owner;
    private @Nullable AnchorPointable<?> points;
    private @Nullable GridUUID address;
    private @Nullable ObjectArrayList<GridLink> links = new ObjectArrayList<>();


    public GridNode(ServerMatrix owner, AnchorPointable<?> points, GridUUID address) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(points);
        Objects.requireNonNull(address);
        this.owner = owner;
        this.points = points;
        this.address = address;
    }

    public GridNode(ServerMatrix owner, AnchorPointable<?> points) {
        Objects.requireNonNull(owner);
        Objects.requireNonNull(points);
        Objects.requireNonNull(address);
        this.owner = owner;
        this.points = points;
        this.address = points.createAddress();
    }

    /**
     * Removes all links from this GridNode that point to the given
     * ending address
     * @param destroyer Entity responsible for removing the links
     * @param address
     */
    public void removeLinksInvolving(@Nullable Entity destroyer, GridUUID address) {
        assertNotDestroyed();
        Iterator<GridLink> linksIterator = links.iterator();
        while(linksIterator.hasNext()) {
            GridLink link = linksIterator.next();
            if(link.endsWith(address) || !link.startsWith(this.getAddress())) {
                linksIterator.remove();
                link.getTransmitter().onConnectionDestroyed(owner.getWorld(), destroyer, link);
                points.onConnectionDestroyed(owner.getWorld(), link);
            }
        }
        if(links.isEmpty()) points.getSurrogate().destroy();
    }

    /**
     * Removes every link that involves this GridNode, from both itself
     * and all other GridNodes that reference this one.
     * @param notify
     */
    public void wipeLinks(boolean notify) {
        assertNotDestroyed();
        Iterator<GridLink> linksIterator = links.iterator();
        while(linksIterator.hasNext()) {
            GridLink link = linksIterator.next();
            linksIterator.remove();
            if(!notify) continue;
            link.getTransmitter().onConnectionDestroyed(owner.getWorld(), null, link);
            points.onConnectionDestroyed(owner.getWorld(), link);
        }
        points.getSurrogate().destroy();
    }

    public CompoundTag writeTo(CompoundTag in) {
        assertNotDestroyed();
        UUIDDiscriminator.write(address, in);
        ListTag serializedLinks = new ListTag();
        for(GridLink link : links)
            serializedLinks.add(link.writeTo(new CompoundTag()));
        in.put("links", serializedLinks);
        return in;
    }

    public boolean isValid() {
        return true;
    }

    public ServerMatrix getOwner() {
        return owner;
    }

    public AnchorPointable<?> getAnchorPoints() {
        return points;
    }

    public GridUUID getAddress() {
        return address;
    }

    public boolean hasLinks() {
        return links.size() > 0;
    }

    public void notifyHost() {
        if(points != null)
            CatnipServices.NETWORK.sendToAllClients(new AnchorPointSyncPacket(address, (byte)(links.size() - 128), true));
    }

    public int getLinkCount() {
        return links == null ? 0 : links.size();
    }

    public GridNode prime(int size) {
        links.ensureCapacity(size);
        return this;
    }

    public GridNode trim() {
        links.trim();
        return this;
    }

    public void addLink(GridLink link) {
        if(link.getStart().equals(this.getAddress())) {
            links.add(link);
            return;
        }
        throw new IllegalArgumentException("Attempted to add invalid link [" + link.getStart().toString(owner.getWorld()) 
            + " -> " + link.getEnd().toString(owner.getWorld()) + "] - Unmatched source for GridNode at " + getAddress().toString(owner.getWorld()));
    }

    public boolean hasLink(GridLink link) {
        if(!link.getStart().equals(this.getAddress())) return false;
        return links.contains(link);
    }

    @Override
    public Iterator<GridLink> iterator() {
        return links.iterator();
    }

    public void forEachLink(Consumer<GridLink> cons) {
        assertNotDestroyed();
        for(int x = 0; x < links.size(); x++)
            cons.accept(links.get(x));
    }

    private void assertNotDestroyed() {
        if(owner == null || points == null || links == null)
            throw new IllegalStateException("An operation attempted to run on a GridNode that has already been destroyed. (This Node has potentially leaked!)");
    }

    public void nullify() {
        this.owner = null;
        this.points = null;
        this.links = null;
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
