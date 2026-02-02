package com.quattage.mechano.api.grid.topology.landmark;

import java.util.Objects;

import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.GridConstruct;
import com.quattage.mechano.api.grid.GridConstruct.GridReferent;
import com.quattage.mechano.api.grid.GridConstruct.SourceProvider;
import com.quattage.mechano.api.grid.GridTracking;
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

public class AncillaryPair implements SourceProvider {

    protected GridUUID<?> startID, endID;
    protected AncillaryNode<?> startAnc, endAnc;

    public AncillaryPair(AncillaryNode<?> startNode, AncillaryNode<?> endNode) {
        assignStart(startNode);
        assignEnd(endNode);
    }

    public AncillaryPair(GridUUID<?> startID, AncillaryNode<?> startNode, GridUUID<?> endID, AncillaryNode<?> endNode) {
        assignStart(startID, startNode);
        assignEnd(endID, endNode);
    }

    public AncillaryPair flippedCopy() {
        return new AncillaryPair(endAnc, startAnc);
    }

    public AncillaryPair assignStart(AncillaryNode<?> ancillary) {
        Objects.requireNonNull(ancillary);
        this.startAnc = ancillary;
        this.startID = GridTracking.getAddress(GridTracking.getSource(ancillary), ancillary);
        return this;
    }

    public AncillaryPair assignEnd(AncillaryNode<?> ancillary) {
        Objects.requireNonNull(ancillary);
        this.endAnc = ancillary;
        this.endID = GridTracking.getAddress(GridTracking.getSource(ancillary), ancillary);
        return this;
    }

    public AncillaryPair assignStart(GridUUID<?> startID, AncillaryNode<?> startNode) {
        Objects.requireNonNull(startID);
        GridConstruct.assertHierarchyIs(startID, ComponentHierarchy.ANCILLARY);
        Objects.requireNonNull(startNode);
        this.startID = startID;
        this.startAnc = startNode;
        return this;
    }

    public AncillaryPair assignEnd(GridUUID<?> endID, AncillaryNode<?> endNode) {
        Objects.requireNonNull(endID);
        GridConstruct.assertHierarchyIs(endID, ComponentHierarchy.ANCILLARY);
        Objects.requireNonNull(endNode);
        this.endID = endID;
        this.endAnc = endNode;
        return this;
    }

    public void onAddedToGrid(Grid grid) {
        Griddable<?> source = GridTracking.getSource(grid.getWorld(), startAnc);
        if(source != null) {
            source.getTerminus().setHasConnections();
            source.onAddedToGrid(grid);
        }
        source = GridTracking.getSource(grid.getWorld(), endAnc);
        if(source != null) {
            source.getTerminus().setHasConnections();
            source.onAddedToGrid(grid);
        }
    }

    public void onRemovedFromGrid(Grid grid) {
        Griddable<?> source = GridTracking.getSource(grid.getWorld(), startAnc);
        if(source != null) {
            source.getTerminus().setHasConnections(false);
            source.onRemovedFromGrid(grid);
        }
        source = GridTracking.getSource(grid.getWorld(), endAnc);
        if(source != null) {
            source.getTerminus().setHasConnections(false);
            source.onRemovedFromGrid(grid);
        }
    }

    public boolean contains(Node node) {
        return startsWith(node) || endsWith(node);
    }

    public boolean contains(GridUUID<?> id) {
        return startsWith(id) || endsWith(id);
    }

    public boolean contains(Object obj) {
        return startsWith(obj) || endsWith(obj);
    }

    public boolean startsWith(Node node) {
        Objects.requireNonNull(node);
        assertHasAncillaries();
        return node instanceof AncillaryNode an ? getStartAncillary() == an : getStartNode() == node;
    }

    public boolean startsWith(GridUUID<?> id) {
        Objects.requireNonNull(id);
        assertHasIDs();
        return startID.equals(id);
    }

    public boolean startsWith(Object obj) {
        return switch (obj) {
            case Node node -> startsWith(node);
            case GridUUID<?> id -> startsWith(id);
            case null, default -> false;
        };
    }

