package com.quattage.mechano.foundation.electricity.grid.landmarks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGridDispatcher;
import com.quattage.mechano.foundation.electricity.watt.unit.WattUnit;
import net.createmod.catnip.data.Pair;

/**
 * A GridPath is a wrapper for an array of GridEdges which represent "leaps" from a source to a destination.
 */
public class GridPath {

    private final GridEdge[] path;
    private final float maxTransferRate;
    private float remainingTransferRate;

    public GridPath(GridEdge[] path, float lowestWatts) {
        this.path = path;
        this.maxTransferRate = lowestWatts;
        this.remainingTransferRate = maxTransferRate;
    }

    public GridPath(Collection<GridEdge> members, float lowestWatts) {
        this.path = members.toArray(GridEdge[]::new);
        this.maxTransferRate = lowestWatts;
        this.remainingTransferRate = maxTransferRate;
    }

    /**
     * Unwinds a Map, where key-value pairs represent links, into a <code>GridPath</code> and returns it. <p>
     * For example, a map containing <code> A : B,  B : C,  C: D,  D: E </code> will return a GridPath structured as <code> A, B, C, D, E </code> 
     * @param path Map of GridVertices describing a path
     * @param start The starting GridVertex, used to establish insertion order of the resulting GridPath
     * @param lowestWatts The slowest wattage rate encountered during each leap <code>path</code>, used to describe the maximum possible rate of the resulting GridPath.
     * @return A new GridPath instance, or null if no valid path could be created frorm the supplied map.
     */
    @Nullable
    public static GridPath ofUnwound(Map<GridVertex, GridVertex> path, GridVertex start, float lowestWatts) {

        if(path == null || path.isEmpty()) return null;

        final List<GridEdge> edgeList = new ArrayList<>();
        GridVertex leap = path.get(start);
        GridEdge edge = start.getLinkTo(leap);

        int x = 0;
        int max = path.size() * 2 + 1;
        if(edge == null) {
            throw new NullPointerException("Error unwinding GridPath at iteration " + x 
            + ": No link exists between " + start + " and " + leap + " from the supplied path: " + path);
        }
        edgeList.add(start.getLinkTo(leap));

        while(leap != null) {

            x++;
            if(x > max) {
                throw new UnboundGridPathException("Error unwinding GridPath - The supplied path (" 
                    + path + ") has no bounding terminus! (Loop in children)");
            }

            GridVertex next = path.get(leap);
            GridEdge nextEdge = leap.getLinkTo(next);
            leap = next;
            if(nextEdge == null) {
                if(leap == null) break;
                throw new NullPointerException("Error unwinding GridPath at iteration " + x 
                    + ": No link exists between " + leap + " and " + next + " from the supplied path: " + path);
            }
            edgeList.add(nextEdge);
        }

        return (!edgeList.isEmpty()) ? new GridPath(edgeList, lowestWatts) : null;
    }

    /**
     * @return A deep-copy of this GridPath whose vertex order is reversed
     */
    public GridPath copyAndInvert() {
        int size = path.length;
        GridEdge[] inverted = new GridEdge[size];
        for(int x = 0; x < size; x++)
            inverted[x] = path[size - x - 1].getInverse();
        return new GridPath(inverted, maxTransferRate);
    }

    /** 
     * @return A float representing the maximum watts per tick that this GridPath can convey
     */
    public synchronized float getMaxTransferRate() {
        float rate = maxTransferRate;
        for(int x = 0; x < path.length; x ++ ) {
            float thisEdgeRate = path[x].getWattsRemaining();
            if(thisEdgeRate == 0) return 0;
            if(thisEdgeRate < rate)
                rate = thisEdgeRate;
        }
        return rate;
    }

    /**
     * Marks that this edge has power flowing through it by
     * increasing every edge's load by <code>wattsToLoad</code>.
     * @param wattsToLoad Watts to increase load by
     */
    public synchronized void addLoad(WattUnit wattsToLoad) {
        if(wattsToLoad == null || wattsToLoad.hasNoPotential()) return;
        float rate = maxTransferRate;
        for(int x = 0; x < path.length; x++) {
            float thisEdgeRate = path[x].loadThroughput(wattsToLoad).getWattsRemaining();
            if(thisEdgeRate <= 0) {
                rate = 0;
                break;
            }
            rate = Math.min(thisEdgeRate, rate);
        }
        remainingTransferRate = rate;
        GlobalTransferGridDispatcher.markPathDirty(this);
    }

    /**
     * Resets every edge's power load to zero.
     */
    public synchronized void resetLoad() {
        for(int x = 0; x < path.length; x++)
            path[x].forgetLoad();
        remainingTransferRate = maxTransferRate;
    }

    /**
     * Gets the "starting" point GridVertex. <p>
     * <strong>Note:</strong> Paths do possess directionality, but this is only for ease of access.
     * A path and its inverse are interchangable.
     * The terms "start" and "end" are used here colloquially and only indicate the arbitrary order that
     * this path was made in, but this path's existence (mostly) guarantees that an inverse version exists in the grid as well.
     * @return The GridVertex at index <code>0</code> of this path.
     */
    public GridVertex getStart() {
        return path[0].getOriginVertex();
    }

    /**
     * Gets the "ending" point GridVertex. <p>
     * <strong>Note:</strong> Paths do possess directionality, but this is only for ease of access.
     * A path and its inverse are interchangable.
     * The terms "start" and "end" are used here colloquially and only indicate the arbitrary order that
     * this path was made in, but this path's existence (mostly) guarantees that an inverse version exists in the grid as well.
     * @return The GridVertex at index <code>length - 1</code> of this path.
     */
    public GridVertex getEnd() {
        return path[path.length - 1].getDestinationVertex();
    }

    /**
     * Converts this GridPath into a {@link GIDPair <code>GIDPair</code>} object
     * for easy hashing
     * @return A new GIDPair instance containing the two ends of this path.
     */
    public GIDPair getHashable() {
        return new GIDPair(getStart(), getEnd());
    }

    /**
     * @return An array representing each "leap" in this path
     */
    public GridEdge[] members() {
        return path;
    }

    /**
     * @return The amount of members in this path
     */
    public int size() { 
        return path.length;
    }

    public boolean containsVertex(GridVertex other) {
        try {
            forEachVertex(vert -> {
                // is this allowed
                if(vert.getSecond().equals(other))
                    throw new BreakoutException();
            });
        } catch(BreakoutException e) { return true; }
        
        return false;
    }

    public boolean containsKey(GID key) {
        try {
            forEachVertex(vert -> {
                if(vert.getSecond().getID().equals(key))
                    throw new BreakoutException();
            });
        } catch(BreakoutException e) { return true; }
        
        return false;
    }

    public boolean isEmpty() {
        return path.length == 0;
    }

    public void forEachVertex(Consumer<Pair<Integer, GridVertex>> op) {
        for(int x = 0; x < path.length; x++) 
            op.accept(Pair.of(x, path[x].getOriginVertex()));
        int last = path.length - 1;
        op.accept(Pair.of(last + 1, path[last].getDestinationVertex()));
    }

    public boolean equals(Object o) {
        if(!(o instanceof GridPath that)) return false;
        return this.getStart().equals(that.getStart()) && this.getEnd().equals(that.getEnd());
    }

    public int hashCode() {
        return (getStart().hashCode() + getEnd().hashCode()) * 31;
    }

    public class BreakoutException extends RuntimeException {}
}