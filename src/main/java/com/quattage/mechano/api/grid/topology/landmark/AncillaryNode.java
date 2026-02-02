package com.quattage.mechano.api.grid.topology.landmark;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.api.grid.GridConstruct;
import com.quattage.mechano.api.grid.GridConstruct.GridReferent;
import com.quattage.mechano.api.grid.GridTracking;
import com.quattage.mechano.api.grid.GridTracking.ComponentHierarchy;
import com.quattage.mechano.api.grid.GridUUID;
import com.quattage.mechano.api.grid.GridUUID.UUIDComposite;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.component.CircuitComponent;
import com.quattage.mechano.api.grid.component.CircuitFactory;
import com.quattage.mechano.foundation.WorldlyObject;
import com.quattage.mechano.foundation.numeric.VectorOperations;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * An interactable feature attached to a Joint, like a wire attachment 
 * point or a sided energy capability. This object is created by the 
 * {@link CircuitFactory} when creating circuits attached to {@link Griddable}
 * instances.
 */
public abstract class AncillaryNode<T extends GridUUID<T>> implements Node, WorldlyObject, GridReferent<T> {

    public static final byte MAX_SHARED_OCCUPANCY = (byte)16;

    private String componentID;
    private @Nullable Griddable<T> source;
    protected @Nullable Node parent;
    private boolean isVisible = true;

    public AncillaryNode(String componentID, boolean isVisible) {
        CircuitComponent.assertValidID(componentID);
        this.componentID = componentID;
        this.isVisible = isVisible;
    }

    @Override
    public boolean localAttach(Terminal pin) {
        assertAttached();
        return parent.localAttach(pin);
    }

    @Override
    public boolean localAttach(Griddable<?> source, AncillaryNode<?> jack) {
        assertAttached();
        return parent.localAttach(source, jack);
    }

    @Override
    public boolean localDetach(Terminal pin) {
        assertAttached();
        return parent.localDetach(pin);
    }

    @Override
    public boolean localDetach(Griddable<?> source, AncillaryNode<?> jack) {
        assertAttached();
        return parent.localDetach(source, jack);
    }

    @Override
    public List<AncillaryNode<?>> getAncillaries() {
        assertAttached();
        return parent.getAncillaries();
    }

    @Override
    public Terminal[] getTerminals() {
        assertAttached();
        return parent.getTerminals();
    }

    /**
     * Called during the initial population of the parent
     * circuit.
     * @param source The Griddable that owns this ancillary
     * @param attached (Optional) The {@link Node} that this ancillary is attached to
     */
    @Override
    @SuppressWarnings("unchecked")
    public void updateOwnership(@Nullable Griddable<?> source, GridConstruct parent) {
        GridConstruct.assertValidOwnership(this, parent);
        this.source = (@Nullable Griddable<T>) source; 
        this.parent = (Node) parent;
    }

    public int getAncillaryNodex() {
        return parent == null ? -1 : parent.getAncillaries().indexOf(this);
    }

    public abstract float getXO();
    public abstract float getYO();
    public abstract float getZO();
    public abstract float getSize();

    /**
     * Returns an AABB describing this AncillaryJack's hitbox.
     * @param basis The real-world position of the parent griddable
     * @param rotation (Optional) a quaternion BlockState or Entity rotation
     * @param size (Optional) A custom size factor for scaling the AABB. Defaults to this jack's actual size.
     * @return A new AABB instance
     */
    public final AABB makeAABB(Vector3d basis) { return makeAABB(basis, getSize()); }

    /**
     * Returns an AABB describing this AncillaryJack's hitbox.
     * @param basis The real-world position of the parent griddable
     * @param rotation (Optional) a quaternion BlockState or Entity rotation
     * @param size (Optional) A custom size factor for scaling the AABB. Defaults to this jack's actual size.
     * @return A new AABB instance
     */
    public abstract AABB makeAABB(Vector3d basis, float size);

    public Vector3f getRotatedOffset(Quaternionf rotation) {
        Vector3f centered = new Vector3f(getXO() + 0.5f, getYO() + 0.5f, getZO() + 0.5f);
        return centered.rotate(rotation);
    }

    public Vector3f getRotatedOffset() {
        return getRotatedOffset(GridTracking.getSource(this).getSourceRotation());
    }

    public Vector3d getRealPosition(Vector3d basePos, Quaternionf baseRot, Vector3d workingVector) {
        Vector3f off = getRotatedOffset(baseRot);
        return basePos.add(off, workingVector);
    }

    public Vector3d getRealPosition() {
        Griddable<?> source = GridTracking.getSource(this);
        if(source == null) return new Vector3d();
        return source.getSourcePos().add(0.5, 0.5, 0.5).add(getXO(), getYO(), getZO());
    }

    public boolean isVisible() { 
        return isVisible; 
    }

    public void forAllEdges(Shapes.DoubleLineConsumer action) {
        float size = getSize();
        double nx = getXO() - size, ny = getYO() - size, nz = getZO() - size;
        double px = getXO() + size, py = getYO() + size, pz = getZO() + size;
        // bottom square
        action.consume(nx, ny, nz, nx, ny, pz);
        action.consume(nx, ny, pz, px, ny, pz);
        action.consume(px, ny, pz, px, ny, nz);
        action.consume(px, ny, nz, nx, ny, nz);
        // top square
        action.consume(nx, py, nz, nx, py, pz);
        action.consume(nx, py, pz, px, py, pz);
        action.consume(px, py, pz, px, py, nz);
        action.consume(px, py, nz, nx, py, nz);
        // vertical sections
        action.consume(nx, ny, nz, nx, py, nz);
        action.consume(nx, ny, pz, nx, py, pz);
        action.consume(px, ny, pz, px, py, pz);
        action.consume(px, ny, nz, px, py, nz);
    }

