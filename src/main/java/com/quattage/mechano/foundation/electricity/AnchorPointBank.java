package com.quattage.mechano.foundation.electricity;

import java.util.ArrayList;
import java.util.HashSet;

import com.quattage.mechano.foundation.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.foundation.utility.Pair;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Triplet;


/***
 * An Anchor
 */
public class AnchorPointBank<T extends BlockEntity> {

    public static final float HITFAC = 4f;

    private final AnchorPoint[] anchorPoints;
    public final T target;

    public AnchorPointBank(T target, ArrayList<AnchorPoint> nodesToAdd, HashSet<RelativeDirection> dirsToAdd, 
        int capacity, int maxRecieve, int maxExtract, int energy) {
        this.target = target;
        this.anchorPoints = populateNodes(nodesToAdd);
    }

    private AnchorPoint[] populateNodes(ArrayList<AnchorPoint> nodesToAdd) {
        if(nodesToAdd == null) 
            throw new NullPointerException("Cannot instantiate new NodeBank instance - nodesToAdd is null!");
        if(nodesToAdd.isEmpty()) 
            throw new IllegalArgumentException("Cannot instantiate new NodeBank instance - nodesToAdd is empty!");

        AnchorPoint[] out = new AnchorPoint[nodesToAdd.size()];
        for(int x = 0; x < out.length; x++) {
            out[x] = nodesToAdd.get(x);
        }
        return out;
    }

    public Level getWorld() {
        return target.getLevel();
    }

    /***
     * Compares AnchorPointHolders
     * @param other Object to compare. 
     * @return True if both AnchorPointHolders share the same target BlockPos.
     */
    public boolean equals(Object other) {
        if(other instanceof AnchorPointBank<?> otherBank) 
            return this.target.getBlockPos().equals(otherBank.target.getBlockPos());
        return false;
    }

    public int hashCode() {
        return target.getBlockPos().hashCode();
    }

    /***
     * Compares this NodeBank's location with a given BlockPos.
     * @param posToCheck Blockpos to check
     * @return True if this NodeBank's target BlockEntity is located
     * at the given BlockPos
     */
    public boolean isAt(BlockPos posToCheck) {
        if(posToCheck == null) return false;
        return this.target.getBlockPos().equals(posToCheck);
    }

    public AnchorPointBank<T> reflectStateChange(BlockState state) {
        for(AnchorPoint node : anchorPoints)
            node.update(state);
        return this;
    }

    /*** 
     * The length of this NodeBank
     * @return int representing how many ElectricNodes can be held in this NodeBank
     */
    public int length() {
        return anchorPoints.length;
    }

    /***
     * @return An array containing all ElectricNodes in this NodeBank.
     */
    public AnchorPoint[] getAnchorPoints() {
        return anchorPoints;
    }


    /***
     * Returns a Pair representing a list of all ElectricNodes in this bank. <p>
     * The first member of the Pair is a list of all ElectricNodes, and the second member of the
     * Pair is an Integer index of the closest ElectricNode in the list to the given Vec3 position.
     * @param hit Vec3 position. Usually this would just be <code>BlockHitResult.getLocation()</code>
     * @return A Pair containing a list of all ElectricNodes.
     */
    public Pair<AnchorPoint[], Integer> getAllNodes(Vec3 hit) {
        return getAllNodes(hit, 0.0f);
    }

    /***
     * Returns a Pair representing a list of all ElectricNodes in this bank. <p>
     * The first member of the Pair is a list of all ElectricNodes, and the second member of the
     * Pair is an Integer index of the closest ElectricNode in the list to the given Vec3 position.
     * @param tolerance Any distance higher than this number will be disregarded as too far away,
     * and will return null.
     * @param hit Vec3 position. Usually this would just be <code>BlockHitResult.getLocation()</code>
     * @return A Pair containing a list of all ElectricNodes.
     */
    public Pair<AnchorPoint[], Integer> getAllNodes(Vec3 hit, float tolerance) {

        Pair<AnchorPoint[], Integer> out = Pair.of(getAll(), -1);
        if(length() == 1) return Pair.of(getAll(), 0);

        double lastDistance = 256;

        for(int x = 0; x < anchorPoints.length; x++) {
            Vec3 center = anchorPoints[x].getLocation();
            double currentDistance = Math.abs(hit.distanceTo(center));
            
            if(tolerance == 0) tolerance = anchorPoints[x].getSize() * 5f;
            if(currentDistance > tolerance) continue;
            if(currentDistance < lastDistance) out.setSecond(x);
            lastDistance = currentDistance;
        }

        return out;
    }

    /***
     * Continually searches in the area surrounding the player's look direction for 
     * BlockEntities that possess AnchorPoints.
     * @param world World to operate within
     * @param start Vec3 starting position of the search (camera posiiton)
     * @param end Vec3 ending position of the search (BlockHitResult)
     * @param scope (Optional, default 5) Width (in blocks) of the "cone" that is searched.
     * Must be an odd number >= 3.
     * @return An ArrayList of pairs, where the first member is the NodeBank itself, and the
     * second member is the point along the ray that is closest to the NodeBank.
     */
    public static ArrayList<Pair<AnchorPointBank<?>, Vec3>> findBanksAlongRay(Level world, Vec3 start, Vec3 end) {
        return findBanksAlongRay(world, start, end, 5);
    }

