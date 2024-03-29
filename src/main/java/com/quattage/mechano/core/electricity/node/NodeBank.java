package com.quattage.mechano.core.electricity.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.item.spool.WireSpool;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.quattage.mechano.core.block.orientation.SimpleOrientation;
import com.quattage.mechano.core.block.orientation.VerticalOrientation;
import com.quattage.mechano.core.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.core.electricity.DirectionalEnergyStorable;
import com.quattage.mechano.core.electricity.LocalEnergyStorage;
import com.quattage.mechano.core.electricity.blockEntity.ElectricBlockEntity;
import com.quattage.mechano.core.electricity.network.EnergySyncS2CPacket;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.electricity.node.connection.ElectricNodeConnection;
import com.quattage.mechano.core.electricity.node.connection.FakeNodeConnection;
import com.quattage.mechano.core.electricity.node.connection.NodeConnectResult;
import com.quattage.mechano.core.electricity.node.connection.NodeConnection;
import com.quattage.mechano.core.util.VectorHelper;
import com.quattage.mechano.network.MechanoPackets;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import oshi.util.tuples.Triplet;


/***
 * A NodeBank addresses, modifies, and serializes a pre-defined list of ElectricNodes.
 * A NodeBank instance can be stored in any electrically-enabled BlockEntity to provide
 * all related functionality, such as making connections and transfering power.
 */
public class NodeBank implements DirectionalEnergyStorable {

    /**
     * The Version String is serialized directly to NBT
     * just in case any of this NBT stuff changes between
     * releases. It can be changed later to invalidate old 
     * blocks and avoid breaking worlds
     */
    private static final String VERSION = "0";
    private final ElectricNode[] allNodes;
    private final Optional<RelativeDirection[]> allInteractions;

    public static final float HITFAC = 4f;

    public final BlockEntity target;
    public final BlockPos pos;

    private final LocalEnergyStorage<NodeBank> energyStorage;
    private LazyOptional<IEnergyStorage> energyHandler = LazyOptional.empty();