    public boolean endsWith(Node node) {
        Objects.requireNonNull(node);
        assertHasAncillaries();
        return node instanceof AncillaryNode an ? getEndAncillary() == an : getEndNode() == node;
    }

    public boolean endsWith(GridUUID<?> id) {
        Objects.requireNonNull(id);
        assertHasIDs();
        return endID.equals(id);
    }

    public boolean endsWith(Object obj) {
        return switch (obj) {
            case Node node -> endsWith(node);
            case GridUUID<?> id -> endsWith(id);
            case null, default -> false;
        };
    }

    public GridUUID<?> getStartID() {
        return startID;
    }
    
    public AncillaryNode<?> getStartAncillary() {
        return startAnc;
    }

    public Node getStartNode() {
        return (Node)getStartAncillary().getParentConstruct();
    }

    public GridUUID<?> getEndID() {
        return endID;
    }

    public AncillaryNode<?> getEndAncillary() {
        return endAnc;
    }

    public Node getEndNode() {
        return (Node)getEndAncillary().getParentConstruct();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.startAnc, this.endAnc);
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AncillaryPair that)) return false;
        return this.startAnc == that.startAnc && this.endAnc == that.endAnc;
    }

    @Override
    public String toString() {
        return "[" + getStartID() + " -> " + getEndID() + "]";
    }

    public NodePair asNodePair() {
        assertHasAncillaries();
        return new NodePair(getStartNode(), getEndNode());
    }

    public Vec3 halfwayBetween() {
        assertHasAncillaries();
        Vector3d startPos = startAnc.getRealPosition();
        Vector3d endPos = endAnc.getRealPosition();
        return new Vec3(
            (startPos.x + endPos.x) / 2d,
            (startPos.y + endPos.y) / 2d,
            (startPos.z + endPos.z) / 2d
        );
    }

    public BlockPos getMiddlePos(LevelReader world) {
        assertHasAncillaries();
        BlockPos startPos = startAnc.getBlockPos();
        BlockPos endPos = startAnc.getBlockPos();
        return new BlockPos(
            (int)((startPos.getX() + endPos.getX()) / 2f),
            (int)((startPos.getY() + endPos.getY()) / 2f),
            (int)((startPos.getZ() + endPos.getZ()) / 2f)
        );
    }

    public boolean isDynamic() {
        assertHasAncillaries();
        Griddable<?> startSource = GridTracking.getSource(startAnc);
        Griddable<?> endSource = GridTracking.getSource(endAnc);
        return (startSource != null && startSource.canMoveDynamically()) || 
            (endSource != null && endSource.canMoveDynamically());
    }

    @OnlyIn(Dist.CLIENT)
    public AncillaryPair getFlipped(LevelReader world) {
        return Grid.client(world).lookup().getLink(endID, startID);
    }

    @OnlyIn(Dist.CLIENT) 
    public void render(BlockEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        
    }

    public void tick(LevelReader world) {
        
    }

    public AncillaryPair validateSelf() {
        assertHasIDs();
        assertHasAncillaries();
        assertHasSources();
        assertNonConflict();
        return this;
    }

    @Override
    public GridReferent<?> getProviderSource() {
        if(startAnc == null) {
            throw new IllegalStateException("Couldn't get provider source for " + this 
                + " - This ancillary pair hasn't located its ancillaries yet! (use the overload of this method that requires a world to avoid this error)");
        }
        return startAnc.getProviderSource();
    }

    @Override
    public GridReferent<?> getProviderSource(LevelReader world) {
        if(startAnc != null) return getProviderSource();
        return startID.getProviderSource(world);
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
        if(startAnc == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting jack is null! (It was never assigned using assignStart())");
        }
        if(endAnc == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending jack is null! (It was never assigned using assignEnd())");
        }
    }

    private void assertHasSources() {
        if(startAnc.getProviderSource() == null) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting jack has no source! (This instance potentially leaked)");
        }
        if(endAnc.getProviderSource() == null) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The ending jack has no source! (This instance potentially leaked)");
        }
    }

    private void assertNonConflict() {
        if(startID.equals(endID)) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting and ending UUIDs are identical!");
        }
        if(startAnc == endAnc) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting and ending AncillaryNode<?> instances are identical!");
        }
    }
}
