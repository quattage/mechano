package com.quattage.mechano.core.electricity.node;

import java.util.ArrayList;
import java.util.HashSet;

import com.quattage.mechano.core.block.orientation.relative.Relative;
import com.quattage.mechano.core.block.orientation.relative.RelativeDirection;
import com.quattage.mechano.core.electricity.node.base.ElectricNode;
import com.quattage.mechano.core.electricity.node.base.ElectricNodeBuilder;
import com.quattage.mechano.core.electricity.node.base.NodeLocation;
import com.quattage.mechano.core.electricity.node.base.NodeMode;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class NodeBankBuilder {
    
    private int capacity = 10000;
    private int maxInput = 5000;
    private int maxOutput = 5000;
    private int startEnergy = 0;

    private BlockEntity target = null;
    
    private final ArrayList<ElectricNode> nodesToAdd = new ArrayList<ElectricNode>();
    private HashSet<RelativeDirection> dirsToAdd = null;

    public NodeBankBuilder() {};

    /***
     * Bind this NodeBank to a given BlockEntity.
     * @param target BlockEntity to bind this NodeBank to
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder at(BlockEntity target) {
        this.target = target;
        return this;
    }

    /***
     * Sets the maximum electircity capacity (in Forge Energy units)
     * of this NodeBank <p> Defaults to 10000.
     * @param capacity
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /***
     * Sets the maximum input and output electricity (in FE per tick)
     * of this NodeBank. <p> Defaults to 5000.
     * @param io FE per Tick
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder maxIO(int io) {
        this.maxInput = io;
        this.maxOutput = io;
        return this;
    }

    /***
     * Sets the maximum input electricity (in FE per tick)
     * of this NodeBank. <p> Defaults to 5000.
     * @param maxInput FE per Tick
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder maxInput(int maxInput) {
        this.maxInput = maxInput;
        return this;
    }

    /***
     * Sets the maximum output electricity (in FE per tick)
     * of this NodeBank. <p> Defaults to 5000.
     * @param maxInput FE per Tick
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder maxOutput(int maxOutput) {
        this.maxOutput = maxOutput;
        return this;
    }

    /***
     * Adds an Energy Capability direction to this NodeBank. This allows
     * energy to pass through the face located in this direction. RelativeDirections
     * (up, down, left, right, etc.) are as you'd expect them to be. Keep in mind that
     * they're relative to the model's orientation in BlockBench. <p>
     * <strong>(RelativeOrientation.FRONT = The arrow pointing north in BlockBench.)
     * </strong>
     * <p>
     * Accepts a RelativeDirection object or a String shorthand:
     * <pre>
     * builder
     * .interfaceSide(RelativeDirection.FRONT) // Adds the local forward direction only.
     * .interfaceSide("A") // adds every direction (all sides).
     * .interfaceSide("ALL") // also adds every direction.
     * .interfaceSide("X") // Adds the local X axis (left and right).
     * .interfaceSide("Y") // Adds the local Y axis (up and down).
     * </pre>
     * @param dir RelativeDirection to add
     * @return This NodeBankBuilder, modified to reflect this change.
     * * Leave empty to completely disable energy interactions.
     */
    public NodeBankBuilder interfaceSide(Relative dir) {
        newDirsIfNull();
        dirsToAdd.add(new RelativeDirection(dir));
        clearDirsIfFull();
        return this;
    }

    /***
     * Adds an Energy Capability direction to this NodeBank. This allows
     * energy to pass through the face located in this direction. RelativeDirections
     * (up, down, left, right, etc.) are as you'd expect them to be. Keep in mind that
     * they're relative to the model's orientation in BlockBench. <p>
     * <strong>(RelativeOrientation.FRONT = The arrow pointing north in BlockBench.)
     * </strong>
     * 
     * <p>
     * Accepts a RelativeDirection object or a String shorthand:
     * <pre>
     * builder
     * .interfaceSide(RelativeDirection.FRONT) // Adds the local forward direction only.
     * .interfaceSide("A") // adds every direction (all sides).
     * .interfaceSide("ALL") // also adds every direction.
     * .interfaceSide("X") // Adds the local X axis (left and right).
     * .interfaceSide("Y") // Adds the local Y axis (up and down).
     * </pre>
     * * Leave empty to completely disable energy interactions.
     * @param dir RelativeDirection to add
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder interfaceSide(String s) {
        newDirsIfNull();
        if(s.toUpperCase().equals("ALL") || s.toUpperCase().equals("A")) {
            dirsToAdd.clear();
        }

        if(s.toUpperCase().equals("X")) {
            dirsToAdd.add(new RelativeDirection(Relative.LEFT));
            dirsToAdd.add(new RelativeDirection(Relative.RIGHT));
            clearDirsIfFull();
            return this;
        }
        
        if(s.toUpperCase().equals("Y")) {
            dirsToAdd.add(new RelativeDirection(Relative.TOP));
            dirsToAdd.add(new RelativeDirection(Relative.BOTTOM));
            clearDirsIfFull();
            return this;
        } 
        
        if(s.toUpperCase().equals("Z")) {
            dirsToAdd.add(new RelativeDirection(Relative.FRONT));
            dirsToAdd.add(new RelativeDirection(Relative.BACK));
            clearDirsIfFull();
            return this;
        } 

        throw new IllegalArgumentException("NodeBank cannot be built" + 
            " - Invalid directional argument: '" + s + "', expected 'ALL', 'X', 'Y', or 'Z'");
    }

    /***
     * Creates a new HashSet if the HashSet is null.
     */
    private void newDirsIfNull() {
        if(dirsToAdd == null) dirsToAdd = new HashSet<RelativeDirection>();
    }

    /***
     * Clears the HashSet if the length is > 6.
     */
    private void clearDirsIfFull() {
        if(dirsToAdd.size() >= 6) dirsToAdd.clear();
    }

    /***
     * Sets this NodeBankBuilder to have no interactions <p>
     * When called, the resulting NodeBank will have its direct energy
     * transfer capabilities disabled. It can still transfer energy
     * through wires, but no energy can be added or subtracted directly
     * through a face. <p>
     * <strong>Note: This doesn't actually need to be called anymore.
     * This behavior is default.</strong>
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder noEnergySides() {
        dirsToAdd = null;
        return this;
    }

    /***
     * Sets the amount of energy that this NodeBank will hold when it is populated.
     * This is normally 0. Changing this basically just means that the EnergyStorage
     * in this NodeBank will start with some free energy when the player places it.
     * @param startEnergy FE to start with
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder withStartingEnergy(int startEnergy) {
        if(startEnergy > capacity) startEnergy = capacity;
        this.startEnergy = startEnergy;
        return this;
    }

    /***
     * Adds a new ElectricNode to this NodeBank
     * @return
     */
    public ElectricNodeBuilder newNode() {
        return new ElectricNodeBuilder(this, target);
    }

    /***
     * Adds a new ElectricNode to this NodeBank from scratch. <p>
     * Requires manual addressing of ElectricNode's constructor.
     * I reccomend using {@link #newNode()} instead.
     * @param node ElectricNode to add
     * @return This NodeBankBuilder, modified to reflect this change.
     */
    public NodeBankBuilder add(ElectricNode node) {
        nodesToAdd.add(node);
        return this;
    }

    /***
     * Builds a new ElectricNode from manually addressed constructor properties.
     * I reccomend using {@link #newNode()} instead.
     * @param location
     * @param id
     * @param maxConnections
     * @return
     */
    public NodeBankBuilder add(NodeLocation location, String id, int maxConnections) {
        return add(new ElectricNode(location, id, maxConnections, nodesToAdd.size()));
    }

    /***
     * Builds a new ElectricNode from manually addressed constructor properties.
     * I reccomend using {@link #newNode()} instead.
     * @param location
     * @param id
     * @param maxConnections
     * @return
     */
    public NodeBankBuilder add(NodeLocation location, String id, NodeMode mode, int maxConnections) {
        return add(new ElectricNode(location, id, mode, maxConnections, nodesToAdd.size()));
    }

    /***
     * Builds a new ElectricNode from manually addressed constructor properties.
     * I reccomend using {@link #newNode()} instead.
     * @param location
     * @param id
     * @param maxConnections
     * @return
     */
    public NodeBankBuilder add(int x, int y, int z, float size, String id, int maxConnections) {
        return add(new ElectricNode(new NodeLocation(target.getBlockPos(), x, y, z, size, Direction.NORTH), id, maxConnections, nodesToAdd.size()));
    }

    /***
     * Builds a new ElectricNode from manually addressed constructor properties.
     * I reccomend using {@link #newNode()} instead.
     * @param location
     * @param id
     * @param maxConnections
     * @return
     */
    public NodeBankBuilder add(int x, int y, int z, float size, Direction defaultDir, String id, int maxConnections) {
        return add(new ElectricNode(new NodeLocation(target.getBlockPos(), x, y, z, size, defaultDir), id, maxConnections, nodesToAdd.size()));
    }

    private void doCompleteCheck() {
        if(target == null) throw new IllegalStateException("NodeBank cannot be built - BlockEntity target is null. (Use .at() during construction)");
        if(nodesToAdd.isEmpty()) throw new IllegalStateException("NodeBank cannot be built - Must contain at least 1 ElectricNode (Use .newNode to add a node)");
    }

    /***
     * Finalizes all changes made to this NodeBankBuilder and returns
     * a new NodeBank. Use this after you've set up all your values.
     * @throws IllegalStateException If you haven't yet configured a BlockEntity target
     * (see {@link #at(BlockEntity) at()})
     * @return a NodeBank instance.
     */
    public NodeBank build() {
        doCompleteCheck();
        return new NodeBank(target, nodesToAdd, dirsToAdd, capacity, maxInput, maxOutput, startEnergy);
    }
}
