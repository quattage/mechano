package com.quattage.mechano.foundation.gridapi.landmark;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.gridapi.Griddable;
import com.quattage.mechano.foundation.gridapi.LinkDataStorable;
import com.quattage.mechano.foundation.gridapi.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.gridapi.anchor.AnchorPoint;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.gridapi.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.gridapi.catenary.WindManager;
import com.quattage.mechano.foundation.gridapi.catenary.model.CatenaryModel;
import com.quattage.mechano.foundation.gridapi.catenary.model.SimulatedCatenary;
import com.quattage.mechano.foundation.gridapi.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.gridapi.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.gridapi.transmitter.Transmitter;
import com.quattage.mechano.foundation.gridapi.transmitter.TransmitterRegistry.TransmitterType;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent.SectionRenderingContext;

@OnlyIn(Dist.CLIENT)
public final class GridCatenary extends GridConnection {

    private final AnchorPoint start;
    private AnchorPoint end;
    private @Nullable CatenaryModel<?> catenary;
    private AABB box = AABB.INFINITE;

    public static void renderToSection(LevelReader world, SectionPos sectionPos, BlockPos sectionOrigin, ObjectSet<GridCatenary> catenaries, SectionRenderingContext context) {
        synchronized(catenaries) {
            CatenaryMesher mesher = CatenaryMesher.asEmpty();
            for(GridCatenary cat : catenaries) {
                if(cat == null || !cat.hasPoints() || cat.canMoveDynamically(world)) continue;
                AnchorPoint point = (AnchorPoint)cat.getPrimaryConstruct(world);
                Vec3 startPos = point.getAddress().getPos(world, 1f);
                cat.accumulateModel(world);
                mesher.withAppearanceForChunkRendering(cat.getTransmitter().getType())
                    .at(startPos).in(context.getRegion())
                    .render(context, cat.getOrCreateModel(world), CatenaryAccessor.getLocalizedOffset(world, sectionOrigin, point), 1f);
                mesher.reset();
            }
        }
    }

    public GridCatenary(LevelReader world, AnchorPoint start, AnchorPoint end, TransmitterType<?> trns) {
        super(trns.make());
        if(!world.isClientSide())
            throw new IllegalArgumentException("Can't instantiate a client-sided GridCatenary in a server-sided world!");
        if(start.getAddress().equals(end.getAddress()))
            throw new IllegalArgumentException("Can't instantiate a GridCatenary where both the start and end positions are the same!");
        this.start = start;
        this.end = end;
        this.start.makeLocallyDynamic(world);
        this.end.makeLocallyDynamic(world);
    }

    private GridCatenary(AnchorPoint start, AnchorPoint end, @Nullable CatenaryModel<?> cat, Transmitter<?> trns) {
        super(trns);
        this.start = start; 
        this.end = end;
        this.catenary = cat;
        if(catenary != null)
            this.catenary.maxLength = trns.getType().getMaximumSpan();
    }