    /***
     * Creates a new NodeBank from an ArrayList of nodes.
     * This NodeBank will have its ElectricNodes pre-populated.
     */
    public NodeBank(BlockEntity target, ArrayList<ElectricNode> nodesToAdd, HashSet<RelativeDirection> dirsToAdd, 
        int capacity, int maxRecieve, int maxExtract, int energy) {
        this.target = target;
        this.pos = target.getBlockPos();
        this.allNodes = populateNodes(nodesToAdd);
        this.allInteractions = populateDirs(dirsToAdd);
        this.energyStorage = new LocalEnergyStorage<NodeBank>
            (this, capacity, maxRecieve, maxExtract, energy);
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

    private Optional<RelativeDirection[]> populateDirs(HashSet<RelativeDirection> dirsToAdd) {
        if(dirsToAdd == null) return Optional.empty();
        if(dirsToAdd.isEmpty()) return Optional.of(new RelativeDirection[0]);

        int x = 0;
        RelativeDirection[] out = new RelativeDirection[dirsToAdd.size()];
        for(RelativeDirection dir : dirsToAdd) {
            out[x] = dir; 
            x++;
        }
        return Optional.of(out);
    }

    public Level getWorld() {
        return target.getLevel();
    }

    /***
     * Gets every direction that this NodeBank can interact with.
     * Directions are relative to the world, and will change
     * depending on the orientation of this NodeBank's parent block.
     * @return A list of Directions; empty if this NodeBank
     * has no interaction directions.
     */
    public Direction[] getInteractionDirections() {
        if(!allInteractions.isPresent()) return new Direction[0];
        if(allInteractions.get().length == 0) return Direction.values();
        
        RelativeDirection[] rels = allInteractions.get();
        Direction[] out = new Direction[rels.length];

        for(int x = 0; x < rels.length; x++) {
            out[x] = rels[x].get();
        }
        return out;
    }

    /***
     * The "raw" version of {@link #getInteractionDirections()} Directly returns
     * RelativeDirections as an Optional. Pretty much exclusively used for debugging.
     * @return Optional list of RelativeDirections. Empty optional indicates all directions,
     * empty list indicates no directions.
     */
    public Optional<RelativeDirection[]> getRelativeDirs() {
        return allInteractions;
    }

    /***
     * Compares NodeBanks. At the moment, this only compares based on
     * location.
     * @param other NodeBank to compare. 
     * @return True if both NodeBanks share the same target BlockPos.
     */
    public boolean equals(NodeBank other) {
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
     * @param otherPos Blockpos to check
     * @return True if this NodeBank's target BlockEntity is located
     * at the given BlockPos
     */
    public boolean isAt(BlockPos otherPos) {
        if(otherPos == null) return false;
        return this.pos.equals(otherPos);
    }

    /***
     * Rotates this NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     */
    public NodeBank rotate(Direction dir) {
        rotate(CombinedOrientation.convert(dir));
        return this;
    }

    /***
     * Rotates this NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     */
    public NodeBank rotate(SimpleOrientation dir) {
        rotate(CombinedOrientation.convert(dir));
        return this;
    }

    /***
     * Rotates this NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     */
    public NodeBank rotate(VerticalOrientation dir) {
        rotate(CombinedOrientation.convert(dir));
        return this;
    }

    /***
     * Rotates this NodeBank to face the given Direction <p>
     * More specifically, it loops through every stored ElectricNode
     * and modifies its NodeLocation based on the given direction.
     * @param dir Acceptable overloads: Direction, CombinedOrientation, 
     * SimpleOrientation, or VerticalOrientation to use as a basis for
     * rotation.
     */
    public NodeBank rotate(CombinedOrientation dir) {
        for(ElectricNode node : allNodes)
            node = node.rotateNode(dir);

        if(!allInteractions.isPresent() ||
            allInteractions.get().length == 0) return this;

        for(RelativeDirection rel : allInteractions.get())
            rel = rel.rotate(dir);
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
     * It searches in a straight line outward from the player's camera, so it's pretty
     * computationally expensive. Use with caution.
     * 
     * @param world World to operate within
     * @param start Vec3 starting position of the search (camera posiiton)
     * @param end Vec3 ending position of the search (BlockHitResult)
     * @param scope (Optional, default 5) Width (in blocks) of the "cone" that is searched.
     * Must be an odd number >= 3.
     * @return An ArrayList of pairs, where the first member is the NodeBank itself, and the
     * second member is the point along the ray that is closest to the NodeBank.
     */
    public static ArrayList<Pair<NodeBank, Vec3>> findBanksAlongRay(Level world, Vec3 start, Vec3 end) {
        return findBanksAlongRay(world, start, end, 5);
    }

    /***
     * Continually searches in the area surrounding the player's look direction for 
     * NodeBanks to pull nodes from. This is a key step required to provide the functionality 
     * related to targeting ElectricNodes to make connections without having to be looking at 
     * the block's VoxelShape directly. This is Intended to be used internally, but may have 
     * some utility elsewhere.<p>
     * 
     * It searches in a straight line outward from the player's camera, so it's pretty
     * computationally expensive. Use with caution.
     * 
     * @param world World to operate within
     * @param start Vec3 starting position of the search (camera posiiton)
     * @param end Vec3 ending position of the search (BlockHitResult)
     * @return An ArrayList of pairs, where the first member is the NodeBank itself, and the
     * second member is the point along the ray that is closest to the NodeBank.

     */
    public static ArrayList<Pair<NodeBank, Vec3>> findBanksAlongRay(Level world, Vec3 start, Vec3 end, int scope) {
        ArrayList<Pair<NodeBank, Vec3>> out =  new ArrayList<Pair<NodeBank, Vec3>>();
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

                        if(world.getBlockEntity(origin.offset(boxOffset)) instanceof ElectricBlockEntity ebe) {

                            if(ebe.nodeBank == null) continue;
                            if(out.size() == 0) {
                                out.add(Pair.of(ebe.nodeBank, lookStep));
                                continue;
                            }

                            boolean alreadyExists = false;
                            for(int search = 0; search < out.size(); search++) {
                                Pair<NodeBank, Vec3> lookup = out.get(search);
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
    public static Triplet<ArrayList<ElectricNode>, Integer, NodeBank> findClosestNodeAlongRay(Level world, Vec3 start, Vec3 end, float tolerance) {

        int closestIndex = -1;
        NodeBank closestBank = null;
        double lastDist = 256;
        ArrayList<ElectricNode> allNearbyNodes = new ArrayList<ElectricNode>();

        for(Pair<NodeBank, Vec3> potential : findBanksAlongRay(world, start, end)) {
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

        return new Triplet<ArrayList<ElectricNode>, Integer, NodeBank>(allNearbyNodes, closestIndex, closestBank);
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

    public int forceFindIndex(ElectricNode node) {
        if(node == null || allNodes.length == 0 )  return -1;
        for(int x = 0; x < allNodes.length; x++)
            if(allNodes[x] == node) return x;
        return -1;
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
        energyStorage.writeTo(in);
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
        energyStorage.readFrom(in);
        String v = in.getString("ver");
        if(v == null || !v.equals(VERSION)) return;

        if(!in.contains("bank")) throw new IllegalArgumentException("CompoundTag [[" + in + "]] contains no relevent data!");
        CompoundTag bank = in.getCompound("bank");

        if(bank.size() != allNodes.length) throw new IllegalStateException("Provided CompoundTag's NodeBank contains " 
            + bank.size() + " nodes, but this NodeBank must store " + allNodes.length + " nodes!");

        for(int x = 0; x < allNodes.length; x++) {
            String thisID = allNodes[x].getId();

            // TODO potentially excessive throw, may replace with continue
            if(!bank.contains(thisID)) throw new IllegalStateException("This NodeBank instance contains an ElectricNode called '" 
                + thisID + "', but the provided NodeBank NBT data does not!");

            allNodes[x] = new ElectricNode(target, bank.getCompound(thisID));
        }
    }

    public boolean contains(String id) {
        for(int x = 0; x < allNodes.length; x++) 
            if(allNodes[x].getId().equals(id)) return true;
        return false;
    }

    @Nullable
    public ElectricNode get(String id) {
        for(int x = 0; x < allNodes.length; x++) 
            if(allNodes[x].getId().equals(id)) return allNodes[x];
        return null;
    }

    public int indexOf(String id) {
        for(int x = 0; x < allNodes.length; x++) 
            if(allNodes[x].getId().equals(id)) return x;
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
        if(target instanceof ElectricBlockEntity ebe)
            ebe.refreshOrient();
    }

    /***
     * Removes all connections that involve both the given NodeBank and this NodeBank
     * @param from NodeBank to use for comparison - All connections that exist to the
     * provided NodeBank will be removed from this NodeBank.
     * 
     */
    public boolean removeSharedConnections(NodeBank origin) {
        boolean changed = false;
        for(ElectricNode node : allNodes) {
            if(node.removeConnectionsInvolving(origin))
                changed = true;
        }
        if(changed) markDirty();
        return changed;
    }

    /***
     * Returns a HashSet containing every NodeBank targeted by connections
     * stored within this NodeBank.
     * @return
     */
    public HashSet<NodeBank> getAllTargetBanks() {
        HashSet<NodeBank> out = new HashSet<NodeBank>();
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
        for(NodeBank bank : getAllTargetBanks()) {
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
     * Initializes the energy capabilities of this NodeBank
     */
    public void loadEnergy() {
        energyHandler = LazyOptional.of(() -> energyStorage);
    }

    public void invalidateEnergy() {
        energyHandler.invalidate();
    }

    public <T> @NotNull LazyOptional<T> provideEnergyCapabilities(@NotNull Capability<T> cap, @Nullable Direction side) {
        return provideEnergyCapabilities(cap, side, getInteractionDirections());
    }

    /***
     * Removes the last connection made to this NodeBank. Used primarily to cancel
     * a connection, where the latest connection made to this NodeBank is a FakeNodeConnection.
     * @param sourceID ElectricNode to remove the last connection from.
     */
    public void cancelConnection(String sourceID) {
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
    public Pair<NodeConnectResult, FakeNodeConnection> makeFakeConnection(WireSpool spoolType, String fromID, Entity targetEntity) {

        Vec3 sourcePos = get(fromID).getPosition();
        FakeNodeConnection fakeConnection = new FakeNodeConnection(spoolType, fromID, sourcePos, targetEntity);
        Mechano.log("Fake connection established: " + fakeConnection + ", to Entity " + targetEntity);

        NodeConnectResult result = allNodes[indexOf(fromID)].addConnection(fakeConnection);

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
    public NodeConnectResult connect(FakeNodeConnection fake, NodeBank targetBank, String targetID) {
        Vec3 sourcePos = fake.getSourcePos();
        Vec3 destPos = targetBank.get(targetID).getPosition();
        String fromID = fake.getSourceID();
        WireSpool spoolType = fake.getSpoolType();

        NodeConnection fromConnection = new ElectricNodeConnection(spoolType, this, sourcePos, targetBank, targetID);
        NodeConnection targetConnection = new ElectricNodeConnection(spoolType, targetBank, destPos, this, fromID, true);

        if(targetBank.equals(this))
            return NodeConnectResult.LINK_CONFLICT;

        NodeConnectResult r1 = targetBank.allNodes[targetBank.indexOf(targetID)].addConnection(targetConnection);
        
        if(r1.isSuccessful()) {
            allNodes[indexOf(fake.getSourceID())].replaceLastConnection(fromConnection);
            markDirty(); targetBank.markDirty();
            return NodeConnectResult.WIRE_SUCCESS;
        }        
        return r1;
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
    public NodeConnectResult connect(WireSpool spoolType, String fromID, NodeBank targetBank, String targetID) {
        Vec3 sourcePos = get(fromID).getPosition();
        Vec3 destPos = targetBank.get(targetID).getPosition();

        NodeConnection fromConnection = new ElectricNodeConnection(spoolType, this, sourcePos, targetBank, targetID);
        NodeConnection targetConnection = new ElectricNodeConnection(spoolType, targetBank, destPos, this, fromID, true);
        Mechano.log("Connection established from: " + fromConnection + "  to: \n" + targetConnection);

        NodeConnectResult r1 = allNodes[indexOf(fromID)].addConnection(fromConnection);
        NodeConnectResult r2 = targetBank.allNodes[targetBank.indexOf(targetID)].addConnection(targetConnection);
    
        if(r1.isSuccessful() && r2.isSuccessful()) {
            markDirty(); targetBank.markDirty();
            return NodeConnectResult.WIRE_SUCCESS;
        }
        return r1;
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
    public static NodeBank retrieveFrom(Level world, BlockEntity root, Vec3i relativePos) {
        return retrieve(world, root.getBlockPos().subtract(relativePos));
    }

    @Override
    public void onEnergyUpdated() {
        target.setChanged();
        MechanoPackets.sendToAllClients(new EnergySyncS2CPacket(energyStorage.getEnergyStored(), target.getBlockPos()));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getEnergyCapability() {
        return energyHandler.cast();
    }

    @Override
    public void setEnergyStored(int energy) {
        energyStorage.setEnergyStored(energy);
    }
}
