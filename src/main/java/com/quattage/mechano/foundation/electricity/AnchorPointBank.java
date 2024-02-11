package com.quattage.mechano.foundation.electricity;

import java.util.ArrayList;
import java.util.HashSet;

import javax.annotation.Nullable;

import org.joml.Vector3f;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.utility.Pair;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Triplet;


/***
 * An Anchor
 */
public class AnchorPointBank<T extends BlockEntity> {
    
    public final T target;
    private final AnchorPoint[] anchorPoints;

    @Nullable
    private final RelativeDirection[] interfaceDirections;
    

    public AnchorPointBank(T target, ArrayList<AnchorPoint> nodesToAdd, ArrayList<RelativeDirection> dirsToAdd) {
        this.target = target;
        this.anchorPoints = populateNodes(nodesToAdd);
        this.interfaceDirections = populateJunctions(dirsToAdd);
    }

    @Nullable
    public static AnchorPointBank<?> getAnchorPointBankAt(Level world, BlockPos pos) {
        if(world == null) return null;
        if(pos == null) return null;
        BlockEntity be = world.getBlockEntity(pos);
        if(be instanceof WireAnchorBlockEntity wbe)
            return wbe.getAnchorBank();
        return null;
    }

    private AnchorPoint[] populateNodes(ArrayList<AnchorPoint> nodesToAdd) {
        if(nodesToAdd == null) 
            throw new NullPointerException("Cannot instantiate new AnchorPointBank instance - nodesToAdd is null!");
        if(nodesToAdd.isEmpty()) 
            throw new IllegalArgumentException("Cannot instantiate new AnchorPointBank instance - nodesToAdd is empty!");

        AnchorPoint[] out = new AnchorPoint[nodesToAdd.size()];
        for(int x = 0; x < out.length; x++)
            out[x] = nodesToAdd.get(x);
        return out;
    }

    private RelativeDirection[] populateJunctions(ArrayList<RelativeDirection> dirsToAdd) {
        if(dirsToAdd == null || dirsToAdd.isEmpty())
            return null;

        RelativeDirection[] out = new RelativeDirection[dirsToAdd.size()];
        for(int x = 0; x < out.length; x++)
            out[x] = dirsToAdd.get(x);
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

    public Pair<AnchorPoint, Double> getClosestAnchor(Vec3 hit) {

        AnchorPoint closestAnchor = null;
        double closestDistance = -1;

        for(AnchorPoint anchor : anchorPoints) {
            Vec3 center = anchor.getPos();
            double distance = Math.abs(hit.distanceTo(center));

            //if(distance > anchor.getSize() * 6f) continue;

            if(distance < closestDistance || closestDistance == -1) {
                closestAnchor = anchor;
                closestDistance = distance;
            }
        }

        return Pair.of(closestAnchor, closestDistance);
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

    public void destroy() {
        for(AnchorPoint anchor : anchorPoints) {
            NETWORK.destroyVertex(anchor.getID());
        }
    }
}
