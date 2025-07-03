package com.quattage.mechano.foundation.api.landmark;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.classifier.GridUUID;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.Catenary;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;
import com.quattage.mechano.foundation.catenary.meshing.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.model.ParametricCatenary;
import com.quattage.mechano.foundation.catenary.model.SimulatedCatenary;
import com.quattage.mechano.foundation.item.SpoolItem;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public class GridCatenary extends Connection {

    private AnchorPoint start;
    private AnchorPoint end;
    private Catenary<?> catenary;

    public GridCatenary(LevelReader world, AnchorPoint start, AnchorPoint end, TransmitterType<?> trns, boolean preGenerate) {
        super(trns.make(), 0);
        if(!world.isClientSide())
            throw new IllegalArgumentException("Can't instantiate a client-sided GridCatenary in a server-sided world!");
        if(start.getAddress().equals(end.getAddress()))
            throw new IllegalArgumentException("Can't instantiate a GridCatenary where both the start and end positions are the same!");
        Vec3 startPos = start.getPos(world);
        Vec3 endPos = end.getPos(world);
        length = (float)startPos.distanceTo(endPos);
        this.start = start;
        this.end = end;
        if(preGenerate) prebuildWire(world);
        else startWire(world);
    }

    private GridCatenary(AnchorPoint start, AnchorPoint end, Catenary<?> cat, Transmitter<?> trns, float length) {
        super(trns, length);
        this.start = start; 
        this.end = end;
        this.catenary = cat;
        catenary.invertOffset();
        this.catenary.maxLength = trns.getType().getMaxLength();
    }

    public void startWire(LevelReader world) {
        this.catenary = new SimulatedCatenary()
            .setOffset(start.getPos(world), end.getPos(world))
            .initialize();
        this.catenary.setTension(trns.getType().defaults.getTension());
        this.catenary.maxLength = trns.getType().getMaxLength();
        return;
    }

    public void prebuildWire(LevelReader world) {
        this.catenary = new ParametricCatenary()
            .setOffset(start.getPos(world), end.getPos(world))
            .initialize();
        this.catenary.setTension(trns.getType().defaults.getTension());
        this.catenary.update();
        this.catenary = catenary.toSimulated(true);
        this.catenary.maxLength = trns.getType().getMaxLength();
        this.catenary.updateAhead(256);
        return;
    }

    @Override
    public GridCatenary inverseCopy() {
        return new GridCatenary(end, start, catenary, trns, length);
    }

    @Override
    public GridUUID getStart() {
        return start == null ? null : start.getAddress();
    }

    @Override
    public GridUUID getEnd() {
        return start == null ? null : end.getAddress();
    }

    @Override
    public boolean isClientSide() {
        return true;
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        throw new UnsupportedOperationException("Client-sided GridCatenaries cannot be serialized!");
    }

    @Override
    public String getConnectionTypeName() {
        return "GridCatenary";
    }

    public boolean isStatic() {
        return !start.getAddress().canMoveDynamically() && !end.getAddress().canMoveDynamically();
    }


    @Override
    public Tension getTension() {
        return catenary.getTension();
    }

    @Override
    public boolean setTension(Tension tension) {
        return catenary.setTension(tension);
    }

    @Override
    public float getLength() {
        return catenary.length;
    }

    @Override
    public float getMaxLength() {
        return trns.getType().getMaxLength();
    }

    @Override
    public boolean resetTension() {
        Tension defaultTension = trns.getType().defaults.getTension();
        if(catenary.tension == defaultTension) return false;
        this.catenary.tension = defaultTension;
        return true;
    }

    /**
     * Changes the offset of this catenary to match its current position
     * in the world.
     */
    @Override
    public void updateShape(LevelReader world, float pTicks) {
        this.length = getEuclideanDistance(world, getStart(), getEnd());
        catenary.setOffset(start.getPos(world, pTicks)  , end.getPos(world, pTicks));
        catenary.update();
    }

    /**
     * Imparts forces upon attached entities
     * according to this catenary's tension
     * @param world World to use as a basis for acquiring additional information about both ends of this catenary
     */
    public void updateKinematics(LevelReader world) {
        if(getEnd().getAnchorPoints(world).isLoose()) {
            Vec3 reelDir = start.getPos(world).subtract(end.getPos(world)).normalize();
            float dirDot = (float)getEnd().getAttachmentVelocity(world).normalize().dot(reelDir);
            if(dirDot < 0) {
                getEnd().setAttachmentVelocity(world, Vec3.ZERO);
                removeFrom(world);
                return;
            }
            Vec3 velocity = getEnd().getAttachmentVelocity(world);
            Vec3 tangential = velocity.subtract(reelDir.scale(velocity.dot(reelDir)));
            Vec3 force = reelDir.scale(CatenaryAttributes.KINEMATIC_SOFT);
            force = force.subtract(tangential.scale(CatenaryAttributes.KINEMATIC_DAMP));
            getEnd().applyForceToAttachment(world, force, true);
            return;
        }

        GridUUID[] ordered = orderedByWeight(world);
        if(!ordered[1].canMoveDynamically()) return;
        Vec3 diff = ordered[0].getPos(world).subtract(ordered[1].getPos(world));
        float softLength = catenary.maxLength * CatenaryAttributes.KINEMATIC_SOFT;
        if(diff.length() < softLength) return;
        ordered[1].applyForceToAttachment(world, diff.normalize().scale(
            0.1 * Math.min(1f, (length - softLength) / (catenary.maxLength - softLength))
        ));
    }

    public void adjustMaxLength(ItemStack stack) {
        if(!(stack.getItem() instanceof SpoolItem<?> schpool)) {
            Mechano.LOGGER.warn("Attempted to adjust maximum working length of " + this 
                + " from invalid item '" + stack.getItem().getClass().getSimpleName() + "!'");
            return;
        }
        if(!schpool.getTransmitterType().equals(trns.getType())) {
            Mechano.LOGGER.warn("Attempted to adjust maximum working length of " + this 
                + " from spool with mismatched transmitter (expected '" + trns.getType() + ",' got '" + schpool.getTransmitterType() + "')");
            return;
        }
        catenary.maxLength = Math.min(trns.getType().getMaxLength(), (stack.getMaxDamage() - stack.getDamageValue()) / 2f);
    }

    /**
     * Sets this GridCatenary's endpoint to the given AnchorPoint. If
     * <code>newEnd</code>'s address matches this GridCatenary's current
     * end point, nothing happens.
     * @param world Level to opreate within (for reasserting this GridCatenary)
     * @param newEnd AnchorPoint to set this GridCatenary's endpoint to
     * @return this GridCatenary for chaining.
     */
    public GridCatenary rebindEndpoint(LevelReader world, AnchorPoint newEnd) {
        if(this.end.getAddress().equals(newEnd.getAddress())) 
            return this;
        reassertAndDo(world, () -> {
            this.end = newEnd;
            updateShape(world, 1);
        });
        return this;
    }

    /**
     * Renders this catenary assuming it's attached to the given owner entity.
     */
    public void renderDynamic(LivingEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(start.getPos(owner.level(), pTicks))
            .in(owner.level())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, catenary, Catenary.getLocalizedOffset(owner, pTicks), pTicks);
        CatenaryMesher.REUSABLE.reset();
    }

    /**
     * Renders this catenary assuming the LocalPlayer is in first person.
     */
    public void renderDynamicFirstPerson(LocalPlayer owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(start.getPos(owner.level(), pTicks))
            .in(owner.level())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, catenary, Catenary.getLocalizedOffset(owner, pTicks).subtract(0, owner.getBbHeight() * 0.9f, 0), pTicks);
        CatenaryMesher.REUSABLE.reset();
    }
}
