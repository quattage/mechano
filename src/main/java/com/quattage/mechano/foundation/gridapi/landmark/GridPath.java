package com.quattage.mechano.foundation.gridapi.landmark;

public class GridPath {
    
    private GridLink[] path;

    public static GridPath makeProvisional() {
        // TODO context checks? Newing an empty grid path may be unsafe in certain circumstances.
        return new GridPath(new GridLink[0]);
    }

    public GridPath(GridLink[] path) {
        this.path = path;
    }

    public void add(GridLink member) {
        GridLink[] copy = new GridLink[path.length + 1];
        System.arraycopy(path, 0, copy, 0, path.length);
        copy[path.length] = member;
        this.path = copy;
    }

    public boolean isValid() {
        return path.length >= 2;
    }

    @Override
    public String toString() {
        String output = "";
        for(int x = 0; x < path.length; x++) {
            GridLink link = path[x];
            output += link == null ? "null" : link.toString();
            if(x < path.length - 1) output += ", ";
        }
        return "[" + output + "]";
    }
}
