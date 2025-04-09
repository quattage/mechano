package com.quattage.mechano.foundation.electricity.grid.landmarks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.block.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.WattBatteryHandlable.ExternalInteractMode;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.grid.LocalTransferGrid;
import com.quattage.mechano.foundation.electricity.impl.WireAnchorBlockEntity;

import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * A GridVertex is a logical representation of a connection point within a LocalTransferGrid.
 * Where GridEdges represent the wires themselves, a GridVertex can be thought of as a connector.
 */
public class GridVertex {

    public static final VertexComparator GUIDANCE_COMPARATOR = new VertexComparator();

    // Grid architecture
    @Nullable private LocalTransferGrid parent;
    @Nullable private WireAnchorBlockEntity host;
    private final GID id;

    // A* guidance variables
    private float f = 0;
    private float heuristic = 0;
    private float cumulative = Float.MAX_VALUE;
    private boolean visited = false;
    
    // functional variables
    private boolean isMember = false;
    public final List<GridEdge> links = new ArrayList<GridEdge>();

    public GridVertex(WireAnchorBlockEntity wbe, LocalTransferGrid parent, GID id) {
        this.parent = parent;
        this.id = id;
        this.host = wbe;
    }

    public GridVertex(Level world, LocalTransferGrid parent, GID id) {
        this.parent = parent;
        this.id = id;
        this.isMember = false;

        BlockEntity be = world.getBlockEntity(id.getBlockPos());
        if(be instanceof WireAnchorBlockEntity wbe) this.host = wbe;
        else this.host = null;
    }

    /**
     * Creates a new GridVertex from NBT. <p>
     * <strong>Important note:</strong> - This constructor makes more GridVertex objects
     * than just the one returned! Vertex data contains links to other vertices. These
     * vertices are traversed depth-first in order to make sure all links refer to the
     * same GridVertex instance at a given GID. These additional GridVertices are added
     * to <code>parent</code>.
     * @param world World to operate within
     * @param parent LocalTransferGrid initiator
     * @param in CompoundTag containing relevent data
     */
    public GridVertex(Level world, LocalTransferGrid parent, CompoundTag in) {
        this.parent = parent;
        this.id = GID.of(in);
        this.isMember = in.getBoolean("m");
        readLinks(world, in.getList("l", Tag.TAG_COMPOUND));

        if(!isEmpty()) {
            BlockEntity be = world.getBlockEntity(id.getBlockPos());
            if(be instanceof WireAnchorBlockEntity wbe) this.host = wbe;
            else this.host = null;
        } else this.host = null;
    }

    /**
     * Refreshes this GridVertex with new data from a CompoundTag.
     * Essentially the same thing as {@link GridVertex the constructor that takes a CompoundTag}, 
     * but can be used on instances that already exist.
     */
    public void readRetroactive(Level world, CompoundTag in) {
        if(hasNoHost()) {
            throw new IllegalStateException("Error reading retroactively on GridVertex " + getID() + 
                " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity is null)");
        }

        setMemberStatus(in.getBoolean("m"));
        readLinks(world, in.getList("l", Tag.TAG_COMPOUND));
    }

    public void readLinks(Level world, ListTag list) {
        for(int x = 0; x < list.size(); x++) 
            GridEdge.deserializeLink(world, getOrFindParent(), this, list.getCompound(x));
    }

    public CompoundTag writeTo(CompoundTag in) {
        id.writeTo(in);
        in.putBoolean("m", this.isMember);
        in.put("l", writeLinks());
        return in;
    }

    private ListTag writeLinks() {
        ListTag out = new ListTag();
        for(GridEdge edge : links) {
            CompoundTag edgeTag = new CompoundTag();
            edge.writeTo(edgeTag);
            out.add(edgeTag);
        }
        return out;
    }

