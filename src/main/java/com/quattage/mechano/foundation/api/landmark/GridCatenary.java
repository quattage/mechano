package com.quattage.mechano.foundation.api.landmark;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.Tension;
import com.quattage.mechano.foundation.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.model.CatenaryModel;
import com.quattage.mechano.foundation.item.SpoolItem;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent.SectionRenderingContext;

@OnlyIn(Dist.CLIENT)
public final class GridCatenary extends GridConnection {

    private final AnchorPoint start;
    private AnchorPoint end;
    private CatenaryModel<?> catenary;
    private AABB box = AABB.INFINITE;

    public static void renderToSection(LevelReader world, BlockPos sectionCenter, SectionPos section, SectionRenderingContext ctx, ObjectSet<GridCatenary> catenaries) {
        for(GridCatenary cat : catenaries) {
            if(!cat.hasPoints()) continue;
            AnchorPoint point = cat.getPrimaryRenderer(world);
            if(!point.getAddress().isInsideOf(world, section)) continue;
            Vec3 startPos = cat.start.getPos(world, 1f);
            cat.reinitializeModel(world, CatenaryAttributes.Initializer.FRESH_SIMULATION);
            CatenaryMesher.REUSABLE
                .at(startPos).in(world)
                .withAppearanceForChunkRendering(cat.trns.getType())
                .render(ctx, cat.getModel(), CatenaryModel.getLocalizedOffset(world, sectionCenter, point), 1f);
            Mechano.LOGGER.info("drew " + cat.getModel());
            CatenaryMesher.REUSABLE.reset();
        }
    }

    public GridCatenary(LevelReader world, AnchorPoint start, AnchorPoint end, TransmitterType<?> trns, @Nullable CatenaryAttributes.Initializer init) {
        super(trns.make());
        if(!world.isClientSide())
            throw new IllegalArgumentException("Can't instantiate a client-sided GridCatenary in a server-sided world!");
        if(start.getAddress().equals(end.getAddress()))
            throw new IllegalArgumentException("Can't instantiate a GridCatenary where both the start and end positions are the same!");
        this.start = start;
        this.end = end;
        if(init == null) init = CatenaryAttributes.Initializer.FRESH_SIMULATION;
        this.catenary = init.make(world, start, end, trns);
    }

    private GridCatenary(AnchorPoint start, AnchorPoint end, CatenaryModel<?> cat, Transmitter<?> trns) {
        super(trns);
        this.start = start; 
        this.end = end;
        this.catenary = cat;
        catenary.invertOffset();
        this.catenary.maxLength = trns.getType().getMaxLength();
    }

    public @Nullable AnchorPoint getPrimaryRenderer(LevelReader world) {
        GridUUID start = getStart();
        GridUUID end = getEnd();
        final boolean isStartVisible = start.isVisibleOnScreen(world);
        final boolean isEndVisible = end.isVisibleOnScreen(world);
        if(isStartVisible && !isEndVisible) return this.start;
        if(isEndVisible && !isStartVisible) return this.end;
        final boolean canStartMove = start.canMoveDynamically();
        final boolean canEndMove = end.canMoveDynamically();
        if(canStartMove && !canEndMove) return this.start;
        if(canEndMove && !canStartMove) return this.end;
        if(start.hashCode() > end.hashCode()) return this.start;
        return this.end;
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        throw new UnsupportedOperationException("Can't evaluate tracking status of client-sided GridCatenary on the server!");
    }

    @Override
    public void sendToClientsTracking(CustomPacketPayload packet) {
        throw new UnsupportedOperationException("Can't sync client-sided GridCatenaries from the server!");
    }

    public GridCatenary reinitializeModel(LevelReader world) {
        return reinitializeModel(world, CatenaryAttributes.Initializer.RESTING_SIMULATION);
    }

    public GridCatenary reinitializeModel(LevelReader world, CatenaryAttributes.Initializer init) {
        CatenaryModel.dispose(this.catenary);
        this.catenary = init.make(world, start, end, trns.getType());
        return this;
    }

    public GridCatenary bakeModel(LevelReader world) {
        if(catenary == null || !catenary.isInitialized()) {
            reinitializeModel(world);
            Mechano.LOGGER.warn("Catenary (" + start.getAddress().toString(world) + " -> " + end.getAddress().toString(world) 
                + ") was baked without an original state and the default initializer was used as a fallback.");
        }
        if(!catenary.isResting()) {
            Mechano.LOGGER.warn("Catenary (" + start.getAddress().toString(world) + " -> " 
                + end.getAddress().toString(world) + ") was baked before it reached a state of restitution.");
        }
        this.catenary = catenary.bake();
        return this;
    }

