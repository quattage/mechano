package com.quattage.mechano.core.electricity.node;

import java.util.ArrayList;
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
import com.quattage.mechano.network.MechanoPackets;
import com.simibubi.create.foundation.utility.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;


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

    public static final float HITFAC = 3f;

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

    public Optional<RelativeDirection[]> getRelDirs() {
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
     * Retrieves this NodeBank as an array
     * @return The raw aray stored within this NodeBan
     */
    public ElectricNode[] values() {
        return allNodes;
    }

    /*** 
     * The length of this NodeBank
     * @return int representing how many ElectricNodes can be held in this NodeBank
     */
    public int length() {
        return allNodes.length;
    }

    public Pair<ElectricNode, Double> getClosest(Vec3 hit) {
        return getClosest(hit, 0.0f);
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
        if(length() == 1) 
            return Pair.of(allNodes[0], 0.1);

        double lastDist = 100;
        boolean customTolerance = tolerance <= 0;

        Pair<ElectricNode, Double> out = Pair.of(null, null);

        for(int x = 0; x < allNodes.length; x++) {
            Vec3 center = allNodes[x].getPosition();
            double dist = Math.abs(hit.distanceTo(center));
            
            if(customTolerance) tolerance = allNodes[x].getHitSize() * 5f;
            if(dist > tolerance) continue;

            if(dist < 0.0001) {
                return Pair.of(allNodes[x], Double.valueOf(dist));
            }

            if(dist < lastDist) {
                out.setFirst(allNodes[x]);
                out.setSecond(Double.valueOf(dist));
            }

            lastDist = dist;
        }

        if(out.getFirst() == null || out.getSecond() == null) return null;

        return out;
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
        get(sourceID).nullifyLastConnection();
        markDirty();
    }

    /***
     * Adds a FakeNodeConnection at the given fromID in this NodeBank.
     * Fake Connections hold a connection between a source BlockEntity and a
     * destination Entity, rather than to BlockEntity positions. 
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
        //Mechano.log("Connection established from: " + fromConnection + "  to: \n" + targetConnection);

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