    /***
     * Continually searches in the area surrounding the player's look direction for 
     * BlockEntities that possess AnchorHolders.
     * @param world World to operate within
     * @param start Vec3 starting position of the search (camera posiiton)
     * @param end Vec3 ending position of the search (BlockHitResult)
     * @param scope (Optional, default 5) Width (in blocks) of the "cone" that is searched.
     * Must be an odd number >= 3.
     * @return An ArrayList of pairs, where the first member is the AnchorHolder itself, and the
     * second member is the point along the ray that is closest to that AnchorHolder.

     */
    public static ArrayList<Pair<AnchorPointBank<?>, Vec3>> findBanksAlongRay(Level world, Vec3 start, Vec3 end, int scope) {
        ArrayList<Pair<AnchorPointBank<?>, Vec3>> out =  new ArrayList<Pair<AnchorPointBank<?>, Vec3>>();
        int maxIterations =  (int)((AnchorPointBank.HITFAC * 0.43) * start.distanceTo(end));

        if(scope % 2 == 0) scope += 1;
        if(scope < 3) scope = 3;
        
        // TODO jeezy creezy refectoreeni 
        // steps through in a straight line out away from the start to the end.
        for(int iteration = 0; iteration < maxIterations; iteration++) {
            float percent = iteration / (float) maxIterations;
            Vec3 lookStep = start.lerp(end, percent);

            BlockPos origin = VectorHelper.toBlockPos(lookStep);

            // nested loops here step through the surrounding area in a cube
            for(int y = 0; y < scope; y++) {
                for(int x = 0; x < scope; x++) {
                    for(int z = 0; z < scope; z++) {
                        Vec3i boxOffset = new Vec3i(
                            (int)(x - (scope / 2)), 
                            (int)(y - (scope / 2)), 
                            (int)(z - (scope / 2))
                        );

                        if(world.getBlockEntity(origin.offset(boxOffset)) instanceof WireNodeBlockEntity ebe) {

                            if(ebe.nodeBank == null) continue;
                            if(out.size() == 0) {
                                out.add(Pair.of(ebe.nodeBank, lookStep));
                                continue;
                            }
                            
                            boolean alreadyExists = false;
                            for(int search = 0; search < out.size(); search++) {
                                Pair<AnchorPointBank<?>, Vec3> lookup = out.get(search);
                                if(lookup.getFirst().equals(ebe.nodeBank)) { 
                                    Vec3 bankCenter = ebe.getBlockPos().getCenter();
                                    double oldDistance = lookup.getSecond().distanceTo(bankCenter);
                                    if(oldDistance < 0.002) break;
                                    if(lookStep.distanceTo(bankCenter) > oldDistance) {
                                        out.set(search, Pair.of(ebe.nodeBank, lookStep));
                                        alreadyExists = true;
                                        break;
                                    }
                                }
                            }
                            if(!alreadyExists)
                                out.add(Pair.of(ebe.nodeBank, lookStep));
                        }
                    }
                }
            }
        }
        return out;
    }

    /***
     * See {@link #findBanksAlongRay(Level world, Vec3 start, Vec3 end) findBanksAlongRay()} 
     * for additional context.
     * @param world World to operate within
     * @param start Vec3 starting position of the search
     * @param end Vec3 ending position of the search
     * @return A Triplet -  
     * 1: List of all AnchorPoints that were deemed "close enough",
     * 2: Integer index of the closest AnchorPoint in the aforementioned list,
     * and 3: the AnchorHolder that the closest AnchorPoint belongs to.
     */
    public static Triplet<ArrayList<AnchorPoint>, Integer, AnchorPointBank<?>> findClosestNodeAlongRay(Level world, Vec3 start, Vec3 end, float tolerance) {

        int closestIndex = -1;
        AnchorPointBank<?> closestBank = null;
        double lastDist = 256;
        ArrayList<AnchorPoint> allNearbyNodes = new ArrayList<AnchorPoint>();

        for(Pair<AnchorPointBank<?>, Vec3> potential : findBanksAlongRay(world, start, end)) {
            for(AnchorPoint node : potential.getFirst().anchorPoints) {
                allNearbyNodes.add(node);
                Vec3 center = node.getLocation();
                double dist = Math.abs(potential.getSecond().distanceTo(center));

                if(tolerance == 0) tolerance = node.getSize() * 5f;
                if(dist > tolerance) continue;

                if(dist < lastDist) {
                    closestIndex = allNearbyNodes.size() - 1;
                    closestBank = potential.getFirst();
                }

            lastDist = dist;
            }
        }

        return new Triplet<ArrayList<AnchorPoint>, Integer, AnchorPointBank<?>>(allNearbyNodes, closestIndex, closestBank);
    }

    public Pair<AnchorPoint, Double> getClosestNode(Vec3 hit) {
        Pair<AnchorPoint[], Integer> out = getAllNodes(hit);
        if(out.getSecond() == -1) return null;
        AnchorPoint closest = out.getFirst()[out.getSecond()];
        return Pair.of(closest, closest.getLocation().distanceTo(hit));
    }

    /***
     * Retrieves this AnchorHolder as an array
     * @return The raw aray stored within this AnchorHolder
     */
    public AnchorPoint[] getAll() {
        return anchorPoints;
    }

    public AnchorPoint get(int index) {
        return anchorPoints[index];
    }

    public int indexOf(AnchorPoint anchor) {
        for(int x = 0; x < anchorPoints.length; x++)
            if(anchorPoints[x].equals(anchor)) return x;
        return -1;
    }

    public String toString() {
        String out = "\nAnchorHolder { \n\tTarget: " + target.getClass().getSimpleName() + " \n\tLocation: " + target.getBlockPos() + ":\n";
        for(int x = 0; x < anchorPoints.length; x++) 
            out += x + ": " + anchorPoints[x] + "\n";
        return out + "}\n\n";
    }

    /***
     * Instructs the parent BlockEntity to send the block update.
     */
    public void markDirty() {
        target.getLevel().sendBlockUpdated(
            target.getBlockPos(),
            target.getBlockState(), 
            target.getBlockState(), 
            3);
        target.setChanged();
    }
}
