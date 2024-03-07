package com.quattage.mechano.foundation.electricity.power.features;

import java.util.List;

import com.simibubi.create.foundation.utility.Pair;

public class GridPath {
    private final GridVertex[] path;

    public GridPath(GridVertex[] path) {
        this.path = path;
    }

    public GridPath(List<GridVertex> path) {
        this.path = (GridVertex[])path.toArray();
    }

    public Pair<GridVertex, GridVertex> getEnds() {
        return Pair.of(path[0], path[path.length - 1]);
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
