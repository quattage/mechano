package com.quattage.mechano.foundation.electricity.power.features;

import java.util.List;
import java.util.Set;

import com.quattage.mechano.Mechano;
import com.simibubi.create.foundation.utility.Pair;

public class GridPath {

    private final GridVertex[] path;
    private final int rate;

    public GridPath(GridVertex[] path, int rate) {
        this.path = path;
        this.rate = rate;
    }

    public GridPath(Set<GridVertex> members, int rate) {
        this.rate = rate;
        this.path = members.toArray(GridVertex[]::new);
    }



    public Pair<GridVertex, GridVertex> getEnds() {
        return Pair.of(path[0], path[path.length - 1]);
    }

    public GIDPair getHashable() {
        return new GIDPair(path[0].getGID(), path[path.length - 1].getGID());
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
