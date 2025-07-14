package com.quattage.mechano.foundation.api.landmark;

import java.util.Iterator;
import java.util.Objects;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.ServerMatrix;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.UpdateResponse.AnchorSyncHolder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.LevelReader;

/**
 * A GridNode is the primary functional element of the {@link SidedGridDispatcher Grid API} 
 * and provides access to the Y axis of an adjacency list defined by the {@link ServerMatrix}.
 */
public class GridNode implements Iterable<GridLink> {

    private final ServerMatrix owner;

    // all fields are null if this GridNode has been destroyed
    private @Nullable Griddable<?> host;
    private @Nullable GridUUID address;
    private @Nullable ObjectArrayList<GridLink> links = new ObjectArrayList<>();


    public static @Nullable GridNode getFrom(LevelReader world, GridUUID addr, @Nullable Griddable<?> points) {
        Objects.requireNonNull(world);
        Objects.requireNonNull(addr);
        if(world.isClientSide()) return null;
        if(points == null) return null;
        SurrogateNode surrogate = points.getSurrogate();
        if(surrogate == null) return null;
        if(!surrogate.isSynced(world)) return null;
        ServerMatrix matrix = surrogate.getOwnerMatrix();
        if(matrix == null || matrix.nodes == null) return null;
        return matrix.nodes.get(addr);
    }

    /**
     * Gets the node at the given address, or creates a new one if
     * no node at this address exists.
     * <p>
     * Note that if the returned GridNode is newly created, it will be blank. 
     * Blank GridNodes that have no links should not persist in the LocalMatrix 
     * for long, since they represent dead ends.
     * @param instantiator The ServerMatrix that is responsible for calling this method. This matrix will be searched
     * for the provided address.
     * @param address Address to get or add (Compatable with any type outlined by {@link NodeMap#get})
     * @param log <code>true</code> if calls to this method should produce error logs when things go wrong
     * @return A (new or preexisting) GridNode at the specified address.
     * @throws NullPointerException if any field is null
     * @throws IllegalStateException if <code>instantiator</code> has been {@link ServerMatrix#destroy destroyed.}
     */
    public static @Nullable GridNode getOrCreateOnLoad(ServerMatrix instantiator, GridUUID address, boolean log) {
        Objects.requireNonNull(instantiator);
        Objects.requireNonNull(address);
        instantiator.assertNotDestroyed();
        GridNode node = instantiator.nodes.get(address);
        if(node != null) return node;
        Griddable<?> points = address.getAnchorPoints(instantiator.getWorld());
        if(points == null) {
            if(log) Mechano.LOGGER.error("Failed to instantiate provisional node at " + address 
                + " - No in-world reference to this address could be found!");
            return null;
        }
        node = new GridNode(instantiator, points, address);
        instantiator.nodes.add(node);
        points.getSurrogate().sync(instantiator.getWorld(), instantiator);
        AnchorSyncHolder.of(node).sendToClients();
        return node;
    }

    /**
     * Gets the node at the given address, or creates a new one if
     * no node at this address exists. Takes in a reference to 
     * a constituent {@link Griddable} to search for pre-existing nodes.
     * <p>
     * Note that if the returned GridNode is newly created, it will be blank. 
     * Blank GridNodes that have no links should not persist in the LocalMatrix 
     * for long, since they represent dead ends.
     * @param instantiator The ServerMatrix that is responsible for calling this method. This matrix will be searched
     * for the provided address.
     * @param address Address to get or add (Compatable with any type outlined by {@link NodeMap#get})
     * @return A (new or preexisting) GridNode at the specified address.
     * @throws NullPointerException if any field is null
     * @throws IllegalStateException if the ServerMatrix belonging to <code>points</code> has been {@link ServerMatrix#destroy destroyed.}
     * @throws IllegalArgumentException if <code>points</code> is not synced or has no valid surrogate
     */
    public static @Nullable GridNode getOrCreate(ServerMatrix instantiator, Griddable<?> points, GridUUID address) {
        Objects.requireNonNull(points);
        Objects.requireNonNull(address);
        if(points.getSurrogate() == null || !points.getSurrogate().isSynced(points.getWorld())) {
            throw new IllegalArgumentException("Failed while retrieving GridNode at " + (points.getWorld() == null ? address.toString() 
            : address.toString(points.getWorld())) + " - The Griddable instance provided is not synced!");
        }
        instantiator.assertNotDestroyed("Failed while retrieving GridNode at " + (points.getWorld() == null ? address.toString() 
            : address.toString(points.getWorld())) + " - The matrix bound to the provided surrogate has been destroyed!");
        GridNode node = instantiator.nodes.get(address);
        if(node != null) return node;
        node = new GridNode(instantiator, points, address);
        instantiator.nodes.add(node);
        points.getSurrogate().sync(instantiator.getWorld(), instantiator);
        AnchorSyncHolder.of(node).sendToClients();
        return node;
    }

    public static GridNode createNew(ServerMatrix instantiator, Griddable<?> points, GridUUID address) {
        Objects.requireNonNull(instantiator);
        Objects.requireNonNull(address);
        Objects.requireNonNull(points);
        GridNode node = new GridNode(instantiator, points, address);
        instantiator.nodes.add(node);
        points.getSurrogate().sync(instantiator.getWorld(), instantiator);
        return node;
    }

    private GridNode(ServerMatrix owner, Griddable<?> points, GridUUID address) {
        this.owner = owner;
        this.host = points;
        this.address = address;
    }

    public void syncToClients() {
        assertNotDestroyed();
        AnchorSyncHolder.of(this).sendToClients();
    }

    public int getLinkCount() {
        assertNotDestroyed();
        return links == null ? 0 : links.size();
    }

    public GridNode prime(int size) {
        assertNotDestroyed();
        links.ensureCapacity(size);
        return this;
    }

    public GridNode trim() {
        assertNotDestroyed();
        links.trim();
        return this;
    }

    public ServerMatrix getOwner() {
        return owner;
    }

    public Griddable<?> getAnchorPoints() {
        return host;
    }

    public GridUUID getAddress() {
        return address;
    }

    public boolean hasLinks() {
        return links != null && links.size() > 0;
    }

    public boolean hasLink(GridLink link) {
        if(!link.getStart().equals(this.getAddress())) return false;
        return links.contains(link);
    }

    @Override
    public Iterator<GridLink> iterator() {
        return links.iterator();
    }

    public void assertNotDestroyed() {
        assertNotDestroyed("An operation attempted to run on a GridNode that has already been destroyed. (This Node has potentially leaked!)");
    }

    protected void assertNotDestroyed(String message) {
        if(host == null || links == null)
            throw new IllegalStateException(message);
    }

    public void nullify() {
        this.host = null;
        this.links = null;
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

    public void addLink(GridLink link) {
        assertNotDestroyed();
        if(!link.getStart().equals(this.getAddress())) {
            throw new IllegalArgumentException("Attempted to add invalid link [" + link.getStart().toString(owner.getWorld()) 
            + " -> " + link.getEnd().toString(owner.getWorld()) + "] - Unmatched source for GridNode at " + getAddress().toString(owner.getWorld()));
        }
        if(links.contains(link)) {
            Mechano.LOGGER.warn("Skipped the addition of a repeat link [" + link.getStart().toString(owner.getWorld()) + " -> " + link.getEnd().toString(owner.getWorld()) + "]");
            return;
        }
        links.add(link);
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