    public abstract void translateStack(Vector3d basis, Vec3 cameraPos, PoseStack matrixStack);

    @OnlyIn(Dist.CLIENT)
    public boolean drawToBuffer(Vector3d basis, Vec3 cameraPos, PoseStack matrix, VertexConsumer buffer, float pTicks) {
        if(!isVisible || !VectorOperations.isInWorld(basis)) return false;
        matrix.pushPose();
        translateStack(basis, cameraPos, matrix);
        PoseStack.Pose transform = matrix.last();
        forAllEdges((x1, y1, z1, x2, y2, z2) -> {
            // yoinked from vanilla
            float xD = (float)(x2 - x1), yD = (float)(y2 - y1), zD = (float)(z2 - z1);
            float len = Mth.sqrt(xD * xD + yD * yD + zD * zD);
            xD /= len; yD /= len; zD /= len;
            buffer.addVertex(transform.pose(), (float)x1, (float)y1, (float)z1)
                .setColor(0, 0, 0, 0.4f)
                .setNormal(transform.copy(), xD, yD, zD);
            buffer.addVertex(transform.pose(), (float)x2, (float)y2, (float)z2)
                .setColor(0, 0, 0, 0.4f)
                .setNormal(transform.copy(), xD, yD, zD);
        });
        matrix.popPose();
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean drawToOutliner(Vector3d basis, Color color, float sizeTicks, float pTicks) {
        if(!isVisible || !VectorOperations.isInWorld(basis)) return false;
        AABB visual = makeAABB(basis, getSize() * sizeTicks);
        Outliner.getInstance().showAABB(this.hashCode(), visual)
            .disableCull()
            .disableLineNormals()
            .colored(color)
            .lineWidth(0.03125f * sizeTicks);
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public void drawGUILabel(List<Component> tooltip, float posX, float posY, GuiGraphics graphics) {
        assertAttached();
        source.drawGUILabel(tooltip, posX, posY, graphics);
    }
    
    @OnlyIn(Dist.CLIENT)
    public boolean isIntersecting(Vector3d basis, VectorOperations.Ray ray) {
        AABB hitbox = makeAABB(basis, getSize());
        return hitbox.clip(ray.start, ray.end).isPresent();
    }

    @Override
    public int getNodalIndex() {
        assertAttached();
        return parent.getNodalIndex();
    }

    @Override
    public void setNodalIndex(int nodalIndex) {
        assertAttached();
        parent.setNodalIndex(nodalIndex);
    }

    @Override
    public int indexOf(AncillaryNode<?> jack) {
        if(jack == this) return parent.indexOf(jack);
        throw new UnsupportedOperationException("Failed while getting the index of a jack from itself - The supplied Jack instance didn't match this one (" 
            + jack + ") - This method shouldn't be called on AncillaryJack instances!");
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        assertAttached();
        parent.forEachNode(cons);
    }

    @Override
    public boolean isGrounded() {
        assertAttached();
        return parent.isGrounded();
    }

    @Override
    public void saturate() {
        assertAttached();
        parent.saturate();
    }

    @Override
    public void reset() {
        assertAttached();
        parent.reset();
    }

    @Override
    public @Nullable GridConstruct getParentConstruct() {
        return parent;
    }

    @Override
    public GridTracking getTrackerScope() {
        assertAttached();
        return source.getTrackerScope();
    }

    @Override
    public @Nullable Griddable<?> getProviderSource() {
        assertAttached();
        return source;
    }

    @Override
    public BlockPos getBlockPos() {
        return source.getBlockPos();
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer sp) {
        return source == null ? false : source.isBeingTrackedBy(sp);
    }

    @Override
    public T getUUID() {
        throw new UnsupportedOperationException("AncillaryNodes cannot be queried for UUIDs - Use bindUUID() instead");
    }

    public Node getAssociatedNode() {
        assertAttached();
        return parent;
    }

    @Override
    public void dispose() {
        if(parent != null) 
            componentID += " (disposed)";
        this.parent = null;
        this.source = null;
        isVisible = false;
    }

    @Override
    public boolean hasBeenDisposed() {
        return componentID.endsWith("(disposed)");
    }

    @Override 
    public boolean hasAncillaries() { 
        return true; 
    }

    @Override
    public @Nullable Level getWorld() {
        return source == null ? null : source.getWorld();
    }

    @Override
    public ComponentHierarchy getHierarchyType() {
        return ComponentHierarchy.ANCILLARY;
    }

    private void assertAttached() { 
        if(parent == null) 
            throw new IllegalArgumentException("Error performing operation on AncillaryJack - This jack has no parent node!");
        if(source == null)
            throw new IllegalArgumentException("Error performing operation on AncillaryJack - This jack has no source griddable!");
    }

    @Override 
    public String getComponentID() { 
        return componentID; 
    }

    @Override
    public String toString() {
        return "AncillaryNode[" + getComponentID() + " @" + System.identityHashCode(this) + "]";
    }

    @Override
    public @Nullable CircuitComponent getComponent(UUIDComposite binding) {
        return this;
    }
}
