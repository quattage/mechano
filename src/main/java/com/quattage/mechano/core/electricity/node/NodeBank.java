package com.quattage.mechano.core.electricity.node;

import java.util.ArrayList;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.electricity.StrictElectricalBlock;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;


/***
 * Manages an Array containing ElectricNodes. <p> The NodeBank is the "master" object
 * used by Electrically-enabled BlockEntities to allow wire attachments and power transfer.
 */
public class NodeBank {

    private final BlockEntity target;
    private final BlockPos pos;
    private final ElectricNode[] NODES;

    /***
     * Creates a new NodeBank from an ArrayList of nodes.
     * This NodeBank will have its ElectricNodes pre-populated.
     * @param nodesToAdd ArrayList of ElectricNodes to populate
     * this NodeBank with.
     */
    public NodeBank(BlockEntity target, ArrayList<ElectricNode> nodesToAdd) {
        this.target = target;
        this.pos = target.getBlockPos();
        this.NODES = populate(nodesToAdd);
    }

    /***
     * Creates a new NodeBank populated with no ElectricNodes. (all nulls) <p>
     * As you can probably imagine, a completely blank Nodebank is 
     * entirely useless. I have no idea if this will ever be used, 
     * but if anyone else happens to be reading this, just use the
     * {@link #NodeBank(ArrayList) standard constructor} instead.
     * @param size Size of this NodeBank
     */
    public NodeBank(int size) {
        this.target = null;
        this.pos = null;
        this.NODES = new ElectricNode[size];
    }

    /***
     * Fills this NodeBank with ElectricNodes from the provided ArrayList
     * @param nodesToAdd
     * @return
     */
    private ElectricNode[] populate(ArrayList<ElectricNode> nodesToAdd) {
        ElectricNode[] out = new ElectricNode[nodesToAdd.size()];
        for(int x = 0; x < out.length; x++) {
            out[x] = nodesToAdd.get(x);
        }
        return out;
    }

    /***
     * Rotates this NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Direction to face
     */
    public NodeBank setOrient(Direction dir) {
        for(ElectricNode node : NODES) {
            node = node.setOrient(dir);
            Mechano.log(node.getId().toUpperCase() + " " + node.getNodeLocation());
        }
        return this;
    }

    /***
     * Rotates this NodeBank to face the given CombinedOrientation <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir CombinedOrientation to face
     */
    public NodeBank setOrient(CombinedOrientation dir) {
        for(ElectricNode node : NODES) {
            node = node.setOrient(dir);
            Mechano.log(node.getId().toUpperCase() + " " + node.getNodeLocation());
        }
        return this;
    }

    /***
     * Retrieves this NodeBank as an array
     * @return The raw aray stored within this NodeBan
     */
    public ElectricNode[] values() {
        return NODES;
    }

    /*** 
     * The length of this NodeBank
     * @return int representing how many ElectricNodes can be held in this NodeBank
     */
    public int length() {
        return NODES.length;
    }

    public Pair<ElectricNode, Double> getClosest(Vec3 hit) {
        return getClosest(hit, 0.6f);
    }

    /***
     * Gets the closest ElectricNode to the provided Vec3. <p>
     * Useful for determining the ELectricNode closest to where the player is looking
     * @param tolerance Any distance higher than this number will be disregarded as "too far away",
     * and will return null.
     * @param hit Vec3 position. Usually this would just be <code>BlockHitResult.getLocation()</code>
     * @return A Pair, where the first member is the ElectricNode itself, and the second member is the
     * distance from this ElectricNode, or null if no node could be found within a reasonable distance.
     */
    @Nullable
    public Pair<ElectricNode, Double> getClosest(Vec3 hit, float tolerance) {
        
        double lastDist = 100;
        Pair<ElectricNode, Double> out = Pair.of(null, null);

        for(int x = 0; x < NODES.length; x++) {
            Vec3 center = NODES[x].getPosition();
            double dist = Math.abs(hit.distanceTo(center));

            if(dist > tolerance) continue;

            if(dist < 0.0001) {
                return Pair.of(NODES[x], Double.valueOf(dist));
            }

            if(dist < lastDist) {
                out.setFirst(NODES[x]);
                out.setSecond(Double.valueOf(dist));
            }

            lastDist = dist;
        }

        if(out.getFirst() == null || out.getSecond() == null) return null;

        return out;
    }

    /***
     * Write this NodeBank to the given CompoundTag
     * @param in CompoundTag to modify with additional data
     * @return the modified CompoundTag
     */
    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        for(int x = 0; x < NODES.length; x++)
            NODES[x].writeTo(out);
        in.putInt("BankX", pos.getX());
        in.putInt("BankY", pos.getY());
        in.putInt("BankZ", pos.getZ());
        in.put("NodeBank", out);
        return in;
    }

    /***
     * Populate this NodeBank with data from a CompoundTag<p>
     * The nodes stored within the given CompoundTag must match the nodes already stored within
     * this NodeBank. If there are issues, errors will be thrown to prevent any mismatching 
     * caused by bad read/writes. <p>
     * <strong>The best way to avoid any issues while using this method is to
     * always ensure the CompoundTag you provide was written by this NodeBank in the first place. <p>
     * (See {@link #writeTo(CompoundTag) writeTo})
     * </strong> 
     * @param in CompoundTag to use
     * @throws IllegalArgumentException if the given CompoundTag doesn't contain a "NodeBank" tag.
     * @throws IllegalArgumentException if the given CompoundTag's NodeBank is incompatable with this NodeBank.
     * @throws IllegalArgumentException if this NodeBank has any ElectricNodes that aren't referenced in the
     * given CompoundTag.
     */
    public void readFrom(CompoundTag in) {
        if(!in.contains("NodeBank")) throw new IllegalArgumentException("CompoundTag [[" + in + "]] contains no relevent data!");
        CompoundTag bank = in.getCompound("NodeBank");

        if(bank.size() != NODES.length) throw new IllegalArgumentException("Provided CompoundTag's NodeBank contains " 
            + bank.size() + " nodes, but this NodeBank can only store" + NODES.length);

        for(int x = 0; x < NODES.length; x++) {
            String thisID = NODES[x].getId();

            // TODO potentially excessive throw, may replace with continue
            if(!bank.contains(thisID)) throw new IllegalArgumentException("This NodeBank instance contains an ElectricNode called '" 
                + thisID + "', but the provided NodeBank NBT data does not!");

            NODES[x] = new ElectricNode(posFromTag(in), thisID, bank.getCompound(NODES[x].getId()));

            if(target.getBlockState().getBlock() instanceof StrictElectricalBlock eBlock) {
                NODES[x].setOrient(target.getBlockState().getValue(StrictElectricalBlock.ORIENTATION));
            }
        }
    }

    private BlockPos posFromTag(CompoundTag in) {
        return new BlockPos(in.getInt("BankX"), in.getInt("BankY"), in.getInt("BankZ"));
    }

    public String toString() {
        String out = "NodeBank bound to " + target.getClass().getSimpleName() + " at " + target.getBlockPos() + ":\n";
        for(int x = 0; x < NODES.length; x++) 
            out += "Node " + x + ": " + NODES[x] + "\n";
        return out;
    }
}
