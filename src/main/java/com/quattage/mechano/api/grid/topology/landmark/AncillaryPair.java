package com.quattage.mechano.api.grid.topology.landmark;

import java.util.Objects;

import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridConstruct;
import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.NodeUnionSet.NodePair;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class AncillaryPair {

    protected GridUUID<?> startID, endID;
    protected AncillaryNode<?> startNode, endNode;

    public AncillaryPair(AncillaryNode<?> startNode, AncillaryNode<?> endNode) {
        assignStart(startNode);
        assignEnd(endNode);
    }

    public AncillaryPair(GridUUID<?> startID, AncillaryNode<?> startNode, GridUUID<?> endID, AncillaryNode<?> endNode) {
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

    public AncillaryPair assignStart(GridUUID<?> startID, AncillaryNode<?> startNode) {
        Objects.requireNonNull(startID);
        GridConstruct.assertHierarchyIs(startID, ComponentHierarchy.ANCILLARY);
        Objects.requireNonNull(startNode);
        this.startID = startID;
        this.startNode = startNode;
        return this;
    }

    public AncillaryPair assignEnd(GridUUID<?> endID, AncillaryNode<?> endNode) {
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

    public GridUUID<?> getStartID() {
        return startID;
    }
    
    public AncillaryNode<?> getStartAncillary() {
        return startNode;
    }

    public Node getStartNode() {
        return (Node)getStartAncillary().getParentConstruct();
    }

    public GridUUID<?> getEndID() {
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

    public boolean isDynamic() {
        Griddable<?> startSource = startNode.getProviderSource();
        Griddable<?> endSource = endNode.getProviderSource();
        return (startSource != null && startSource.canMoveDynamically()) || 
            (endSource != null && endSource.canMoveDynamically());
    }

    public AncillaryPair getFlipped(LevelReader world) {
        return Grid.getUnsided(world).getLink(endNode.getProviderSource(), startID);
    }

    @OnlyIn(Dist.CLIENT) 
    public void render(BlockEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        
    }

    public AncillaryPair validateSelf() {
        assertHasIDs();
        assertHasAncillaries();
        assertHasSources();
        assertNonConflict();
        return this;
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
