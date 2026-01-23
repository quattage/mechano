package com.quattage.mechano.api.grid.topology;

import java.util.Objects;

import org.joml.Vector3d;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridComponentTracker.ComponentHierarchy;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.ComponentUUID;
import com.quattage.mechano.api.grid.component.GridConstruct;
import com.quattage.mechano.api.grid.topology.netlist.NodeUnionSet.NodePair;
import com.quattage.mechano.api.grid.topology.vertex.AncillaryNode;
import com.quattage.mechano.api.grid.topology.vertex.Node;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class AncillaryPair {

    protected ComponentUUID<?> startID, endID;
    protected AncillaryNode<?> startNode, endNode;

    public AncillaryPair(AncillaryNode<?> startNode, AncillaryNode<?> endNode) {
        assignStart(startNode);
        assignEnd(endNode);
    }

    public AncillaryPair(ComponentUUID<?> startID, AncillaryNode<?> startNode, ComponentUUID<?> endID, AncillaryNode<?> endNode) {
        assignStart(startID, startNode);
        assignEnd(endID, endNode);
    }

    public AncillaryPair flippedCopy() {
        return new AncillaryPair(endNode, startNode);
    }

    public AncillaryPair assignStart(AncillaryNode<?> startNode) {
        Objects.requireNonNull(startNode);
        this.startNode = startNode;
        this.startID = startNode.bindUUID(startNode.getProviderSource().getUUID());
        return this;
    }

    public AncillaryPair assignEnd(AncillaryNode<?> endNode) {
        Objects.requireNonNull(endNode);
        this.endNode = endNode;
        this.endID = endNode.bindUUID(endNode.getProviderSource().getUUID());
        return this;
    }

    public AncillaryPair assignStart(ComponentUUID<?> startID, AncillaryNode<?> startNode) {
        Objects.requireNonNull(startID);
        GridConstruct.assertHierarchyIs(startID, ComponentHierarchy.ANCILLARY);
        Objects.requireNonNull(startNode);
        this.startID = startID;
        this.startNode = startNode;
        return this;
    }

    public AncillaryPair assignEnd(ComponentUUID<?> endID, AncillaryNode<?> endNode) {
        Objects.requireNonNull(endID);
        GridConstruct.assertHierarchyIs(endID, ComponentHierarchy.ANCILLARY);
        Objects.requireNonNull(endNode);
        this.endID = endID;
        this.endNode = endNode;
        return this;
    }

    public void onAddedToGrid(Grid grid) {
        
    }

    public void onRemovedFromGrid(Grid grid) {
        
    }

    public ComponentUUID<?> getStartID() {
        return startID;
    }
    
    public AncillaryNode<?> getStartAncillary() {
        return startNode;
    }

    public Node getStartNode() {
        return (Node)getStartAncillary().getParentConstruct();
    }

    public ComponentUUID<?> getEndID() {
        return endID;
    }

    public AncillaryNode<?> getEndAncillary() {
        return endNode;
    }

    public Node getEndNode() {
        return (Node)getEndAncillary().getParentConstruct();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.startNode, this.endNode);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AncillaryPair that)) return false;
        return this.startNode == that.startNode && this.endNode == that.endNode;
    }

    @Override
    public String toString() {
        return "[" + getStartID() + " -> " + getEndID() + "]";
    }

    public NodePair asNodePair() {
        return new NodePair(getStartNode(), getEndNode());
    }

    public Vec3 halfwayBetween() {
        assertHasAncillaries();
        Vector3d startPos = startNode.getRealPosition();
        Vector3d endPos = endNode.getRealPosition();
        return new Vec3(
            (startPos.x + endPos.x) / 2d,
            (startPos.y + endPos.y) / 2d,
            (startPos.z + endPos.z) / 2d
        );
    }

    public BlockPos getMiddlePos(LevelReader world) {
        assertHasAncillaries();
        BlockPos startPos = startNode.getBlockPos(world);
        BlockPos endPos = startNode.getBlockPos(world);
        return new BlockPos(
            (int)((startPos.getX() + endPos.getX()) / 2f),
            (int)((startPos.getY() + endPos.getY()) / 2f),
            (int)((startPos.getZ() + endPos.getZ()) / 2f)
        );
    }

    public AncillaryPair validateSelf() {
        assertHasIDs();
        assertHasAncillaries();
        assertHasSources();
        assertNonConflict();
        return this;
    }

    public boolean isDynamic() {
        Griddable<?> startSource = startNode.getProviderSource();
        Griddable<?> endSource = endNode.getProviderSource();
        return (startSource != null && startSource.canMoveDynamically()) || (endSource != null && endSource.canMoveDynamically());
    }

    private void assertHasIDs() {
        if(startID == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting jack's UUID is null! (It was never assigned using assignStart())");
        }
        if(endID == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending jack's UUID is null! (It was never assigned using assignEnd())");
        }
    }

    protected void assertHasAncillaries() {
        if(startNode == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting jack is null! (It was never assigned using assignStart())");
        }
        if(endNode == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending jack is null! (It was never assigned using assignEnd())");
        }
    }

    private void assertHasSources() {
        if(startNode.getProviderSource() == null) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting jack has no source! (This instance potentially leaked)");
        }
        if(endNode.getProviderSource() == null) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The ending jack has no source! (This instance potentially leaked)");
        }
    }

    private void assertNonConflict() {
        if(startID.equals(endID)) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting and ending UUIDs are identical!");
        }
        if(startNode == endNode) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting and ending AncillaryNode<?> instances are identical!");
        }
    }
}
