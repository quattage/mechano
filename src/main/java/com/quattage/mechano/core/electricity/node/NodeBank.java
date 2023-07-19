package com.quattage.mechano.core.electricity.node;

import java.util.ArrayList;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.StrictElectricalBlock;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.electricity.node.connection.ElectricNodeConnection;
import com.quattage.mechano.core.electricity.node.connection.FakeNodeConnection;
import com.quattage.mechano.core.electricity.node.connection.NodeConnection;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;


/***
 * A NodeBank addresses, modifies, and serializes a pre-defined list of ElectricNodes.
 * A NodeBank instance can be stored in any electrically-enabled BlockEntity to provide
 * all related functionality, such as making connections and transfering power.
 */
public class NodeBank {

    /**
     * The Version String is serialized directly to NBT
     * just in case any of this NBT stuff changes between
     * releases. It can be changed later to invalidate old 
     * blocks and avoid breaking worlds
     */
    private final String VERSION = "0";
    private final ElectricNode[] NODES;

    public final BlockEntity target;
    public final BlockPos pos;

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
        for(ElectricNode node : NODES) 
            node = node.setOrient(dir);
        return this;
    }

    /***
     * Rotates this NodeBank to face the given CombinedOrientation <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir CombinedOrientation to face
     */
    public NodeBank setOrient(CombinedOrientation dir) {
        for(ElectricNode node : NODES)
            node = node.setOrient(dir);
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

    public int forceFindIndex(ElectricNode node) {
        if(node == null || NODES.length == 0 )  return -1;
        for(int x = 0; x < NODES.length; x++)
            if(NODES[x] == node) return x;
        return -1;
    }

    /***
     * Write this NodeBank to the given CompoundTag
     * @param in CompoundTag to modify with additional data
     * @return the modified CompoundTag
     */
    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        for(int x = 0; x < NODES.length; x++) {
            //Mechano.log(x + ": " + NODES[x].getId());
            NODES[x].writeTo(out);
        }
        in.putString("BankVersion", VERSION);
        in.put("NodeBank", out);
        return in;
    }

    /***
     * Returns true if there is a connection in this NodeBank
     * that needs to be updated every tick.
     * @return
     */
    public boolean shouldAlwaysRender() {
        for(ElectricNode n : NODES) 
            if(n.shouldAlwaysRender())return true;
        return false;
    }

    /***
     * Populate this NodeBank with data from a CompoundTag<p>
     * The nodes stored within the given CompoundTag must match the nodes already stored within
     * this NodeBank. If there are issues, errors will be thrown to prevent any mismatching 
     * caused by bad read/writes. <p>
     * <strong>The best way to avoid any issues while using this method is to
     * always ensure the CompoundTag you provide was written by this NodeBank in the first place. <p>
     * (See {@link #writeTo(CompoundTag) writeTo})
     * </strong> Bank
     * @param in CompoundTag to use
     * @throws IllegalArgumentException if the given CompoundTag doesn't contain a "NodeBank" tag.
     * @throws IllegalArgumentException if the given CompoundTag's NodeBank is incompatable with this NodeBank.
     * @throws IllegalArgumentException if this NodeBank has any ElectricNodes that aren't referenced in the
     * given CompoundTag.
     */
    public void readFrom(CompoundTag in) {
        String v = in.getString("BankVersion");
        if(v == null || !v.equals(VERSION)) return;

        if(!in.contains("NodeBank")) throw new IllegalArgumentException("CompoundTag [[" + in + "]] contains no relevent data!");
        CompoundTag bank = in.getCompound("NodeBank");

        if(bank.size() != NODES.length) throw new IllegalArgumentException("Provided CompoundTag's NodeBank contains " 
            + bank.size() + " nodes, but this NodeBank must store " + NODES.length + " nodes!");

        for(int x = 0; x < NODES.length; x++) {
            String thisID = NODES[x].getId();

            // TODO potentially excessive throw, may replace with continue
            if(!bank.contains(thisID)) throw new IllegalArgumentException("This NodeBank instance contains an ElectricNode called '" 
                + thisID + "', but the provided NodeBank NBT data does not!");

            NODES[x] = new ElectricNode(target, bank.getCompound(thisID));
            

            if(target.getBlockState().getBlock() instanceof StrictElectricalBlock eBlock)
                NODES[x].setOrient(target.getBlockState().getValue(StrictElectricalBlock.ORIENTATION));
        }
    }

    public boolean contains(String id) {
        for(int x = 0; x < NODES.length; x++) 
            if(NODES[x].getId().equals(id)) return true;
        return false;
    }

    @Nullable
    public ElectricNode get(String id) {
        for(int x = 0; x < NODES.length; x++) 
            if(NODES[x].getId().equals(id)) return NODES[x];
        return null;
    }

    public int indexOf(String id) {
        for(int x = 0; x < NODES.length; x++) 
            if(NODES[x].getId().equals(id)) return x;
        return -1;
    }

    public String toString() {
        String out = "NodeBank bound to " + target.getClass().getSimpleName() + " at " + target.getBlockPos() + ":\n";
        for(int x = 0; x < NODES.length; x++) 
            out += "Node " + x + ": " + NODES[x] + "\n";
        return out;
    }

    public void markDirty() {
        target.getLevel().sendBlockUpdated(pos,
            target.getBlockState(), 
            target.getBlockState(), 
            3);
        target.setChanged();
    }

    public void initConnections() {
        for(int x = 0; x < NODES.length; x++) {
            NODES[x].initConnections(target);
        }
    }

    /***
     * Adds a FakeNodeConnection at the given fromID in this NodeBank.
     * Fake Connections hold a connection between a source BlockEntity and a
     * destination Entity, rather than to BlockEntity positions. 
     * This means that they can be updated in real-time, for things like 
     * attaching a wire to a player.
     */
    public FakeNodeConnection makeFakeConnection(WireSpool spoolType, String fromID, Entity targetEntity) {
        Vec3 sourcePos = get(fromID).getPosition();
        FakeNodeConnection fakeConnection = new FakeNodeConnection(spoolType, fromID, sourcePos, targetEntity);
        Mechano.log("Fake connection established: " + fakeConnection + ", to Entity " + targetEntity);

        NODES[indexOf(fromID)].addConnection(fakeConnection);
        return fakeConnection;
    }

    /***
     * Establish an ElectricNodeConnection between two ElectricNodes, where the
     * input is the FakeNodeConnection to "upgrade" into an ElectricNodeConnection
     */
    public void makeFullConnection(FakeNodeConnection fake, NodeBank targetBank, String targetID) {
        NODES[indexOf(fake.getSourceID())].nullifyLastConnection();
        connect(fake.getSpoolType(), fake.getSourceID(), targetBank, targetID);
    }

    /***
     * Establish a NodeConnection between one ElectricNode in this NodeBank,
     * and one ElectricNode in another bank. <p>
     * A connection between two ElectricNodes is made of two NodeConnections, where both 
     * ends store mirrored copies of the connection. <p> In practice, this means that the
     * ElectricNode that the player selects first, <strong>(the "from" node)</strong>, recieves
     * a normal NodeConnection, and the ElectricNode that the player selects second, 
     * <strong>(the "target" node)</strong>, receives an inverted copy of the same NodeConnection.
     * 
     * @param spoolType Type of connection to create - Determines transfer rate, wire model, etc.
     * @param targetBank The other NodeBank, where the destination ElectricNode is.
     * @param targetID The name of the destinationElectricNode in the targetBank.
     */
    public void connect(WireSpool spoolType, String fromID, NodeBank targetBank, String targetID) {
        Vec3 sourcePos = get(fromID).getPosition();
        Vec3 destPos = targetBank.get(targetID).getPosition();

        NodeConnection fromConnection = new ElectricNodeConnection(spoolType, this, sourcePos, targetBank, targetID);
        NodeConnection targetConnection = new ElectricNodeConnection(spoolType, targetBank, destPos, this, fromID);
        Mechano.log("Connection established from: " + fromConnection + "  to: \n" + targetConnection);

        NODES[indexOf(fromID)].addConnection(fromConnection);
        targetBank.NODES[targetBank.indexOf(targetID)].addConnection(targetConnection);

        markDirty(); targetBank.markDirty();
    }

    /***
     * Get the NodeBank at the given location.
     * @param world World to operate within
     * @param pos BlockPos of the target BlockEntity
     * @return The NodeBank at the given BlockPos, or null if none exists.
     */
    @Nullable
    public static NodeBank retrieve(Level world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if(be == null) return null;
        if(be instanceof ElectricBlockEntity ebe)
            return ebe.nodes;
        return null;
    }

    /***
     * Gets the NodeBank at the location determined by the given offset
     * @param world World to operate within
     * @param root The root ElectricBlockEntity, the one getting the NodeBank
     * @param relativePos Relative position of the target NodeBank, usually acquired
     * from a NodeConnection
     * @return The NodeBank at the surmised BlockPos, or null if one does not exist.
     */
    @Nullable
    public static NodeBank retrieveFrom(Level world, BlockEntity root, Vec3i relativePos) {
        return retrieve(world, root.getBlockPos().subtract(relativePos));
    }
}
