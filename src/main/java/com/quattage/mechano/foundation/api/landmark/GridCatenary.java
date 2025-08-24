package com.quattage.mechano.foundation.api.landmark;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.api.catenary.CatenaryMesher;
import com.quattage.mechano.foundation.api.catenary.WindManager;
import com.quattage.mechano.foundation.api.catenary.model.CatenaryModel;
import com.quattage.mechano.foundation.api.catenary.model.SimulatedCatenary;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry.TransmitterType;

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

    @Override
    public GridCatenary inverseCopy() {
        return new GridCatenary(end, start, catenary, trns);
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

    public CatenaryModel<?> getOrCreateModel(LevelReader world) {
        if(catenary != null) return this.catenary;
        GridCatenary lookup = LinkDataStorable.getAsClient(world, this);
        if(lookup != null) {
            if(lookup.catenary != null) this.catenary = lookup.catenary;
            else {
                this.catenary = CatenaryAttributes.Initializer.FRESH_SIMULATION_EXPRESSIVE
                    .make(world, start, end, trns.getType());
                lookup.catenary = this.catenary;
            }
        }
        return this.catenary;
    }

    public GridCatenary reinitializeModel(LevelReader world, CatenaryAttributes.Initializer init) {
        CatenaryModel.disposeOf(this.catenary);
        this.catenary = init.make(world, start, end, trns.getType());
        return this;
    }

    public CatenaryModel<?> accumulateModel(LevelReader world) {
        catenary = CatenaryAttributes.Initializer.RESTING_SIMULATION.make(world, start, end, trns.getType());
        return catenary;
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
    public void updateShapeFixed(LevelReader world) {
        if(!(world instanceof ClientLevel cl)) return;
        CatenaryModel<?> model = getOrCreateModel(world);
        if(!isMoving(world)) return;
        model.setOrderedOffset(world, start, end, 1);
        model.update();
        if(WindManager.INSTANCE.isEnabled() && model instanceof SimulatedCatenary scat)
            scat.applyWind(WindManager.INSTANCE.sample(cl, getMiddle(world)));
    }

    @Override
    public boolean canMoveDynamically(LevelReader world) {
        if(!hasPoints() || catenary == null) return false;
        return this.start.getAddress().canMoveDynamically(world) && this.end.getAddress().canMoveDynamically(world);
    }

    public boolean isMoving(LevelReader world) {
        if(catenary == null) return canMoveDynamically(world);
        return !catenary.isResting();
    }

    @Override
    public void adjustSpan(LevelReader world, float length) {
        getOrCreateModel(world);
        if(!canMoveDynamically(world)) startMoving((ClientLevel)world);
        catenary.adjustSpan(world, Math.max(0.25f, Math.min(length, trns.getType().getMaximumSpan())));
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

    public Vec3 getMiddlePos(LevelReader world) {
        if(!hasPoints()) return Vec3.ZERO;
        Vec3 startPos = start.getPos(world);
        Vec3 endPos = end.getPos(world);
        return new Vec3((startPos.x + endPos.x) / 2d, (startPos.y + endPos.y) / 2d, (startPos.z + endPos.z) / 2d);
    }

    @Override
    public void setDataScope(DataScope scope) {
        if(getStart() != null) getStart().setDataScope(scope);
        if(getEnd() != null) getEnd().setDataScope(scope);
    }

    public void replaceAddresses(ClientLevel world, GridUUID startAddress, GridUUID endAddress, @Nullable DataScope forcedScope) {
        if(!hasPoints()) throw new IllegalStateException("Couldn't replace addresses for a GridCatenary with no points!");
        LinkDataStorable.popAsClient(world, this);
        this.start.replaceAddress(startAddress);
        this.end.replaceAddress(endAddress);
        if(forcedScope == null)
            correctDataScopes(world);
        else setDataScope(forcedScope);
        LinkDataStorable.put(world, this);
    }

    /**
     * Renders this catenary to the provided renderer feature as an extension of a LivingEntity's geometry
     * <h2>This method is not thread safe!</h2>
     * The meshing process implemented here utilizes the {@link CatenaryMesher#REUSABLE reusable mesher},
     * this mesher has vertices added and removed from it during its lifespan, so any timing issues
     * (caused by concurrent access) will result in bad vertex ordering and geometry artifacts.
     * @param owner 
     * @param buffers
     * @param matrixStack
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void renderDynamic(LivingEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(getMiddlePos(owner.level()))
            .in(owner.level())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, getOrCreateModel(owner.level()), CatenaryAccessor.getLocalizedOffset(owner, pTicks), pTicks);
        CatenaryMesher.REUSABLE.reset();
    }

    /**
     * Renders this catenary to the provided renderer feature as an extension of a LivingEntity's geometry
     * <h2>This method is not thread safe!</h2>
     * The meshing process implemented here utilizes the {@link CatenaryMesher#REUSABLE reusable mesher},
     * this mesher has vertices added and removed from it during its lifespan, so any timing issues
     * (caused by concurrent access) will result in bad vertex ordering and geometry artifacts.
     * @param owner The owner of this catenary, used for acquiring a local offset vector
     * @param offset An optional, additional offset vector to apply when rendering this wire
     * @param buffers BufferSource to push vertices to,
     * @param matrixStack PoseStach pertaining to the relevent rendering context.
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void renderDynamic(LivingEntity owner, Vec3 offset, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        CatenaryMesher.REUSABLE
            .at(getMiddlePos(owner.level()))
            .in(owner.level())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, getOrCreateModel(owner.level()), CatenaryAccessor.getLocalizedOffset(owner, pTicks).subtract(offset), pTicks);
        CatenaryMesher.REUSABLE.reset();
    }

    /**
     * Renders this catenary to to the provided renderer feature while applying
     * the proper AnchorPoint offset with the given BlockEntity.
     * This method is designed specifically to be invoked in BlockEntityRenderer contexts.
     * @param owner The owner of this catenary, used for acquiring a local offset vector from an AnchorPoint.
     * @param buffers BufferSource to push vertices to,
     * @param matrixStack PoseStach pertaining to the relevent rendering context.
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void renderDynamic(BlockEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        TrackedStreamable[] ordered = TrackedStreamable.orderedByAssertionPriority(owner.getLevel(), start, end);
        if(!(ordered[0] instanceof AnchorPoint ap1) || !(ordered[1] instanceof AnchorPoint ap2)) return;
        Vec3 startPos = ap1.getPos(owner.getLevel(), pTicks);
        Vec3 endPos = ap2.getPos(owner.getLevel(), pTicks);
        CatenaryMesher.REUSABLE
            .at(startPos.add(endPos).scale(0.5f))
            .in(owner.getLevel())
            .withAppearance(trns.getType())
            .render(buffers, matrixStack, getOrCreateModel(owner.getLevel()), 
                ap1.getOffset().subtract(startPos.subtract(endPos).scale(0.5f)), pTicks);
        CatenaryMesher.REUSABLE.reset();
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
    public GridUUID getStart() {
        return start == null ? null : start.getAddress();
    }

    @Override
    public GridUUID getEnd() {
        return start == null ? null : end.getAddress();
    }
    
    @Override
    public TrackedStreamable getPrimaryConstruct(LevelReader world) {
        return TrackedStreamable.orderedByAssertionPriority(world, start, end)[0];
    }

    @Override
    public boolean isClientSide() {
        return true;
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        throw new UnsupportedOperationException("Client-sided GridCatenaries cannot be serialized!");
    }

    public String describeCatenary() {
        if(catenary == null) return "Catenary uninitialized, " + getSpan() + "m";
        return (catenary.isResting() ? "Catenary at rest" : "Catenary simulating") + ", [" + catenary.length + "/" + catenary.maxLength + "m], type: " + trns.getType().toString();
    }

    @Override
    public String describeConnectionType() {
        return "GridCatenary(" + trns.getType() + ")";
    }

    @Override
    public String toString() {
        return "GridCatenary[" + (start == null ? "NULL" : start.getAddress()) + " -> " + (end == null ? "NULL" : end.getAddress()) + "]";
    }
}
