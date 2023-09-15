package com.quattage.mechano.foundation.electricity;

import java.util.ArrayList;
import java.util.HashSet;

import javax.annotation.Nullable;

import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.foundation.effect.ParticleBuilder;
import com.quattage.mechano.foundation.effect.ParticleSpawner;
import com.quattage.mechano.foundation.electricity.core.connection.ElectricNodeConnection;
import com.quattage.mechano.foundation.electricity.core.connection.FakeNodeConnection;
import com.quattage.mechano.foundation.electricity.core.connection.NodeConnectResult;
import com.quattage.mechano.foundation.electricity.core.connection.NodeConnection;
import com.quattage.mechano.foundation.electricity.core.node.ElectricNode;
import com.quattage.mechano.foundation.electricity.system.SystemVertex;
import com.quattage.mechano.foundation.helper.VectorHelper;
import com.simibubi.create.foundation.utility.Pair;

import static com.quattage.mechano.foundation.electricity.system.GlobalTransferNetwork.NETWORK;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Triplet;


/***
 * A NodeBank addresses, modifies, and serializes a pre-defined list of ElectricNodes.
 * A NodeBank instance can be stored in any electrically-enabled BlockEntity to provide
 * all related functionality, such as making connections and transfering power.
 */
public class NodeBank<T extends ElectricBlockEntity> {

    /**
     * The Version String is serialized directly to NBT
     * just in case any of this NBT stuff changes between
     * releases. It can be changed later to invalidate old 
     * blocks and avoid breaking worlds
     */
    public static final float HITFAC = 4f;
    private static final String VERSION = "0";

    private final ElectricNode[] allNodes;
    public final T target;
    public final BlockPos pos;

    /***
     * Creates a new NodeBank from an ArrayList of nodes.
     * This NodeBank will have its ElectricNodes pre-populated.
     */
    public NodeBank(T target, ArrayList<ElectricNode> nodesToAdd, HashSet<RelativeDirection> dirsToAdd, 
        int capacity, int maxRecieve, int maxExtract, int energy) {
        this.target = target;
        this.pos = target.getBlockPos();
        this.allNodes = populateNodes(nodesToAdd);
    }

    private ElectricNode[] populateNodes(ArrayList<ElectricNode> nodesToAdd) {
        if(nodesToAdd == null) 
            throw new NullPointerException("Cannot instantiate new NodeBank instance - nodesToAdd is null!");
        if(nodesToAdd.isEmpty()) 
            throw new IllegalArgumentException("Cannot instantiate new NodeBank instance - nodesToAdd is empty!");

        ElectricNode[] out = new ElectricNode[nodesToAdd.size()];
        for(int x = 0; x < out.length; x++) {
            out[x] = nodesToAdd.get(x);
        }
        return out;
    }

    public Level getWorld() {
        return target.getLevel();
    }

    /***
     * Compares NodeBanks. At the moment, this only compares based on
     * location.
     * @param other NodeBank to compare. 
     * @return True if both NodeBanks share the same target BlockPos.
     */
    public boolean equals(NodeBank<?> other) {
        return other == null ? false : pos.equals(other.pos);
    }