    /**
     * Syncs this <code>GridVertex</code> to its coorresponding 
     * {@link AnchorPoint <code>AnchorPoint</code>}. This GridVertex's parent BlockEntity <strong>must</strong> be accessible when invoked.
     * For simplified syncing before BE capabilities are loaded, see {@link GridVertex#doSimplifiedSync() <code>doSimplifiedSync()</code>}.
     * @param repath If <code>TRUE</code> this method will instruct the parent {@link LocalTransferGrid <code>LocalTransferGrid</code>} to 
     * find new paths to this GridVertex, only if the status of this GridVertex was changed as a result of this call.
     * @throws NullPointerException if for any reason no cooresponding <code>AnchorPoint</code> could be found
     */
    public void doFullSync(boolean repath) {

        if(hasNoHost()) throw new IllegalStateException("Could not fully sync GridVertex at  " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");

        if(host.battery.getInteractionStatus().isInteracting()) {
            getCoorespondingAnchor().setParticipant(this);
            if(!isMember()) {
                setMemberStatus(true);
                // TODO other such goodness
            }

            if(repath) getOrFindParent().pathfindFrom(this, true, false);

        } else {
            getCoorespondingAnchor().nullifyParticipant();
            if(isMember()) {
                setMemberStatus(false);
                if(!isEmpty()) getOrFindParent().getPathManager().removePathsEndingIn(getID());
            }
        }
    }

    /**
     * Syncs this <code>GridVertex</code> to its cooresponding {@link AnchorPoint <code>AnchorPoint</code>}
     * without accessing BlockEntity capabilities. This method is designed to be invoked during the world loading
     * process. For any other case, use {@link GridVertex#doFullSync(boolean) <code>syncToHostBE()</code>}.
     */
    public void doSimplifiedSync() {

        if(hasNoHost()) throw new IllegalStateException("Could not perform simplified sync on GridVertex at  " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");

        if(isMember()) {
            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(id)) 
                    anchor.setParticipant(this);
            }
        } else {
            for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
                if(anchor.getID().equals(id)) 
                    anchor.nullifyParticipant();
            }
        }
    }

    /**
     * Nullifies the data in this GridVertex as well as any BE-sided instances that may exist.
     * Nullifying this data guarantees that accessing a GridVertex instance after it has been
     * removed from its LocalTransferGrid will throw an exception. It is reccomended call this 
     * whenever possible to enforce this convention and make bugs easier to find.
     * <p>This may or may not also speed up garbage collection, but that is not its intended purpose.
     */
    public void markRemoved() {

        if(hasNoHost()) throw new IllegalStateException("Error marking GridVertex for removal at " + id + " - This GridVertex has already been marked for removal!");
        stripHostInstances();

        this.host = null;
        links.clear();
    }

    /**
     * Instructs the cooresponding AnchorPoint to forget its cached GridVertex instance.
     * This is required in order to ensure that the BE has the most up-to-date information
     * about its GridVertices.
     */
    public void stripHostInstances() {
        for(AnchorPoint anchor : host.getAnchorBank().getAll()) {
            if(anchor.getID().equals(id)) 
                anchor.nullifyParticipant();
        }
    }

    /**
     * Broadcasts chunk changes to the client for every chunk containing GridVertices linked 
     * to this GridVertex.
     */
    public void refreshLinkedChunks() {
        final Set<BlockPos> visited = new HashSet<>(11);
        for(GridEdge edge : links) {
            GridVertex vert = edge.getDestinationVertex();
            if(!vert.isAt(id))
                visited.add(vert.id.getBlockPos());
        }
    }

    /**
     * Evaluates whether or not this GridVertex is currently compatable with the provided
     * GridVertex. <p><strong>Note:</strong> Paths are treated as directional in this context.
     * For example, <code>this.canFormPathTo(o)</code> may evaluate to true, but that does not
     * guarantee the same outcome for <code>o.canFormPathTo(this)</code>.
     * @param o GridVertex to compare
     * @return <code>TRUE</code> if a path can be formed from <code>this</code> to <code>o</code>
     */
    public boolean canFormPathTo(GridVertex o) {
        if(!this.isMember()) return false;
        if(!o.isMember()) return false;
        if(this.host == null || o.host == null) return false;
        if(this.equals(o)) return false;
        return getHostCapabilityMode().isCompatableWith(o.getHostCapabilityMode());
    }

    /**
     * Finds and returns this <code>GridVertex</code>'s cooresponding {@link AnchorPoint <code>AnchorPoint</code>}.
     * The <code>AnchorPoint</code> is required for creating a logical link between this <code>GridVertex</code> and its physicalized in-world
     * location.
     * @return The AnchorPoint at this <code>GridVertex</code>'s {@link GID <code>GID</code>}
     */
    public AnchorPoint getCoorespondingAnchor() {
        if(hasNoHost()) throw new IllegalStateException("Error getting cooresponding AnchorPoint for GridVertex at " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");
        if(host.getAnchorBank() == null) return null;
        return host.getAnchorBank().get(id.getSubIndex());
    }

    public float getF() {
        return f;
    }

    public float getStoredHeuristic() {
        return heuristic;
    }

    /**
     * Calculates a heuristic value for A* guidance based on euclidean distance and store it in this GridVertex.
     * Whenever possible, implementations should use {@link GridVertex#getAndStoreFastHeuristic(GridEdge) the faster alternative } instead.
     * @param other GridVertex to calculate heuristic with
     * @return The Euclidian distance between this vertex and the given vertex
     */
    public float getAndStoreHeuristic(GridVertex other) {
        BlockPos a = id.getBlockPos();
        BlockPos b = other.id.getBlockPos();
        heuristic = GridEdge.getEuclideanDistance(a, b);
        f = cumulative + heuristic;
        return heuristic;
    }

    /**
     * Gets the pre-computed heuristic value from the provided GridEdge and stores it in this GridVertex.
     * @param other GridEdge to grab the pre-computed heuristic from
     * @return The Euclidian distance (length) of the given edge.
     */
    public float getAndStoreFastHeuristic(GridEdge edge) {
        heuristic = edge.getDistance();
        f = cumulative + heuristic;
        return this.heuristic;
    }

    public float getCumulative() {
        return this.cumulative;
    }

    public void setCumulative(float g) {
        this.cumulative = g;
    }

    /***
     * Mark this GridVertex as visited during iteration. Used for A*
     */
    public void markVisited() {
        this.visited = true;
    }
    
    /***
     * @return <code>TRUE</code> if this GridVertex has been marked as visited by 
     * calling {@link GridVertex#markVisited() <code>markVisited()</code>}
     */
    public boolean hasBeenVisited() {
        return visited;
    }

    /***
     * Resets the F, heuristic, cumulative, and visited values
     * of this GridVertex to their defaults. 
     * @return this GridVertex, modified as a result of this call.
     */
    public GridVertex resetHeuristics() {
        this.f = 0;
        this.heuristic = 0;
        this.cumulative = Float.MAX_VALUE;
        this.visited = false;
        return this;
    }

    /*** 
     * Gets the hashable version of this GridVertex.
     * @return the GID stored within this GridVertex.
     */
    public GID getID() {
        return id;
    }

    /***
     * @param pos BlockPos to check
     * @return <code>TRUE</code> if this GridVertex resides at the provided BlockPos
     */
    public boolean isAt(BlockPos pos) {
        return id.getBlockPos().equals(pos);
    }

    /**
     * @param id GID to check
     * @return <code>TRUE</code> if this GridVertex resides at the provided GID
     */
    public boolean isAt(GID id) {
        return this.id.equals(id);
    }

    /**
     * Adds a previously created GridEdge to this GridVertex's links.
     * @throws IllegalArgumentException <code>edge</code>'s origin vertex
     * must belong to this GridVertex such that <code>edge.getOriginVertex().equals(this)</code>
     * evaluates to <code>TRUE</code> - otherwise, this exception is thrown.
     * @param edge GridEdge to add
     * @return <code>TRUE</code> if this GridVertex was modified as a result of this call
     */
    public boolean 
    addLink(GridEdge edge) {
        if(!edge.getOriginVertex().equals(this))
            throw new IllegalArgumentException("Error adding link to " + this + " - Cannot add a GridEdge whos origin doesn't belong to this GridVertex!");
        if(links.contains(edge)) return false;
        return links.add(edge);
    }

    /**
     * Adds a new edge to this GridVertex from this GridVertex to <code>other</code>
     * @param other GridVertex to link
     * @return <code>TRUE</code> if this GridVertex was modified as a result of this call
     */
    public boolean addLink(GridVertex other, int linkType) {
        if(isLinkedTo(other)) return false;
        links.add(new GridEdge(this, other, linkType));
        return true;
    }

    /**
     * Removes the link from this GridVertex to the given GridVertex and returns it.
     * @param other GID or GridVertex
     * @returns The edge that was removed from this GridVertex, or null of no edge matching the provided GridVertex was found.
     */
    @Nullable
    public GridEdge popLink(GridVertex other) {
        
        int index = -1;
        for(int x = 0; x < links.size(); x++) {
            if(links.get(x).getDestinationVertex().equals(other))
                index = x;
        }

        if(index >= 0)
            return links.remove(index);

        return null;
    }

    /**
     * Removes the link from this GridVertex to the given GridVertex and returns it.
     * @param other GID or GridVertex
     * @returns The edge that was removed from this GridVertex, or null of no edge matching the provided GridVertex was found.
     */
    @Nullable
    public GridEdge popLink(GID other) {
        int index = -1;
        for(int x = 0; x < links.size(); x++) {
            if(links.get(x).getDestinationVertex().getID().equals(other))
                index = x;
        }

        if(index >= 0) 
            return links.remove(index);

        return null;
    }

    /***
     * Checks whether this GridVertex is linked to the given GridVertex.
     * @param other GridVertex to check for 
     * @return True if this GridVertex contains a link to the given GridVertex
     */
    public boolean isLinkedTo(GridVertex other) {
        for(int x = 0; x < links.size(); x++) {
            if(links.get(x).getDestinationVertex().equals(other)) 
                return true;
        }
        return false;
    }

    /***
     * Gets the GridEdge from this GridVertex to the given GridVertex.
     * @param other GridVertex to check for 
     * @return The GridEdge whose starting position is <code>this</code> and whose ending position is <code>other</code>, or null if no such GridEdge exists.
     */
    public GridEdge getLinkTo(GridVertex other) {
        for(int x = 0; x < links.size(); x++) {
            GridEdge edge = links.get(x);
            if(edge.getDestinationVertex().equals(other)) 
                return edge;
        }
        return null;
    }

    protected void unlinkAll() {
        links.clear();
    }

    /***
     * Checks whether this GridVertex has any links attached to it
     * @return True if this GridVertex doesn't contain any links
     */
    public boolean isEmpty() {
        if(links == null) return true;
        return links.isEmpty();
    }

    public String toString() {
        String sig = isMember ? "Type - 'M'" : "Type - 'A'";
        String mode = "Mode -'";
        try {
            mode += getHostCapabilityMode().toString() + "'";
        } catch(IllegalStateException e) {
            mode += "DIRTY'";
        }
        
        return "{" + sig  + ", " + mode + ", At " + posAsString() + ", H: " + hashCode() + "}";
    }

    public String toFormattedString() {
        String sig = isMember ? "Type - §d§l'Member'§r§7" : "Type - §5§l'Actor'§r§7";
        String mode = "Mode - §r§l'" + getHostCapabilityMode().toString() + "' §r§7";
        return "§r§7(" + sig  + ", " + mode + ")\n                Host: " + posAsString() + "\n";
    }

    public String posAsString() {
        return id.toString();
    }

    /**
     * Compares equivalence of two GridVertex objects based on position. Does not compare the content
     * of either GridVertex's edge list.
     * @return <code>TRUE</code> if <code>this</code> and <code>other</code> both have the same GID position.
     */
    public boolean equals(Object other) {
        if(other instanceof GridVertex ov)
            return this.id.equals(ov.id);
        return false;
    }

    public int hashCode() {
        return id.hashCode();
    }

    public void setMemberStatus(boolean isMember) {
        this.isMember = isMember;
    }

    public boolean isMember() {
        return isMember;
    }

    public ExternalInteractMode getHostCapabilityMode() {
        if(hasNoHost()) throw new IllegalStateException("Error getting ExternalInteractMode for GridVertex at " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");
        return host.battery.getMode();
    }

    public LocalTransferGrid getOrFindParent() {
        if(parent == null) {
            if(hasNoHost()) throw new IllegalStateException("Error getting LocalTransferGrid parent for GridVertex at " + id + " - This GridVertex is invalid or has been marked for removal! (Host BlockEntity reference is null)");
            Pair<Integer, LocalTransferGrid> sub = GlobalTransferGrid.of(host.getLevel()).getSystemContaining(id);
            if(sub == null) throw new NullPointerException("Error getting LocalTransferGrid parent for GridVertex at " + id + " - No LocalTransferGrid could be found containing a GridVertex at this ID!");
            parent = sub.getSecond();
        } 
        return parent;
    }

    public void replaceParent(LocalTransferGrid parent) {
        if(parent == null) throw new NullPointerException("Error replacing GridVertex parent - Parent cannot be set to a null value!");
        this.parent = parent;
        resetHeuristics();
    }

    /**
     * A GridVertex has no host if its parent BlockEntity is null. This can happen in two ways:
     * <li> <code>-</code> No BlockEntity could be found at this GridVertex's cooresponding <code>GID</code> block position. This can only happen if the constructor fails to find a BE during NBT loading, and will leave this GridVertex in an invalid state if one cannot be found.
     * <li> <code>-</code> This GridVertex has been marked for removal, either because it was simply removed, or because it has invalid/outdated data.
     * @return <code>TRUE</code> if this GridVertex host is null.
     */
    public boolean hasNoHost() {
        return host == null;
    }

    public WireAnchorBlockEntity getHost() {
        if(hasNoHost()) throw new NullPointerException("Error getting host from GridVertex at " + id + " - This GridVertex has no host!");
        return host;
    }

    public Set<GridPath> getConnectedPaths() {
        return getOrFindParent().getPathManager().getPathsAt(getID());
    }

    // Used to sort the tentative vertex list during A* pathfinding to achieve a deterministic sorting order guided by a heuristic.
    private static class VertexComparator implements Comparator<GridVertex> {

        @Override
        public int compare(GridVertex vertA, GridVertex vertB) {
            if(vertA.f > vertB.f) return 1;
            if(vertA.f < vertB.f) return -1;

            BlockPos a = vertA.id.getBlockPos();
            BlockPos b = vertB.id.getBlockPos();
            if(a.getX() > b.getX()) return 1;
            if(a.getX() < b.getX()) return -1;
            if(a.getY() > b.getY()) return 1;
            if(a.getY() < b.getY()) return -1;
            if(a.getZ() > b.getZ()) return 1;
            if(a.getZ() < b.getZ()) return -1;
            return 0;
        }
        
    }
}