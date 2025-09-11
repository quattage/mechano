package com.quattage.mechano.foundation.api.landmark;

import java.util.Iterator;
import java.util.Objects;

import javax.annotation.Nullable;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.ServerMatrix;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSynchronizer;
import com.quattage.mechano.foundation.api.switchboard.LinkSwapPacket;
import com.quattage.mechano.foundation.helper.Worldly;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * A GridNode is the primary functional element of the {@link SidedGridDispatcher Grid API} 
 * and provides access to the Y axis of an adjacency list defined by the {@link ServerMatrix}.
 */
public class GridNode extends GridUUID implements Iterable<GridLink>, Worldly {

    /**
     * The only time this can be reassigned is if 
     * this ServerMatrix is merged onto another.
     * ({@link #swapOwner})
     */
    private @NotNull ServerMatrix owner;

    // all fields are null if this GridNode has been destroyed
    private @Nullable Griddable<?> host;
    protected @Nullable GridUUID address;
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
        // i have no idea how, but very rarely the nodemap is null here during 
        // world load and this fix appears to have no adverse side effects.
        if(instantiator.nodes == null)
            instantiator.nodes = new NodeMap(2);
        GridNode node = instantiator.nodes.get(address);
        if(node != null) return node;
        Griddable<?> points = address.getOrFindGriddable(instantiator.getWorld());
        if(points == null) {
            if(log) Mechano.LOGGER.error("Failed to instantiate provisional node at " + address 
                + " - No in-world reference to this address could be found!");
            return null;
        }
        node = new GridNode(instantiator, points, address);
        instantiator.nodes.add(node);
        points.getSurrogate().sync(instantiator.getWorld(), instantiator);
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
        if(points.getSurrogate() == null) {
            throw new IllegalArgumentException("Failed while retrieving GridNode at " + (points.getWorld() == null ? address.toString() 
            : address.toString(points.getWorld())) + " - The Griddable instance couldn't provide a surrogate!");
        }
        instantiator.assertNotDestroyed("Failed while retrieving GridNode at " + (points.getWorld() == null ? address.toString() 
            : address.toString(points.getWorld())) + " - The matrix bound to the provided surrogate has been destroyed!");
        GridNode node = instantiator.nodes.get(address);
        if(node != null) return node;
        node = new GridNode(instantiator, points, address);
        instantiator.nodes.add(node);
        points.getSurrogate().sync(instantiator.getWorld(), instantiator);
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

    /**
     * This method is useful only in specific circumstances where the address
     * of this GridNode is outdated and needs to be re-asserted in order
     * to reflect the current in-world location of whatever this GridNode
     * is owned by. The best working example I currently have of this 
     * occuring is if a connector is moved by a Create contraption.
     * @param world World to operate within
     * @param newAddress New address to use when replacing
     * @param newHolder New holder, which contaains <code>newAddress</code>, to rebind this node to.
     * @param sendPackets Whether or not to sync changes to the client
     */
    public void replaceHolder(LevelReader world, Griddable<?> newHolder, GridUUID newAddress, boolean sendPackets) {
        if(!(world instanceof ServerLevel sl)) return;
        Objects.requireNonNull(newHolder);
        if(newHolder.getSurrogate() == null) {
            Mechano.LOGGER.warn("Skipped swapping address for " + this + " - The provided holder (" 
                + newHolder + ")" + " failed to supply a surrogate node!");
        }
        if(this.address.isUnindexed(newHolder.getSurrogate().getOrCreateAddress())) {
            Mechano.LOGGER.warn("Skipped swapping address for " + this 
                + " - The provided address is identical to the pre-existing one!");
            return;
        }
        assertNotDestroyed();
        /*
         * its important that packets are sent before any changes are made, since sendToClientsTracking()
         * may fail if the new address (and/or holder points to an Entity that hasn't yet been added to the world.
        */
        GridUUID oldAddress = this.address;
        if(sendPackets) {
            for(GridLink link : links) {
                LinkDataStorable.popAsServer(world, link);
                link.sendToClientsTracking(sl, new LinkSwapPacket(
                    AnchorSynchronizer.of(oldAddress, (byte)links.size()), 
                    AnchorSynchronizer.of(link.getEnd(), (byte)link.getEndNode().links.size()), 
                    newAddress, GridResponse.TASK_SWAP_START)
                );
            }
        }
        owner.nodes.remove(this.address);
        this.address = newAddress;
        this.host = newHolder;
        newHolder.getSurrogate().sync(world, owner);
        owner.nodes.add(this);
        for(GridLink link : links) {
            link.getStartNode().address = newAddress;
            link.getStartNode().host = newHolder;
            for(GridLink inverse : link.getEndNode()) {
                if(inverse.getEndNode().address.equals(oldAddress)) {
                    inverse.getEndNode().address = this.address;
                    inverse.getEndNode().host = newHolder;
                }
            }
            link.sendLevelUpdates(sl);
            LinkDataStorable.pushAsServer(world, link);
        }
    }

    public void broadcast(ServerLevel world) {
        broadcast(world, links.isEmpty() ? GridResponse.TASK_FORGET_ANCHORS : GridResponse.TASK_SYNC_ANCHORS);
    }

