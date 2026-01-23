package com.quattage.mechano.api.grid.topology.landmark;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.api.ServerGrid;
import com.quattage.mechano.api.catenary.CatenaryMeshBuffer;
import com.quattage.mechano.api.catenary.model.CatenaryModel;
import com.quattage.mechano.api.catenary.model.SimulatedCatenary;
import com.quattage.mechano.api.grid.GridConstruct;
import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.api.transmitter.TransmitterType.UnionFactory;

import net.minecraft.client.multiplayer.ClientLevel;
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
public class ComponentLink<T extends CircuitComponent> extends AncillaryPair implements CircuitComponent, GridConstruct {

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
        return new ComponentLink<T>(component, trns, endID, endNode, startID, startNode);
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

    public @Nullable CircuitComponent applyTo(ServerGrid grid) {
        this.component = TransmitterType.applyUnion(grid, trns.getFactory(), this, getStartAncillary(), getEndAncillary());
        return this.component;
    }

    public void invalidate() {
        this.component.reset();
        this.component = null;
    }

    public TransmitterType getTransmitter() {
        return trns;
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        if(startNode != null) startNode.forEachNode(cons);
        if(endNode != null) endNode.forEachNode(cons);
    }

    @Override
    public boolean isGrounded() {
        return (startNode != null && startNode.isGrounded()) || (endNode != null && endNode.isGrounded());
    }

    @Override
    public void saturate() {
        if(component != null) component.saturate();
    }

    @Override
    public void reset() {
        if(component != null) component.reset();
    }

    @Override
    public String getComponentID() {
        return "link_" + trns.getName();
    }

    @Override
    public String toString() {
        return getComponentID() + "[" + startID + " -> " + endID + "]";
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.LINK;
    }

    @Override
    public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
        if(binding.getHierarchyType() == ComponentHierarchy.ANCILLARY)
            return binding.get() == 0 ? startNode : endNode;
        return component;
    }

    @Override
    public @Nullable GridConstruct getParentConstruct() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public CatenaryModel<?> getCatenary(LevelReader world) {
        if(catenary != null) return catenary;
        AncillaryPair opposite = getFlipped(world);
        if(opposite instanceof ComponentLink<?> cl && cl.catenary != null) {
            this.catenary = cl.catenary;
            return catenary;
        }
        initializeCatenary();
        if(opposite instanceof ComponentLink<?> cl) 
            cl.catenary = this.catenary;
        return catenary;
    }

    @OnlyIn(Dist.CLIENT)
    public void initializeCatenary() {
        catenary = new SimulatedCatenary();
        catenary.setOffset(trns, startNode.getRealPosition(), endNode.getRealPosition())
            .initializeSpan()
            .calculateSegmentation(trns)
            .pinEndpoints();
        catenary.update(trns);
    }

    @OnlyIn(Dist.CLIENT) 
    public void tickCatenary(ClientLevel world) {
        assertHasAncillaries();
        Griddable<?> startSource = startNode.getProviderSource();
        Griddable<?> endSource = startNode.getProviderSource();
        if(GridReferent.choosePrimary(startSource, endSource) != startSource) return;
        Vector3d startPos = startNode.getRealPosition();
        Vector3d endPos = endNode.getRealPosition();
        catenary.setOffset(trns, startPos, endPos);
    }

    @Override
    @OnlyIn(Dist.CLIENT) 
    public void render(BlockEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        Vec3 worldMid = halfwayBetween();
        CatenaryMeshBuffer.REUSABLE 
            .bindTo(trns).at(worldMid).in(owner.getLevel())
            .render(buffers, matrixStack, getCatenary(owner.getLevel()), worldMid.subtract(Vec3.atLowerCornerOf(owner.getBlockPos())), pTicks);
        CatenaryMeshBuffer.REUSABLE.reset();
    }
}


