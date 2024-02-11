package com.quattage.mechano.foundation.electricity;

import java.util.ArrayList;
import java.util.HashSet;

import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;

/***
 * AnchorPointSearchResults are used as a way to return all relevent information in one go.
 * It's boxed up as its own object to avoid excess iteration during execution of expensive search algorithms.
 */
public class AnchorPointSearchResult {

    private final HashSet<AnchorPoint> allPoints;
    private final AnchorPoint closestPoint;
    private final AnchorPointBank<?> closestBank;
    private final double distance;
    

    public AnchorPointSearchResult(HashSet<AnchorPoint> allPoints, AnchorPoint closestPoint, AnchorPointBank<?> closestBank, double distance) {
        this.allPoints = allPoints;
        this.closestPoint = closestPoint;
        this.closestBank = closestBank;
        this.distance = distance;
    }

    /***
     * @return A HashSet of all points found within the search area
     */
    public HashSet<AnchorPoint> getAll() {
        return allPoints;
    }

    /***
     * @return The closest AnchorPoint found to the player's raycast result
     */
    public AnchorPoint getClosest() {
        return closestPoint;
    }

    /***
     * @return The AnchorPointBank that the closest AnchorPoint belongs to
     */
    public AnchorPointBank<?> getClosestBank() {
        return closestBank;
    }
    
    /***
     * @return The distance away from the player's raycast result that the closest AnchorPoint is
     */
    public double getClosestDistance() {
        return distance;
    }
}