    @Override
    public void broadcast(ServerLevel world, GridResponse response) {
        if(getOwner().getWorld() == null) {
            Mechano.LOGGER.warn("Couldn't broadcast sync for node at " + getAddress() 
                + " because this address has no world!");
            return;
        }
        switch(response) {
            case TASK_CREATE_LINK, TASK_SYNC_ANCHORS -> {
                if(!hasLinks()) {
                    Mechano.LOGGER.warn("Response '" + response + "' ignored due to bad syncing context");
                    return;
                }
                getGriddable().getSurrogate().sync(getOwner().getWorld(), getOwner());
                getGriddable().onAnchorSynced(getOwner().getWorld(), getAddress().getIndex());
                AnchorSynchronizer.of(this).sendToClients(world);
            }
            case TASK_DESTROY_LINK, TASK_DESTROY_LINK_LAZY, TASK_FORGET_ANCHORS -> {
                getGriddable().getSurrogate().forgetIfNeeded(getOwner().getWorld());
                getGriddable().onAnchorSynced(getOwner().getWorld(), getAddress().getIndex());
                AnchorSynchronizer.of(this).sendToClients(world);
            }
            case null, default -> Mechano.LOGGER.error("Respose type '" + response + "' is unsupported for braodcasting");
        }
    }

    @Override
    public void sendLevelUpdates(Level world) {
        assertNotDestroyed();
        address.sendLevelUpdates(world);
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

    public int getLinkCount() {
        assertNotDestroyed();
        return links == null ? 0 : links.size();
    }

    public GridNode primeLinks(int size) {
        if(links == null) {
            Mechano.LOGGER.warn("Skipped priming destroyed GridNode at " + address);
            return this;
        }
        links.ensureCapacity(size);
        return this;
    }

    public GridNode trimLinks() {
        if(links == null) 
            return this;
        links.trim();
        return this;
    }

    @Override
    public int getSectionY(LevelReader world) {
        assertNotDestroyed();
        return address.getSectionY(world);
    }

    @Override
    public IAttachmentHolder getDataStorageHolder(LevelReader world) {
        assertNotDestroyed();
        return address.getDataStorageHolder(world);
    }

    public ServerMatrix getOwner() {
        return owner;
    }

    /**
     * Called whenever a pre-existing {@link ServerMatrix}
     * takes ownership of this GridNode. This method is only
     * to be used internally, especially during calls to 
     * {@link ServerMatrix#addAll}.
     * @param owner
     */
    @ApiStatus.Internal
    public void swapOwner(ServerMatrix owner) {
        this.owner = owner;
        host.getSurrogate().sync(owner.getWorld(), owner);
    }

    public Griddable<?> getGriddable() {
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

    public boolean isLinkedTo(GridNode other) {
        for(GridLink link : links) {
            if(link.getEnd().equals(other.getAddress())) 
                return true;
        }
        return false;
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
        this.links.clear(); // GC friendly? idk
        this.links = null;
        this.host = null;
    }

    @Override
    public void writeTo(CompoundTag in) {
        assertNotDestroyed();
        UUIDDiscriminator.write(address, in);
        ListTag serializedLinks = new ListTag();
        for(GridLink link : links) {
            link.fixDataScopes(getWorld(), true);
            serializedLinks.add(link.writeTo(new CompoundTag()));
        }
        in.put("links", serializedLinks);
    }

    @Override
    public void writeTo(ByteBuf buffer) {
        throw new UnsupportedOperationException("GridNodes cannot be written to ByteBuffers!");
    }

    @Override
    public void writeTo(RecordBuilder<?> builder) {
        throw new UnsupportedOperationException("GridNodes cannot be written to dynamic records!");
    }

    @Override
    public void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet) {
        assertNotDestroyed();
        address.sendToClientsTracking(world, packet);
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        return address.isBeingTrackedBy(player);
    }

    @Override
    public boolean isInsideOf(LevelReader world, ChunkPos chunk) {
        assertNotDestroyed();
        return address.isInsideOf(world, chunk);
    }

    @Override
    public boolean isInsideOf(LevelReader world, SectionPos section) {
        assertNotDestroyed();
        return address.isInsideOf(world, section);
    }

    @Override
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        throw new UnsupportedOperationException("Frustum checks can only be called on the client!");
    }

    @Override
    public DataScope getDataScope(LevelReader world) {
        assertNotDestroyed();
        return address.getDataScope(world);
    }

    @Override
    public void setDataScope(DataScope scope) {
        assertNotDestroyed();
        address.setDataScope(scope);
    }

    @Override
    public String describeDataScope(LevelReader world) {
        return address.describeDataScope(world) + " (Queried from active GridNode)";
    }

    @Override
    public UUIDDiscriminator getDiscriminatorType() {
        return address.getDiscriminatorType();
    }

    @Override
    public BlockPos getBlockPos(LevelReader world) {
        return address.getBlockPos(world);
    }

    @Override
    public Vec3 getPos(LevelReader world, float pTicks) {
        return address.getPos(world, pTicks);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz) {
        return address.getOffsetPos(world, pTicks, ox, oy, oz);
    }

    @Override
    public int getIndex() {
        return address.getIndex();
    }

    @Override
    public GridUUID indexedCopy(int index) {
        return address.indexedCopy(index);
    }

    @Override
    public @Nullable AnchorPoint getAnchor(ClientLevel world) {
        return address == null ? null : address.getAnchor(world);
    }

    @Override
    public @Nullable Griddable<?> getOrFindGriddable(LevelReader world) {
        return address == null ? null : address.getOrFindGriddable(world);
    }

    @Override
    public @Nullable SurrogateNode getSurrogate(LevelReader world) {
        return address == null ? null : address.getSurrogate(world);
    }

    @Override
    public float getWeight(LevelReader world) {
        return address.getWeight(world);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof GridNode that)) return false;
        return this.address.equals(that.address);
    }

    @Override
    public boolean isUnindexed(GridUUID other) {
        return address == null ? false : address.isUnindexed(other);
    }

    @Override
    public @Nullable Level getWorld() {
        return owner == null ? null : owner.getWorld();
    }

    @Override
    public String toString() {
        return "GridNode at (" + (address == null ? "NULL" : address.toString()) + ")";
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public int getPriority() {
        return this.address == null ? 0 : this.address.getPriority();
    }
}
