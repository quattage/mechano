package com.quattage.mechano.api.grid.topology.landmark.link;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.catenary.CatenaryMeshBuffer;
import com.quattage.mechano.api.catenary.model.CatenaryModel;
import com.quattage.mechano.api.catenary.model.SimulatedCatenary;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.HierarchicalConstruct.GridReferent;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.topology.GridDomain;
import com.quattage.mechano.api.grid.topology.landmark.AncillaryNode;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * A link that connects two {@link AncillaryNode ancillaries}
 * together, like a wire.
 */
public class ComponentLink<T extends CircuitComponent> extends AncillaryPair {

    private @Nullable CatenaryModel<?> catenary;
    private final TransmitterType trns;
    private CircuitComponent component;

    public ComponentLink(TransmitterType trns, AncillaryNode<?> startNode, AncillaryNode<?> endNode) {
        super(startNode, endNode);
        Objects.requireNonNull(trns);
        this.trns = trns;
    }

    public ComponentLink(TransmitterType trns, GridUUID<?> startID, AncillaryNode<?> startNode, GridUUID<?> endID, AncillaryNode<?> endNode) {
        super(startID, startNode, endID, endNode);
        Objects.requireNonNull(trns);
        this.trns = trns;
    }

    private ComponentLink(CircuitComponent component, TransmitterType trns, GridUUID<?> startID, AncillaryNode<?> startNode, GridUUID<?> endID, AncillaryNode<?> endNode) {
        this(trns, startID, startNode, endID, endNode);
        this.component = component;
    }

    @Override
    public ComponentLink<T> flippedCopy() {
        return new ComponentLink<T>(component, trns, endID, (AncillaryNode<?>) b, startID, (AncillaryNode<?>) a);
    }

    /**
     * Returns the {@link CircuitComponent} backed by this ComponentLink.
     * <code>null</code> values returned here indicate either that this ComponentLink's component
     * hasn't been instantiated yet or that the result of a previous call to {@link #applyTo}
     * returned no component.
     * @return The CircuitComponent controlled and instantiated by this ComponentLink's {@link UnionFactory}
     * @see #apply
     */
    public @Nullable CircuitComponent getApplied() {
        return component;
    }

    public @Nullable CircuitComponent applyUnions(GridDomain domain) {
        this.component = TransmitterType.applyUnion(domain, trns.getFactory(), this, this);
        return this.component;
    }

    public TransmitterType getTransmitter() {
        return trns;
    }

    @Override
    public void saturate() {
        super.saturate();
        if(component != null) component.saturate();
    }

    @Override
    public void reset() {
        super.reset();
        if(component != null) component.reset();
    }

    @Override
    public void MNAAllocate(ServerGrid grid, GridDomain domain) {
        super.MNAAllocate(grid, domain);
        if(component != null) component.MNAAllocate(grid, domain);
    }

    @Override
    public void MNADeallocate(ServerGrid grid, GridDomain domain) {
        super.MNADeallocate(grid, domain);
        if(component != null) component.MNADeallocate(grid, domain);
    }

    @Override
    public String getComponentID() {
        return "link_" + trns.getName();
    }

    @Override
    public String toString() {
        return getComponentID() + "[" + startID + " -> " + endID + "]";
    }

    public boolean isPrimary() {
        Griddable<?> startSource = getStartAncillary().getReferent();
        Griddable<?> endSource = getEndAncillary().getReferent();
        GridReferent<?> primary = GridReferent.choosePrimary(startSource, endSource);
        return startSource == primary;
    }

    @OnlyIn(Dist.CLIENT)
    public CatenaryModel<?> getCatenary(LevelReader world) {
        if(catenary != null) return catenary;
        if(isPrimary()) return initializeCatenary();
        AncillaryPair opposite = getFlipped(world);
        if(opposite instanceof ComponentLink<?> cl && cl.catenary != null) {
            this.catenary = cl.getCatenary(world);
            return catenary;
        }
        throw new IllegalStateException("Couldn't get catenary model from " + this 
            + " - This link is not primary, but no opposing link could be located in the grid.");
    }

    @OnlyIn(Dist.CLIENT)
    public CatenaryModel<?> initializeCatenary() {
        catenary = new SimulatedCatenary();
        catenary.setOffset(trns, getStartAncillary().getRealPosition(), getEndAncillary().getRealPosition())
            .initializeSpan()
            .calculateSegmentation(trns)
            .pinEndpoints();
        return catenary;
    }

    @OnlyIn(Dist.CLIENT)
    public void saturateCatenary() {
        if(catenary == null || !catenary.isInitialized())
            initializeCatenary();
        catenary.updateAhead(trns, 256);
    }

    @Override
    public void tick(LevelReader world) {
        assertHasNodes();
        Griddable<?> startSource = getSourceA();
        Griddable<?> endSource = getSourceB();
        if(startSource == null || endSource == null) return;
        Vector3d startPos = getStartAncillary().getRealPosition();
        Vector3d endPos =  getEndAncillary().getRealPosition();
        getCatenary(world).setOffset(trns, startPos, endPos);
        getCatenary(world).update(trns);
    }

    @Override
    @OnlyIn(Dist.CLIENT) 
    public void render(BlockEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        Vec3 worldMid = halfwayBetween();
        CatenaryMeshBuffer.REUSABLE 
            .in(owner.getLevel())
            .at(worldMid)
            .bindTo(trns)
            .render(buffers, matrixStack, getCatenary(owner.getLevel()), worldMid.subtract(Vec3.atLowerCornerOf(owner.getBlockPos())), pTicks);
        CatenaryMeshBuffer.REUSABLE.reset();
    }
}