    /***
     * Hashes this NodeBank based on its target BlockEntity's location.
     */
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
        return this.pos.equals(posToCheck);
    }

    public NodeBank<T> reflectStateChange(BlockState state) {
        if(state == null) return this;
        CombinedOrientation target = DirectionTransformer.extract(state);
        //Mechano.log("New orientation:" + target);
        for(ElectricNode node : allNodes)
            node = node.rotateToFace(target);
        return this;
    }

    /*** 
     * The length of this NodeBank
     * @return int representing how many ElectricNodes can be held in this NodeBank
     */
    public int length() {
        return allNodes.length;
    }

    public Pair<ElectricNode[], Integer> getAllNodes(Vec3 hit) {
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
    public Pair<ElectricNode[], Integer> getAllNodes(Vec3 hit, float tolerance) {

        Pair<ElectricNode[], Integer> out = Pair.of(values(), -1);
        if(length() == 1) return Pair.of(values(), 0);

        double lastDist = 256;

        for(int x = 0; x < allNodes.length; x++) {
            Vec3 center = allNodes[x].getPosition();
            double dist = Math.abs(hit.distanceTo(center));
            
            if(tolerance == 0) tolerance = allNodes[x].getHitSize() * 5f;
            if(dist > tolerance) continue;

            if(dist < lastDist) {
                out.setSecond(x);
            }

            lastDist = dist;
        }

        return out;
    }

    /***
     * Continually searches in the area surrounding the player's look direction for 
     * NodeBanks to pull nodes from. This is a key step required to provide the functionality 
     * related to targeting ElectricNodes to make connections without having to be looking at 
     * the block's VoxelShape directly. This is Intended to be used internally, but may have 
     * some utility elsewhere.<p>
     * 
     * It searches in a straight line outward from the given start to the given end position,
     * and also indexes through the surrounding blocks in that line in a sort of "cone" shape. 
     * It's pretty computationally expensive. Use with caution.
     * 
     * @param world World to operate within
     * @param start Vec3 starting position of the search (camera posiiton)
     * @param end Vec3 ending position of the search (BlockHitResult)
     * @param scope (Optional, default 5) Width (in blocks) of the "cone" that is searched.
     * Must be an odd number >= 3.
     * @return An ArrayList of pairs, where the first member is the NodeBank itself, and the
     * second member is the point along the ray that is closest to the NodeBank.
     */
    public static ArrayList<Pair<NodeBank<?>, Vec3>> findBanksAlongRay(Level world, Vec3 start, Vec3 end) {
        return findBanksAlongRay(world, start, end, 5);
    }

    /***
     * Continually searches in the area surrounding the player's look direction for 
     * NodeBanks to pull nodes from. This is a key step required to provide the functionality 
     * related to targeting ElectricNodes to make connections without having to be looking at 
     * the block's VoxelShape directly. This is Intended to be used internally, but may have 
     * some utility elsewhere.<p>
     * 
     * It searches in a straight line outward from the given start to the given end position,
     * and also indexes through the surrounding blocks in that line in a sort of "cone" shape. 
     * It's pretty computationally expensive. Use with caution.
     * 
     * @param world World to operate within
     * @param start Vec3 starting position of the search (camera posiiton)
     * @param end Vec3 ending position of the search (BlockHitResult)
     * @return An ArrayList of pairs, where the first member is the NodeBank itself, and the
     * second member is the point along the ray that is closest to the NodeBank.

     */
    public static ArrayList<Pair<NodeBank<?>, Vec3>> findBanksAlongRay(Level world, Vec3 start, Vec3 end, int scope) {
        ArrayList<Pair<NodeBank<?>, Vec3>> out =  new ArrayList<Pair<NodeBank<?>, Vec3>>();
        int maxIterations =  (int)((NodeBank.HITFAC * 0.43) * start.distanceTo(end));

        if(scope % 2 == 0) scope += 1;
        if(scope < 3) scope = 3;

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
                                Pair<NodeBank<?>, Vec3> lookup = out.get(search);
                                if(lookup.getFirst().equals(ebe.nodeBank)) { // if the arraylist already contains this NodeBank
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
     * 1: List of all ElectricNodes that were deemed "close enough",
     * 2: Integer index of the closest ElectricNode in the aforementioned list,
     * and 3: the NodeBank that the closest node belongs to.
     */
    public static Triplet<ArrayList<ElectricNode>, Integer, NodeBank<?>> findClosestNodeAlongRay(Level world, Vec3 start, Vec3 end, float tolerance) {

        int closestIndex = -1;
        NodeBank<?> closestBank = null;
        double lastDist = 256;
        ArrayList<ElectricNode> allNearbyNodes = new ArrayList<ElectricNode>();

        for(Pair<NodeBank<?>, Vec3> potential : findBanksAlongRay(world, start, end)) {
            for(ElectricNode node : potential.getFirst().allNodes) {
                allNearbyNodes.add(node);
                Vec3 center = node.getPosition();
                double dist = Math.abs(potential.getSecond().distanceTo(center));

                if(tolerance == 0) tolerance = node.getHitSize() * 5f;
                if(dist > tolerance) continue;

                if(dist < lastDist) {
                    closestIndex = allNearbyNodes.size() - 1;
                    closestBank = potential.getFirst();
                }

            lastDist = dist;
            }
        }

        return new Triplet<ArrayList<ElectricNode>, Integer, NodeBank<?>>(allNearbyNodes, closestIndex, closestBank);
    }

    @Nullable
    public Pair<ElectricNode, Double> getClosestNode(Vec3 hit) {
        Pair<ElectricNode[], Integer> out = getAllNodes(hit);
        if(out.getSecond() == -1) return null;
        ElectricNode closest = out.getFirst()[out.getSecond()];
        return Pair.of(closest, closest.getPosition().distanceTo(hit));
    }

    /***
     * Retrieves this NodeBank as an array
     * @return The raw aray stored within this NodeBan
     */
    public ElectricNode[] values() {
        return allNodes;
    }

    /***
     * Write this NodeBank to the given CompoundTag
     * @param in CompoundTag to modify with additional data
     * @return the modified CompoundTag
     */
    public CompoundTag writeTo(CompoundTag in) {
        CompoundTag out = new CompoundTag();
        for(int x = 0; x < allNodes.length; x++) {
            allNodes[x].writeTo(out);
        }
        in.putString("ver", VERSION);
        in.put("bank", out);
        
        return in;
    }

    /***
     * Returns true if there is a connection in this NodeBank
     * that needs to be updated every tick.
     * @return
     */
    public boolean shouldAlwaysRender() {
        for(ElectricNode n : allNodes) 
            if(n.shouldAlwaysRender()) return true;
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
     * @throws IllegalStateException if the given CompoundTag's NodeBank is incompatable with this NodeBank.
     * @throws IllegalStateException if this NodeBank has any ElectricNodes that aren't referenced in the
     * given CompoundTag.
     */
    public void readFrom(CompoundTag in) {
        String v = in.getString("ver");
        if(v == null || !v.equals(VERSION)) return;

        if(!in.contains("bank")) throw new IllegalArgumentException("CompoundTag [[" + in + "]] contains no relevent data!");
        CompoundTag bank = in.getCompound("bank");

        if(bank.size() != allNodes.length) throw new IllegalStateException("Provided CompoundTag's NodeBank contains " 
            + bank.size() + " nodes, but this NodeBank must store " + allNodes.length + " nodes!");

        for(int x = 0; x < allNodes.length; x++) 
            allNodes[x] = new ElectricNode(target, bank.getCompound(x + ""));
    }

    @Nullable
    public ElectricNode get(int index) {
        return allNodes[index];
    }

    public int indexOf(ElectricNode node) {
        for(int x = 0; x < allNodes.length; x++)
            if(allNodes[x].equals(node)) return x;
        return -1;
    }

    public String toString() {
        String out = "\nNodeBank { \n\tTarget: " + target.getClass().getSimpleName() + " \n\tLocation: " + target.getBlockPos() + ":\n";
        for(int x = 0; x < allNodes.length; x++) 
            out += x + ": " + allNodes[x] + "\n";
        return out + "}\n\n";
    }

    /***
     * Instructs the parent BlockEntity to send the block update.
     */
    public void markDirty() {
        target.getLevel().sendBlockUpdated(pos,
            target.getBlockState(), 
            target.getBlockState(), 
            3);
        target.setChanged();
    }

    public void refreshTargetOrient() {
            target.reOrient();
    }

    /***
     * Removes all connections that involve both the given NodeBank and this NodeBank
     * @param from NodeBank to use for comparison - All connections that exist to the
     * provided NodeBank will be removed from this NodeBank.
     * 
     */
    public boolean removeSharedConnections(NodeBank<?> origin) {
        boolean changed = false;
        for(ElectricNode node : allNodes) {
            if(node.removeConnectionsInvolving(origin))
                changed = true;
        }
        if(changed)
            markDirty();
        return changed;
    }

    public boolean hasConnections() {
        for(ElectricNode node : allNodes)
            if(node.connections.length > 0) return true;
        return false;
    }

    /***
     * Returns a HashSet containing every NodeBank targeted by connections
     * stored within this NodeBank.
     * @return
     */
    public HashSet<NodeBank<?>> getAllTargetBanks() {
        HashSet<NodeBank<?>> out = new HashSet<NodeBank<?>>();
        for(ElectricNode node : allNodes) {
            out.addAll(node.getAllTargetBanks(this));
        }
        return out;
    }

    /***
     * Safely invalidates all connections made to this NodeBank. Usually called
     * when this NodeBank's parent Block is destroyed.
     */
    public void destroy() {
        for(NodeBank<?> bank : getAllTargetBanks()) {
            for(int x = 0; x < bank.allNodes.length; x++)
                NETWORK.unlink(new SystemVertex(this.target.getBlockPos()), new SystemVertex(bank.target.getBlockPos()));
            bank.removeSharedConnections(this);
        }
    }

    /***
     * Initializes the connections stored in this NodeBank. For now, all this does is
     * set the NodeConnection's destination positions in every ElectricNode in this bank. <p>
     * It does this because the actual vectors for the destination positions cannot be
     * obtained while the level is loading, beacuse the level doesn't yet exist in a callable
     * form.
     */
    public void init() {
        for(ElectricNode node : allNodes)
            node.initConnections(target);
        markDirty();
    }

    /***
     * Removes the last connection made to this NodeBank. Used primarily to cancel
     * a connection, where the latest connection made to this NodeBank is a FakeNodeConnection.
     * @param sourceID ElectricNode to remove the last connection from.
     */
    public void cancelConnection(int sourceID) {
        ElectricNode node = get(sourceID);
        if(node != null) node.nullifyLastConnection();
        markDirty();
    }

    /***
     * Adds a FakeNodeConnection at the given fromID in this NodeBank.
     * Fake Connections hold a connection between a source BlockEntity and a
     * destination Entity, rather than two BlockEntity positions. 
     * This means that they can be updated in real-time, for things like 
     * attaching a wire to a player.
     */
    public Pair<NodeConnectResult, FakeNodeConnection> makeFakeConnection(WireSpool spoolType, int fromIndex, Entity targetEntity) {

        ElectricNode nodeToConnect = get(fromIndex);
        Vec3 sourcePos = nodeToConnect.getPosition();
        FakeNodeConnection fakeConnection = new FakeNodeConnection(spoolType, fromIndex, sourcePos, targetEntity, this.target.getBlockPos());
        NodeConnectResult result = allNodes[fromIndex].addConnection(fakeConnection);

        return Pair.of(result, fakeConnection);
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
     * @param fake Fake Connection, usually from a BlockEntity to a player, to make into a real connection.
     * @param spoolType Type of connection to create - Determines transfer rate, wire model, etc.
     * @param targetBank The other NodeBank, where the destination ElectricNode is.
     * @param targetID The name of the destinationElectricNode in the targetBank.
     */
    public NodeConnectResult connect(FakeNodeConnection fake, NodeBank<?> targetBank, int targetID) {
        Vec3 sourcePos = fake.getSourcePos();
        Vec3 destPos = targetBank.get(targetID).getPosition();
        WireSpool spoolType = fake.getSpoolType();

        NodeConnection fromConnection = new ElectricNodeConnection(spoolType, this, sourcePos, targetBank, targetID);
        NodeConnection targetConnection = new ElectricNodeConnection(spoolType, targetBank, destPos, this, fake.getSourceID(), true);

        if(targetBank.equals(this))
            return NodeConnectResult.LINK_CONFLICT;

        NodeConnectResult r1 = targetBank.allNodes[targetID].addConnection(targetConnection);
        
        if(r1.isSuccessful()) {

            if(target.getLevel() instanceof ServerLevel sWorld) {

                ParticleSpawner particle = ParticleBuilder.ofType(ParticleTypes.END_ROD)
                    .at(fromConnection.getSourcePos())
                    .randomness(0.1f)
                    .density(3)
                    .build();

                particle.spawnAsServer(sWorld);
                particle.setPos(fromConnection.getDestPos());
                particle.spawnAsServer(sWorld);
            }

            allNodes[fake.getSourceID()].replaceLastConnection(fromConnection);
            markDirty(); targetBank.markDirty();

            NETWORK.link(new SystemVertex(fromConnection.getParentPos(), fake.getSourceID()), new SystemVertex(targetConnection.getParentPos(), targetID));
            return NodeConnectResult.WIRE_SUCCESS;
        }        
        return r1;
    }

    public SystemVertex approximate() {
        return new SystemVertex(this);
    }

    /***
     * Get the NodeBank at the given location.
     * @param world World to operate within
     * @param pos BlockPos of the target BlockEntity
     * @return The NodeBank at the given BlockPos, or null if none exists.
     */
    @Nullable
    public static <T> NodeBank<?> retrieve(Level world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if(be == null) return null;
        if(be instanceof WireNodeBlockEntity ebe)
            return ebe.nodeBank;
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
    public static <T> NodeBank<?> retrieveAtRelative(Level world, BlockEntity root, Vec3i relativePos) {
        return retrieve(world, root.getBlockPos().subtract(relativePos));
    }
}