    public GridCatenary unbakeModel(LevelReader world) {
        if(catenary == null || !catenary.isInitialized()) {
            reinitializeModel(world);
            Mechano.LOGGER.warn("Catenary (" + start.getAddress().toString(world) + " -> " + end.getAddress().toString(world) 
                + ") was unbaked without an original state and the default initializer was used as a fallback.");
            return this;
        }
        if(catenary.isMovable()) {
            Mechano.LOGGER.warn("Catenary (" + start.getAddress().toString(world) + " -> " + end.getAddress().toString(world) 
                + ") was unbaked from a previously unbaked state - no change was made.");
            return this;
        }
        this.catenary = catenary.toSimulated(true);
        return this;
    }

    @Override
    public GridCatenary inverseCopy() {
        return new GridCatenary(end, start, catenary, trns);
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
        return "GridCatenary(" + trns.getType() + ")";
    }

    public boolean isMoving() {
        return catenary != null 
            && (catenary.isMovable() && !catenary.isResting()) 
            && (start.getAddress().canMoveDynamically() || !end.getAddress().canMoveDynamically());
    }

    @Override
    public Tension getTension() {
        return catenary.getTension();
    }

    @Override
    public boolean setTension(Tension tension) {
        if(!catenary.isMovable()) {
            Mechano.LOGGER.warn("Catenary '" + catenary + "' is static and can't have its tension adjusted.");
            return false;
        }
        return catenary.setTension(tension);
    }

    @Override
    public boolean resetTension() {
        if(!catenary.isMovable()) {
            Mechano.LOGGER.warn("Catenary '" + catenary + "' is static and can't have its tension reset");
            return false;
        }
        Tension defaultTension = trns.getType().defaults.getTension();
        if(catenary.tension == defaultTension) return false;
        this.catenary.tension = defaultTension;
        return true;
    }

    @Override
    public float getLength() {
        return catenary.length;
    }

    @Override
    public float getMaxLength() {
        return trns.getType().getMaxLength();
    }

    /**
     * Changes the offset of this catenary to match its current position
     * in the world.
     */
    @Override
    public void updateShape(LevelReader world, float pTicks) {
        if(!catenary.isMovable()) {
            Mechano.LOGGER.warn("Catenary (" + start.getAddress().toString(world) + " -> " + end.getAddress().toString(world) 
                + ") is static and can't be moved.");
            return;
        }
        catenary.setOffset(start.getPos(world, pTicks)  , end.getPos(world, pTicks));
        catenary.update();
    }

    /**
     * Imparts forces upon attached entities
     * according to this catenary's tension
     * @param world World to use as a basis for acquiring additional information about both ends of this catenary
     */
    public void updateKinematics(LevelReader world) {
        if(!hasPoints()) {
            Mechano.LOGGER.warn("Catenary (" + start.getAddress().toString(world) + " -> " + end.getAddress().toString(world) 
                + ") has no valid points and cannot apply external forces.");
            return;
        }
        if(!catenary.isMovable()) {
            Mechano.LOGGER.warn("Catenary (" + start.getAddress().toString(world) + " -> " + end.getAddress().toString(world) 
                + ") is static and can't apply external forces.");
            return;
        }
        Griddable<?> points = getEnd().getAnchorPoints(world);
        if(points == null) return;
        if(points.isLoose()) {
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
            0.1f * Math.min(1f, (catenary.length - softLength) / (catenary.maxLength - softLength))
        ));
    }

    public void adjustMaxLength(ItemStack stack) {
        if(!catenary.isMovable()) {
            Mechano.LOGGER.warn("Catenary '" + catenary + "' is static and can't have its length adjusted.");
            return;
        }
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
        if(!catenary.isMovable()) {
            Mechano.LOGGER.warn("Catenary (" + start.getAddress().toString(world) + " -> " + end.getAddress().toString(world) 
                + ") is static and can't have its endpoint rebound.");
            return this;
        }
        if(this.end.getAddress().equals(newEnd.getAddress())) {
            Mechano.LOGGER.warn("Attempted to rebind endpoint of Catenary (" + start.getAddress().toString(world) + " -> " + end.getAddress().toString(world) 
                + ") to itself.");
            return this;
        }
        reassertAndDo(world, () -> {
            this.end = newEnd;
            updateShape(world, 1);
        });
        return this;
    }

    public CatenaryModel<?> getModel() {
        return this.catenary;
    }

    @Override
    public boolean hasPoints() {
        return super.hasPoints() && getModel() != null && getModel().isInitialized();
    }

    /**
     * Renders this catenary assuming it's attached to the given owner entity.
     */
    public void renderDynamic(LivingEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(start.getPos(owner.level(), pTicks))
            .in(owner.level())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, catenary, CatenaryModel.getLocalizedOffset(owner, pTicks), pTicks);
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
            .render(buffers, matrixStack, catenary, CatenaryModel.getLocalizedOffset(owner, pTicks).subtract(0, owner.getBbHeight() * 0.9f, 0), pTicks);
        CatenaryMesher.REUSABLE.reset();
    }
}