    /**
     * Imparts forces upon attached entities
     * according to this catenary's tension
     * @param world World to use as a basis for acquiring additional information about both ends of this catenary
     */
    public void updateKinematics(LevelReader world) {
        if(!canMoveDynamically(world)) {
            Mechano.LOGGER.warn("Attempted invalid kinematic update for non-dynamic " + this);
            return;
        }
        Griddable<?> points = getEnd().getOrFindGriddable(world);
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

        if(catenary == null) return;
        GridUUID[] ordered = orderedByWeight(world);
        if(!ordered[1].canMoveDynamically(world)) return;
        Vec3 diff = ordered[0].getPos(world).subtract(ordered[1].getPos(world));
        float softLength = catenary.maxLength * CatenaryAttributes.KINEMATIC_SOFT;
        if(diff.length() < softLength) return;
        ordered[1].applyForceToAttachment(world, diff.normalize().scale(
            0.1f * Math.min(1f, (catenary.length - softLength) / (catenary.maxLength - softLength))
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
        if(canMoveDynamically(world)) {
            Mechano.LOGGER.warn("Skipped an attempt to unfreeze " + this + " - this catenary is already dynamic, so this method call is redundant.");
            return;
        }
        if(catenary == null || !catenary.isInitialized())
            reinitializeModel(world, CatenaryAttributes.Initializer.FRESH_SIMULATION);
        LinkDataStorable.remove(world, this);
        sendLevelUpdates(world);
        this.start.makeLocallyDynamic(world);
        this.end.makeLocallyDynamic(world);
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
        if(!canMoveDynamically(world)) {
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

    @Override
    public void updateShape(LevelReader world, float pTicks) {
        if(canMoveDynamically(world))
            setOrderedOffset(world, getOrCreateModel(world), pTicks);
        getOrCreateModel(world).update();
    }

    /**
     * Updates the simulated shape of this GridCatenary on
     * a fixed update cycle. Expected to be called by a scheduled
     * ticking method, such as Entity or BlockEntity tick 
     * (NOT a renderer). This method comes with some extra stuff, 
     * like wind :)
     * @param holder
     * @param world
     */
    public void updateShapeFixed(ClientLevel world, Griddable<?> holder) {
        CatenaryModel<?> model = getOrCreateModel(world);
        if(!isMoving(world)) return;
        setOrderedOffset(world, model, 1);
        model.update(1);
        if(!WindManager.INSTANCE.isEnabled()) return;
        if(!(model instanceof SimulatedCatenary scat)) return;
        scat.applyWind(WindManager.INSTANCE.sample(world, getMiddle(world)));
    }

    public void setOrderedOffset(LevelReader world, CatenaryModel<?> model, float pTicks) {
        TrackedStreamable[] ordered = TrackedStreamable.orderedByAssertionPriority(world, start, end);
        model.setOffset(((AnchorPoint)ordered[0]).getPos(world, pTicks), ((AnchorPoint)ordered[1]).getPos(world, pTicks));
    }

    public CatenaryModel<?> getOrCreateModel(LevelReader world) {
        if(catenary != null) return catenary;
        TrackedStreamable[] ordered = TrackedStreamable.orderedByAssertionPriority(world, start, end);
        this.catenary = CatenaryAttributes.Initializer.FRESH_SIMULATION_EXPRESSIVE
            .make(world, (AnchorPoint)ordered[0], (AnchorPoint)ordered[1], trns.getType());
        GridCatenary opposite = LinkDataStorable.getAsClient(world, this);
        if(opposite != null) opposite.catenary = this.catenary;
        return this.catenary;
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

    public CatenaryModel<?> getModel(LevelReader world) {
        if(catenary != null) return catenary;
        GridCatenary opposite = LinkDataStorable.getAsClient(world, this);
        this.catenary = opposite.catenary;
        return catenary;
    }

    @Override
    public boolean canMoveDynamically(LevelReader world) {
        if(!hasPoints() || catenary == null) return false;
        return this.start.getAddress().canMoveDynamically(world) && this.end.getAddress().canMoveDynamically(world);
    }

    public boolean isMoving(LevelReader world) {
        if(catenary == null)
            return canMoveDynamically(world);
        if(!catenary.isResting()) {
            if(catenary instanceof SimulatedCatenary scat) 
                scat.applyWind(null);
        };
        return true;
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

    public String describeCatenary() {
        if(catenary == null) return "Catenary uninitialized, " + getSpan() + "m";
        return (catenary.isResting() ? "Catenary at rest" : "Catenary simulating") + ", [" + catenary.length + "/" + catenary.maxLength + "m], type: " + trns.getType().toString();
    }

    @Override
    public void adjustSpan(LevelReader world, float length) {
        getOrCreateModel(world);
        if(!canMoveDynamically(world)) startMoving((ClientLevel)world);
        catenary.adjustSpan(world, Math.max(0.25f, Math.min(length, trns.getType().getMaximumSpan())));
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        throw new UnsupportedOperationException("Tracking status can only be evaluated on the Server - GridCatenaries do not have access to this information!");
    }

    @Override
    public void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet) {
        throw new UnsupportedOperationException("Client-sided GridCatenaries can't send packets! This is a server-only feature!");
    }

    @Override
    public String describeDataScope(LevelReader world) {
        return getPrimaryConstruct(world).describeDataScope(world) + " (Queried by GridCatenary)";
    }

    @Override
    public DataScope getDataScope(LevelReader world) {
        return getPrimaryConstruct(world).getDataScope(world);
    }

    @Override
    public IAttachmentHolder getDataStorageHolder(LevelReader world) {
        return getPrimaryConstruct(world).getDataStorageHolder(world);
    }

    public BlockPos getMiddle(LevelReader world) {
        if(!hasPoints()) return BlockPos.ZERO;
        BlockPos startPos = start.getAddress().getBlockPos(world);
        BlockPos endPos = end.getAddress().getBlockPos(world);
        return new BlockPos(
            (int)((startPos.getX() + endPos.getX()) / 2f),
            (int)((startPos.getY() + endPos.getY()) / 2f),
            (int)((startPos.getZ() + endPos.getZ()) / 2f)
        );
    }

    @Override
    public void setDataScope(DataScope scope) {
        if(getStart() != null) getStart().setDataScope(scope);
        if(getEnd() != null) getEnd().setDataScope(scope);
    }

    public void replaceAddresses(LevelReader world, GridUUID startAddress, GridUUID endAddress) {

        LinkDataStorable.popAsClient(world, this);
        if(this.start != null) this.start.replaceAddress(startAddress);
        if(this.end != null) this.end.replaceAddress(endAddress);

        DataScope startScope = this.start.getDataScope(world);
        DataScope endScope = this.end.getDataScope(world);
        if(startScope == DataScope.STATIC_CHUNK && endScope != DataScope.STATIC_CHUNK)
            this.start.setDataScope(DataScope.BLOCKENTITY);
        if(startScope != DataScope.STATIC_CHUNK && endScope == DataScope.STATIC_CHUNK)
            this.end.setDataScope(DataScope.BLOCKENTITY);

        LinkDataStorable.put(world, this);
    }

    /**
     * Renders this catenary to the provided renderer features.
     * 
     * <h2>These methods are not thread safe!</h2>
     * @param owner
     * @param offset
     * @param buffers
     * @param matrixStack
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void renderDynamic(LivingEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(((GridUUID)getPrimaryConstruct(owner.level())).getPos(owner.level(), pTicks))
            .in(owner.level())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, getOrCreateModel(owner.level()), CatenaryAccessor.getLocalizedOffset(owner, pTicks), pTicks);
        CatenaryMesher.REUSABLE.reset();
    }

    /**
     * Renders this catenary to the provided renderer features.
     * 
     * <h2>This method is not thread safe!</h2>
     * The meshing process implemented here utilizes the {@link CatenaryMesher#REUSABLE reusable mesher},
     * this mesher has vertices added and removed from it during its lifespan, so any timing issues
     * that result in bad vertex ordering will cause huge geometry artifacting. 
     * @param owner The owner of this wire, obtainable as the {@link TrackedStreamable#orderedByAssertionPriority(LevelReader, TrackedStreamable, TrackedStreamable) prioritized construct}
     * @param offset 
     * @param buffers
     * @param matrixStack
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void renderDynamic(LivingEntity owner, Vec3 offset, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(((GridUUID)getPrimaryConstruct(owner.level())).getPos(owner.level(), pTicks))
            .in(owner.level())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, getOrCreateModel(owner.level()), CatenaryAccessor.getLocalizedOffset(owner, pTicks).subtract(offset), pTicks);
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
