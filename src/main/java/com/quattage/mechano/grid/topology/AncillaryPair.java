package com.quattage.mechano.grid.topology;

import java.util.Objects;
import java.util.function.Consumer;

import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.Griddable;
import com.quattage.mechano.grid.Grid;
import com.quattage.mechano.grid.GridTracking;
import com.quattage.mechano.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.grid.topology.core.CircuitComponent;
import com.quattage.mechano.grid.topology.core.GridUUID;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct;
import com.quattage.mechano.grid.topology.core.HierarchicalConstruct.GridReferent;
import com.quattage.mechano.grid.topology.core.Node;
import com.quattage.mechano.grid.topology.core.NodePair;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class AncillaryPair extends NodePair implements CircuitComponent {

    protected GridUUID<?> startID, endID;

    public AncillaryPair(AncillaryNode<?> startNode, AncillaryNode<?> endNode) {
        super(startNode, endNode);
        assignStart(startNode);
        assignEnd(endNode);
    }

    public AncillaryPair(GridUUID<?> startID, AncillaryNode<?> startNode, GridUUID<?> endID, AncillaryNode<?> endNode) {
        super(startNode, endNode);
        assignStart(startID, startNode);
        assignEnd(endID, endNode);
    }

    public AncillaryPair flippedCopy() {
        return new AncillaryPair((AncillaryNode<?>) b, (AncillaryNode<?>) a);
    }

    public AncillaryPair assignStart(AncillaryNode<?> ancillary) {
        Objects.requireNonNull(ancillary);
        this.a = ancillary;
        this.startID = GridTracking.getAddress(GridTracking.getReferentOrThrow(ancillary), ancillary);
        return this;
    }

    public AncillaryPair assignEnd(AncillaryNode<?> ancillary) {
        Objects.requireNonNull(ancillary);
        this.b = ancillary;
        this.endID = GridTracking.getAddress(GridTracking.getReferentOrThrow(ancillary), ancillary);
        return this;
    }

    public AncillaryPair assignStart(GridUUID<?> startID, AncillaryNode<?> startNode) {
        Objects.requireNonNull(startID);
        HierarchicalConstruct.assertHierarchyIs(startID, ComponentHierarchy.ANCILLARY);
        Objects.requireNonNull(startNode);
        this.startID = startID;
        this.a = startNode;
        return this;
    }

    public AncillaryPair assignEnd(GridUUID<?> endID, AncillaryNode<?> endNode) {
        Objects.requireNonNull(endID);
        HierarchicalConstruct.assertHierarchyIs(endID, ComponentHierarchy.ANCILLARY);
        Objects.requireNonNull(endNode);
        this.endID = endID;
        this.b = endNode;
        return this;
    }

    public void onAddedToGrid(Grid grid) {
        Griddable<?> source = GridTracking.getReferentOrThrow(grid.getWorld(), a);
        if(source != null) {
            source.getTerminus().setHasConnections();
            source.onAddedToGrid(grid);
        }
        source = GridTracking.getReferentOrThrow(grid.getWorld(), b);
        if(source != null) {
            source.getTerminus().setHasConnections();
            source.onAddedToGrid(grid);
        }
    }

    public void onRemovedFromGrid(Grid grid) {
        Griddable<?> source = GridTracking.getReferentOrThrow(grid.getWorld(), a);
        if(source != null) {
            source.getTerminus().setHasConnections(false);
            source.onRemovedFromGrid(grid);
        }
        source = GridTracking.getReferentOrThrow(grid.getWorld(), b);
        if(source != null) {
            source.getTerminus().setHasConnections(false);
            source.onRemovedFromGrid(grid);
        }
    }

    @Override
    public int nodeCount() {
        assertHasNodes();
        return this.a.nodeCount() + this.b.nodeCount();
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        assertHasNodes();
        getStartAncillary().forEachNode(cons);
        getEndAncillary().forEachNode(cons);
    }

    @Override
    public boolean isGrounded() {
        return (a != null && a.isGrounded()) || (b != null && b.isGrounded());
    }

    @Override
    public void saturate() {
        if(a != null) a.saturate();
        if(b != null) b.saturate();
    }

    @Override
    public void reset() {
        if(a != null) a.reset();
        if(b != null) b.reset();
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
        assertHasNodes();
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
        assertHasNodes();
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
        return (AncillaryNode<?>) a;
    }

    public Node getStartNode() {
        return (Node)getStartAncillary().getParentConstruct();
    }

    public GridUUID<?> getEndID() {
        return endID;
    }

    public AncillaryNode<?> getEndAncillary() {
        return (AncillaryNode<?>) b;
    }

    public Node getEndNode() {
        return (Node)getEndAncillary().getParentConstruct();
    }

    @Override
    public String toString() {
        return "[" + getStartID() + " -> " + getEndID() + "]";
    }

    public Vec3 halfwayBetween() {
        assertHasNodes();
        Vector3d startPos = getStartAncillary().getRealPosition();
        Vector3d endPos = getEndAncillary().getRealPosition();
        return new Vec3(
            (startPos.x + endPos.x) / 2d,
            (startPos.y + endPos.y) / 2d,
            (startPos.z + endPos.z) / 2d
        );
    }

    public BlockPos getMiddlePos(LevelReader world) {
        assertHasNodes();
        BlockPos startPos = getStartAncillary().getBlockPos();
        BlockPos endPos = getEndAncillary().getBlockPos();
        return new BlockPos(
            (int)((startPos.getX() + endPos.getX()) / 2f),
            (int)((startPos.getY() + endPos.getY()) / 2f),
            (int)((startPos.getZ() + endPos.getZ()) / 2f)
        );
    }

    public boolean isDynamic() {
        assertHasNodes();
        Griddable<?> startSource = GridTracking.getReferentOrThrow(a);
        Griddable<?> endSource = GridTracking.getReferentOrThrow(b);
        return (startSource != null && startSource.canMoveDynamically()) || 
            (endSource != null && endSource.canMoveDynamically());
    }

    @OnlyIn(Dist.CLIENT)
    public AncillaryPair getFlipped(LevelReader world) {
        return Grid.client(world).getLinkMatching(endID, startID);
    }

    @OnlyIn(Dist.CLIENT) 
    public void render(BlockEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        
    }

    public void tick(LevelReader world) {
        
    }

    public AncillaryPair validateSelf() {
        assertHasIDs();
        assertHasNodes();
        assertHasSources();
        assertNonConflict();
        return this;
    }

    @Override
    public GridReferent<?> getReferent() {
        if(a == null) {
            throw new IllegalStateException("Couldn't get provider source for " + this 
                + " - This ancillary pair hasn't located its ancillaries yet! (use the overload of this method that requires a world to avoid this error)");
        }
        return a.getReferent();
    }

    @Override
    public GridReferent<?> getReferent(LevelReader world) {
        if(a != null) return getReferent();
        return startID.getReferent(world);
    }

    protected void assertHasIDs() {
        if(startID == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The starting jack's UUID is null! (It was never assigned using assignStart())");
        }
        if(endID == null) {
            throw new NullPointerException("An operation failed on ComponentLink " + this 
                + " - The ending jack's UUID is null! (It was never assigned using assignEnd())");
        }
    }

    @Override
    protected void assertNonConflict() {
        super.assertNonConflict();
        if(startID.equals(endID)) {
            throw new IllegalStateException("An operation failed on ComponentLink " + this 
                + " - The starting and ending UUIDs are identical!");
        }
    }
}
