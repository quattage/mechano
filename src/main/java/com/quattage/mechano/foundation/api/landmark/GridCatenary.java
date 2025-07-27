package com.quattage.mechano.foundation.api.landmark;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.VoxelUUID;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.catenary.model.CatenaryModel;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    public static void renderToSection(LevelReader world, SectionPos sectionPos, BlockPos sectionOrigin, ObjectSet<GridCatenary> catenaries, SectionRenderingContext context) {
        synchronized(catenaries) {
            CatenaryMesher mesher = CatenaryMesher.asEmpty();
            for(GridCatenary cat : catenaries) {
                if(cat == null || !cat.hasPoints() || cat.canMoveDynamically()) continue;
                AnchorPoint point = cat.getPrimaryRenderer(null);
                if(point == null || !point.getAddress().isInsideOf(world, sectionPos)) continue;
                Vec3 startPos = point.getAddress().getPos(world, 1f);
                cat.accumulateModel(world);
                mesher.withAppearanceForChunkRendering(cat.getTransmitter().getType())
                    .at(startPos).in(context.getRegion())
                    .render(context, cat.getOrCreateModel(world), CatenaryAccessor.getLocalizedOffset(world, sectionOrigin, point), 1f);
                mesher.reset();
            }
        }
    }

    /**
     * Enforces a deterministic (if somewhat arbitrary) renderer
     * priority between two {@link AnchorPoint AnchorPoints}. 
     * This method is used to decide which end of a {@link GridCatenary}
     * should take render priority when drawing catenary meshes.
     * @param world World to use to check visibility status. If <code>null</code>,
     * frustum culling is ignored.
     * @return The {@link AnchorPoint} that takes priority over the 
     * other out of the two provided
     */
    public static AnchorPoint[] orderedByRenderPriority(@Nullable LevelReader world, AnchorPoint start, AnchorPoint end) {
        AnchorPoint[] out = new AnchorPoint[2];

        final boolean canStartMove = start.getAddress().canMoveDynamically();
        final boolean canEndMove = end.getAddress().canMoveDynamically();
        if(canStartMove && !canEndMove) {
            out[0] = start;
            out[1] = end;
            return out;
        }
        if(canEndMove && !canStartMove) {
            out[0] = end;
            out[1] = start;
            return out;
        }

        if(world != null) {
            final boolean isStartVisible = start.getAddress().isVisibleOnScreen(world);
            final boolean isEndVisible = end.getAddress().isVisibleOnScreen(world);
            if(isStartVisible && !isEndVisible) {
                out[0] = start;
                out[1] = end;
                return out;
            }
            if(isEndVisible && !isStartVisible) {
                out[0] = end;
                out[1] = start;
                return out;
            }
        }
        /**
         * this edge case handling is here as a "temporary" measure to ensure
         * that we don't try to inject dynamic wire geometry to the level chunk,
         * since that would be stupid.
         */
        if(!(start.getAddress() instanceof VoxelUUID) && (end.getAddress() instanceof VoxelUUID)) {
            out[0] = start;
            out[1] = end;
            return out;
        }
        if((start.getAddress() instanceof VoxelUUID) && !(end.getAddress() instanceof VoxelUUID)) {
            out[0] = end;
            out[1] = start;
            return out;
        }
        if(start.getAddress() instanceof VoxelUUID && end.getAddress() instanceof VoxelUUID) {
            if(start.hashCode() > end.hashCode()) {
                out[0] = start;
                out[1] = end;
                return out;
            }
            out[0] = end;
            out[1] = start;
            return out;
        }
        out[0] = start;
        out[1] = end;
        return out;
    }

    public GridCatenary(LevelReader world, AnchorPoint start, AnchorPoint end, TransmitterType<?> trns) {
        super(trns.make());
        if(!world.isClientSide())
            throw new IllegalArgumentException("Can't instantiate a client-sided GridCatenary in a server-sided world!");
        if(start.getAddress().equals(end.getAddress()))
            throw new IllegalArgumentException("Can't instantiate a GridCatenary where both the start and end positions are the same!");
        this.start = start;
        this.end = end;
        this.start.makeLocallyDynamic();
        this.end.makeLocallyDynamic();
    }

    private GridCatenary(AnchorPoint start, AnchorPoint end, @Nullable CatenaryModel<?> cat, Transmitter<?> trns) {
        super(trns);
        this.start = start; 
        this.end = end;
        this.catenary = cat;
        if(catenary != null)
            this.catenary.maxLength = trns.getType().getMaximumSpan();
    }

    public AnchorPoint getPrimaryRenderer(@Nullable LevelReader world) {
        return orderedByRenderPriority(world, start, end)[0];
    }

    /**
     * Imparts forces upon attached entities
     * according to this catenary's tension
     * @param world World to use as a basis for acquiring additional information about both ends of this catenary
     */
    public void updateKinematics(LevelReader world) {
        if(!canMoveDynamically()) {
            Mechano.LOGGER.warn("Attempted invalid kinematic update for non-dynamic " + this);
            return;
        }
        Griddable<?> points = getEnd().getAnchorPoints(world);
        if(points == null) return;
        if(points.isMovable()) {
            Vec3 reelDir = start.getPos(world).subtract(end.getPos(world)).normalize();
            float dirDot = (float)getEnd().getAttachmentVelocity(world).normalize().dot(reelDir);
            if(dirDot < 0) {
                getEnd().setAttachmentVelocity(world, Vec3.ZERO);
                LinkDataStorable.remove(world, this);
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
        float softLength = getOrCreateModel(world).maxLength * CatenaryAttributes.KINEMATIC_SOFT;
        if(diff.length() < softLength) return;
        ordered[1].applyForceToAttachment(world, diff.normalize().scale(
            0.1f * Math.min(1f, (getOrCreateModel(world).length - softLength) / (getOrCreateModel(world).maxLength - softLength))
        ));
    }

    /**
     * Marks start/end points as dynamic if possible and 
     * un-bakes this wire. This method will broadcast chunk updates
     * if this wire was previously baked to a chunk.
     * For the opposite operation, see {@link #startMoving}
     * @param world
     */
    public void startMoving(ClientLevel world) {
        if(canMoveDynamically()) {
            Mechano.LOGGER.warn("Skipped an attempt to unfreeze " + this + " - this catenary is already dynamic, so this method call is redundant.");
            return;
        }
        if(catenary == null || !catenary.isInitialized())
            reinitializeModel(world, CatenaryAttributes.Initializer.FRESH_SIMULATION);
        LinkDataStorable.remove(world, this);
        sendLevelUpdates(world);
        this.start.makeLocallyDynamic();
        this.end.makeLocallyDynamic();
        LinkDataStorable.put(world, this);
    }

    /**
     * Marks start/end points as static if possible and 
     * un-bakes this wire. This method will broadcast chunk updates
     * if this wire was previously baked to a chunk.
     * For the opposite operation, see {@link #startMoving}.
     * <h2>Important note:</h2> This method will freeze this catenary's mesh
     * and simulation regardless of the mobility state of its start and end points.
     * If this {@link GridCatenary} has movable {@link AnchorPoint AnchorPoints},
     * this method will result in visual issues as the wire would freeze in place
     * but remain attached to a potentially moving renderer.
     * @param world
     */
    public void freezeInPlace(ClientLevel world) {
        if(!canMoveDynamically()) {
            Mechano.LOGGER.warn("Skipped an attempt to freeze " + this + " - this catenary is already frozen, so this method call is redundant.");
            return;
        }
        if(catenary == null || !catenary.isInitialized())
            reinitializeModel(world, CatenaryAttributes.Initializer.RESTING_SIMULATION);
        LinkDataStorable.remove(world, this);
        this.start.getAddress().setDataScope(DataScope.STATIC_CHUNK);
        this.end.getAddress().setDataScope(DataScope.STATIC_CHUNK);
        LinkDataStorable.put(world, this);
        sendLevelUpdates(world);
    }

    /**
     * 
     * @param world world to operate within
     * @param pTicks partial ticks, accessible in most rendering contexts. If you're unsure, just pass as this parameter.
     */
    @Override
    public void updateShape(LevelReader world, float pTicks) {
        if(canMoveDynamically())
            getOrCreateModel(world).setOffset(start.getPos(world, pTicks), end.getPos(world, pTicks));
        getOrCreateModel(world).update();
    }

    public CatenaryModel<?> getOrCreateModel(LevelReader world) {
        if(catenary != null) return catenary;
        catenary = CatenaryAttributes.Initializer.FRESH_SIMULATION_EXPRESSIVE.make(world, start, end, trns.getType());
        return catenary;
    }

    public CatenaryModel<?> accumulateModel(LevelReader world) {
        catenary = CatenaryAttributes.Initializer.RESTING_SIMULATION.make(world, start, end, trns.getType());
        return catenary;
    }

    public GridCatenary reinitializeModel(LevelReader world, CatenaryAttributes.Initializer init) {
        CatenaryModel.dispose(this.catenary);
        this.catenary = init.make(world, start, end, trns.getType());
        return this;
    }

    public CatenaryModel<?> getModel() {
        return catenary;
    }

    @Override
    public boolean canMoveDynamically() {
        if(!hasPoints() || catenary == null) return false;
        return this.start.getAddress().canMoveDynamically() && this.end.getAddress().canMoveDynamically();
    }

    public boolean isMoving() {
        if(catenary == null)
            return canMoveDynamically();
        return !catenary.isResting();
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

    @Override
    public float getSpan() {
        return catenary == null ? -1 : catenary.length;
    }

    @Override
    public float getMaximumSpan() {
        return trns == null ? 32 : trns.getType().getMaximumSpan();
    }

    public AnchorPoint getStartAnchor() {
        return start;
    }

    public AnchorPoint getEndAnchor() {
        return end;
    }

    @Override
    public void adjustSpan(LevelReader world, float length) {
        getOrCreateModel(world);
        if(!canMoveDynamically()) startMoving((ClientLevel)world);
        catenary.adjustSpan(world, Math.max(0.25f, Math.min(length, trns.getType().getMaximumSpan())));
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        throw new UnsupportedOperationException("Tracking status can only be evaluated on the Server - GridCatenaries do not have access to this information!");
    }

    @Override
    public void sendToClientsTracking(CustomPacketPayload packet) {
        throw new UnsupportedOperationException("Client-sided GridCatenaries can't send packets! This is a server-only feature!");
    }

    @Override
    public String describeDataScope(LevelReader world) {
        return getStart().describeDataScope(world) + "(Queried by GridCatenary)";
    }

    @Override
    public DataScope getDataScope() {
        return getStart().getDataScope();
    }

    @Override
    public void setDataScope(DataScope scope) {
        if(getStart() != null) getStart().setDataScope(scope);
        if(getEnd() != null) getEnd().setDataScope(scope);
    }

    /**
     * Updates the simulated shape of this GridCatenary on
     * a fixed update cycle. Expected to be called by a scheduled
     * ticking method, such as Entity or BlockEntity tick.
     * @param holder
     * @param world
     */
    public void updateShapeFixed(Griddable<?> holder, LevelReader world) {
        if(!hasPoints() || !holder.containsAnchor(getPrimaryRenderer(null))) return;
        CatenaryModel<?> model = getOrCreateModel(world);
        if(!isMoving()) return;
        model.setOffset(start.getPos(world, 1), end.getPos(world, 1)).update();
    }

    // TODO REUSABLE MESHER MEANS THAT THESE METHODS ARE NOT THREAD SAFE
    /**
     * Renders this catenary assuming it's attached to the given owner entity.
     */
    public void renderDynamic(LivingEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(start.getPos(owner.level(), pTicks))
            .in(owner.level())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, getOrCreateModel(owner.level()), CatenaryAccessor.getLocalizedOffset(owner, pTicks), pTicks);
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
            .render(buffers, matrixStack, getOrCreateModel(owner.level()), CatenaryAccessor.getLocalizedOffset(owner, pTicks).subtract(0, owner.getBbHeight() * 0.9f, 0), pTicks);
        CatenaryMesher.REUSABLE.reset();
    }

    /**
     * Renders this catenary assuming the starting point is attached to the given BlockEntity
     */
    public void renderDynamic(BlockEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(start.getPos(owner.getLevel(), pTicks))
            .in(owner.getLevel())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, getOrCreateModel(owner.getLevel()), start.getOffset(), pTicks);
        CatenaryMesher.REUSABLE.reset();
    }

    @Override
    public String toString() {
        return "GridCatenary[" + (start == null ? "NULL" : start.getAddress()) + " -> " + (end == null ? "NULL" : end.getAddress()) + "]";
    }
}
