package com.quattage.mechano.foundation.electricity.power.features;

import java.util.Collection;

public class GridPath {

    private final GridVertex[] path;
    private final int rate;

    public GridPath(GridVertex[] path, int rate) {
        this.path = path;
        this.rate = rate;
    }

    public GridPath(Collection<GridVertex> members, int rate) {
        this.rate = rate;
        this.path = members.toArray(GridVertex[]::new);
    }

    public int getRate() {
        return rate;
    }

    public GridVertex getStart() {
        return path[0];
    }

    public GridVertex getEnd() {
        return path[path.length - 1];
    }

    public GIDPair getHashable() {
        return new GIDPair(path[0].getGID(), path[path.length - 1].getGID());
    }

    public GridVertex[] members() {
        return path;
    }

    public int size() { 
        return path.length;
    }

    public String toString() {
        if(path.length == 0) return "Path {EMPTY}";
        String out = "Path {";

        int x = 0;
        for(GridVertex vert : path) {
            x++;
            out += vert.posAsString() + (x < path.length ? " -> " : "}");
        }
        return out;
    }

    public boolean isEnd(GridVertex other) {
        return path[0].equals(other) || path[path.length - 1].equals(other); 
    }

    public boolean contains(GridVertex other) {
        for(GridVertex vert : path)
            if(other.equals(vert)) return true;
        return false;
    }
}
