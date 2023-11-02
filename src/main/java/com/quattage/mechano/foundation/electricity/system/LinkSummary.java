package com.quattage.mechano.foundation.electricity.system;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/***
 * A LinkSummary serves as a cache to store a valid path between two SystemVertices.
 */
public class LinkSummary {
    
    private final SystemVertex target;
    private final HashSet<SVID> pathToTarget = new HashSet<SVID>();

    private int maxThroughput = 0;

    public LinkSummary(SystemVertex target, SVID[] path) {
        for(SVID id : path) 
            pathToTarget.add(id);
        this.target = target;
    }

    public LinkSummary(SystemVertex target, ArrayList<SVID> path) {
        for(SVID id : path) 
            pathToTarget.add(id);
        this.target = target;
    }

    public SystemVertex getTarget() {
        return target;
    }



    public boolean existsInPath(SVID id) {
        return true;
    }
}
