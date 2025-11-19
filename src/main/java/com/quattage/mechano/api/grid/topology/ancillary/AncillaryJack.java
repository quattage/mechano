package com.quattage.mechano.api.grid.topology.ancillary;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.CircuitFactory;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.api.grid.topology.CircuitComponent;
import com.quattage.mechano.api.grid.topology.Node;
import com.quattage.mechano.api.grid.topology.Terminal;
import com.quattage.mechano.foundation.numeric.VectorOperations;

import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
public abstract class AncillaryJack implements Node {

    private final String componentID;
    private @Nullable Griddable source;
    protected @Nullable Node parent;
    private boolean isVisible = true;

    public AncillaryJack(String componentID, boolean isVisible) {
        CircuitComponent.checkID(componentID);
        this.componentID = componentID;
        this.isVisible = isVisible;
    }

    /**
     * Called during the initial population of the parent
     * circuit.
     * @param source The Griddable that own this ancillary
     * @param attached (Optional) The {@link Node} that this ancillary is attached to
     */
    public void attachTo(@Nullable Griddable source, @Nullable Node attached) { 
        this.source = source; 
        this.parent = attached;
    }

    /**
     * Called during the initial population of the parent
     * circuit.
     * @param source The Griddable that own this ancillary
     * @param attached (Optional) The {@link Node} that this ancillary is attached to
     */
    public void attachTo(Griddable source) { attachTo(source, this.parent); }

    @Override
    public void updateOwnership(@Nullable Griddable source, CircuitComponent parent, int index) {
        attachTo(source);
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

    public Vector3d getRealPosition(Vector3d basePos, Quaternionf baseRot, Vector3d workingVector) {
        Vector3f off = getRotatedOffset(baseRot);
        return basePos.add(off, workingVector);
    }

    public boolean isVisible() { 
        return isVisible; 
    }

    
    public void forAllEdges(Shapes.DoubleLineConsumer action) {
        float size = getSize();
        double nx = getXO() - size;
        double ny = getYO() - size;
        double nz = getZO() - size;
        double px = getXO() + size;
        double py = getYO() + size;
        double pz = getZO() + size;
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

    abstract void translateStack(Vector3d basis, Vec3 cameraPos, PoseStack matrixStack);

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
        getSource().drawGUILabel(tooltip, posX, posY, graphics);
    }
    
    @OnlyIn(Dist.CLIENT)
    public boolean isIntersecting(Vector3d basis, VectorOperations.Ray ray) {
        AABB hitbox = makeAABB(basis, getSize());
        return hitbox.clip(ray.start, ray.end).isPresent();
    }

    /**
     * Must be called at least once per ancillary instance to 
     * initially populate this object's internal Griddable reference.
     * If this method is not called, ancillaries will not work correctly.
     * @param source
     */
    public void loadOnto(Griddable source) {
        this.source = source;
    }

    public @NotNull Griddable getSource() {
        return source;
    }

    @Override public String getComponentID() { return componentID; }

    @Override public String describeState() { 
        if(parent == null) return "@(null)";
        return "@(" + parent.getIndex() + ", " + parent.hashCode() + ")"; 
    }

    @Override
    public String toString() {
        return getComponentID() + "[" + describeState() + "]";
    }

    // the rest of the implementation of this class defers itself to the parent joint

    @Override
    public Collection<Terminal> getTerminals() {
        assertAttached();
        return parent.getTerminals();
    }

    @Override
    public void forEachNode(Consumer<Node> cons) {
        assertAttached();
        parent.forEachNode(cons);
    }

    @Override
    public ResourceLocation asResource() {
        return Mechano.asResource(getSerializedName());
    }

    @Override
    public boolean isGrounded() {
        assertAttached();
        return parent.isGrounded();
    }

    @Override
    public void saturate() {
        assertAttached();
    }

    @Override
    public void reset() {
        assertAttached();
    }

    @Override
    public @Nullable CircuitComponent getParentComponent() {
        return parent;
    }

    @Override
    public double getVoltage() {
        if(parent == null) return 0;
        return parent.getVoltage();
    }

    @Override
    public void setVoltage(double volts) {
        if(parent == null) return;
        parent.setVoltage(volts);
    }

    @Override
    public Collection<AncillaryJack> getAllAncillaries() {
        assertAttached();
        return parent.getAllAncillaries();
    }

    @Override
    public boolean attach(Terminal pin) {
        assertAttached();
        return parent.attach(pin);
    }

    @Override
    public boolean detach(Terminal pin) {
        assertAttached();
        return parent.detach(pin);
    }

    @Override
    public boolean attach(Griddable source, AncillaryJack jack) {
        assertAttached();
        return parent.attach(source, jack);
    }

    @Override
    public boolean detach(Griddable source, AncillaryJack jack) {
        assertAttached();
        return parent.detach(source, jack);
    }

    @Override
    public boolean involves(Terminal pin) {
        if(parent == null) return false;
        return parent.involves(pin);
    }

    @Override
    public void dispose() {
        this.parent = null;
        this.source = null;
    }

    @Override
    public int getIndex() {
        assertAttached();
        return parent.getIndex();

    }

    @Override
    public void updateOwnership(CircuitComponent parent, int index) {
        assertAttached();
        this.parent.updateOwnership(parent, index);
    }

    @Override
    public int size() {
        if(parent == null) return 0;
        return parent.size();
    }

    @Override public boolean hasAncillaries() { return true; }

    private void assertAttached() { if(parent == null) throw new IllegalArgumentException("This AncillaryJack is not attached to a node"); }

    @Override
    public Type getType() {
        return CircuitComponent.Type.ANCILLARY_NODE;
    }

    @Override
    public boolean isSignificant() {
        return parent != null && parent.isSignificant();
    }
}
