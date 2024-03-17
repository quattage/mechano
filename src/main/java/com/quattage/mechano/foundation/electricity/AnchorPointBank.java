package com.quattage.mechano.foundation.electricity;

import java.util.ArrayList;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.core.anchor.AnchorPoint;
import com.quattage.mechano.foundation.electricity.power.GlobalTransferGrid;
import com.quattage.mechano.foundation.electricity.power.features.GridVertex;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/***
 * The AnchorPointBank represents a collection of AnchorPoints. It also provides helpers and relevent implementation
 * for BlockEntity related actions.
 */
public class AnchorPointBank<T extends BlockEntity> {
    
    public final T target;
    public boolean isAwaitingConnection = false;
    private final AnchorPoint[] anchorPoints;

    private float time;
    private float oldTime;

    private GlobalTransferGrid net;

    public AnchorPointBank(T target, ArrayList<AnchorPoint> nodesToAdd) {
        this.target = target;
        this.anchorPoints = populateNodes(nodesToAdd);
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

    public AnchorPointBank<T> reflectStateChange(BlockState state) {
        for(AnchorPoint node : anchorPoints)
            node.update(state);
        return this;
    }

    public int size() {
        return anchorPoints.length;
    }

    public AnchorPoint[] getAnchorPoints() {
        return anchorPoints;
    }

    public Pair<AnchorPoint, Double> getClosestAnchor(Vec3 hit) {
        AnchorPoint closestAnchor = null;
        double closestDistance = -1;

        for(AnchorPoint anchor : anchorPoints) {
            Vec3 center = anchor.getPos();
            double distance = Math.abs(hit.distanceTo(center));

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

    public boolean isEmpty() {
        return anchorPoints.length == 0;
    }

    public boolean hasOnlyOneAnchor() {
        return anchorPoints.length == 1;
    }

    public void setIsAwaitingConnection(Level world, boolean isAwaitingConnection) {
        if(world.isClientSide)
            this.isAwaitingConnection = isAwaitingConnection;
    }

    public float tickTime(float pTicks) {
        return tickTime(pTicks, 0.001f);
    }

    public float tickTime(float pTicks, float step) {
        oldTime = time;
        if(time < 1) time += step;
        else time = 0;
        return Mth.lerp(pTicks, oldTime, time);
    }

    public boolean equals(Object other) {
        if(other instanceof AnchorPointBank<?> otherBank) 
            return this.target.getBlockPos().equals(otherBank.target.getBlockPos());
        return false;
    }

    public void sync() {
        for(AnchorPoint anchor : anchorPoints) {
            GridVertex vert = anchor.getParticipant();
            if(vert != null) vert.syncToHostBE();
        }
    }

    public int hashCode() {
        return target.getBlockPos().hashCode();
    }

    public void markDirty() {
        target.getLevel().sendBlockUpdated(
            target.getBlockPos(),
            target.getBlockState(), 
            target.getBlockState(), 
            3);
        target.setChanged();
    }

    public void destroy() {
        if(net == null) return;
        for(AnchorPoint anchor : anchorPoints) {
            GridVertex part = anchor.getParticipant();
            if(part == null) net.findAndDestroyVertex(anchor.getID(), true);
            else net.destroyVertex(part, true);
            anchor.nullifyParticipant();
        }
    }

    public void initialize(Level world) {
        this.net = GlobalTransferGrid.of(world);
    }

    public Level getWorld() {
        return target.getLevel();
    }
}
