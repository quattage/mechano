package com.quattage.mechano.foundation.api.landmark;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.quattage.mechano.foundation.api.ClientGrid;
import com.quattage.mechano.foundation.api.LinkDataStorable;
import com.quattage.mechano.foundation.api.LinkDataStorable.DataScope;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccessor;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.api.catenary.CatenaryMeshBuffer;
import com.quattage.mechano.foundation.api.catenary.CatenaryModel;
import com.quattage.mechano.foundation.api.catenary.SimulatedCatenary;
import com.quattage.mechano.foundation.api.catenary.WindManager;
import com.quattage.mechano.foundation.api.landmark.identifier.ContraptionUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.AwaitingLinkBuffer.ProcessMode;
import com.quattage.mechano.foundation.api.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.helper.Duo;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;

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

    /**
     * Searches multiple times through the {@link LinkDataStorable link data store} 
     * to find a {@link GridCatenary} matching the given {@link GridUUID UUIDs}.
     * Calls to this method will adjust the {@link GridConnection#getDataScope data scope}
     * of the IDs passed if no match was found, and try again.
     * @param world world to operate within
     * @param start
     * @param end
     * @return The {@link GridCatenary} located between <code>start</code> and <code>end</code>, or <code>null</code>.
     */
    public static @Nullable GridCatenary findLoosely(ClientLevel world, GridUUID start, GridUUID end) {
        ConnectionKey key = new ConnectionKey(start, end);
        GridCatenary cat = LinkDataStorable.getAsClient(world, key, true);
        if(cat != null && cat.hasPoints()) return cat;
        key.fixDataScopes(world);
        cat = LinkDataStorable.getAsClient(world, key, true);
        if(cat != null && cat.hasPoints()) return cat;
        return null;
    }

    /**
     * An alternative approach to {@link #findLoosely()} that will brute-force iterate in a worse-case scenario.
     * This method can find {@link GridCatenary} instances that have been orphaned by some adverse process, or 
     * instances that don't follow the correct {@link TrackedStreamable#orderedByAssertionPriority assertion priority scheme}.
     * A catenary can become orphaned in rare circumstances where a contraption's lifecycle isn't properle tracked by a client 
     * (due to packet loss or client-sided timing issues), or if API users overuse the {@link GridCatenary#replaceStart discrete UUID replacement} 
     * system on catenaries with shared AnchorPoint references.
     * <h4> This method is rather expensive, so it shouldn't be called frequently. Ideally, this method can be removed once the GridAPI
     * is truly watertight.</h4>
     * @param world
     * @param start
     * @param end
     * @return The {@link GridCatenary} located between <code>start</code> and <code>end</code>, or <code>null</code>.
     */
    public static @Nullable GridCatenary findLooselyWithOrphans(ClientLevel world, GridUUID start, GridUUID end) {
        GridCatenary cat = findLoosely(world, start, end);
        if(cat != null) return cat;
        ConnectionKey key = new ConnectionKey(start, end);
        /**
         * catenaries may be orphaned by the contraption reification process
         * so they have to be looked up iteratively, but this only has to happen once.
         */
        Duo<GridUUID> orphanLookupIDs = TrackedStreamable.orderedByAssertionPriority(world, start, end);
        LinkDataStorable<?> potentialStorage = LinkDataStorable.getAsClient(orphanLookupIDs.second().getDataStorageHolder(world));
        if(potentialStorage != null) {
            GridConnection acquiredOrphan = potentialStorage.getAndRemoveOrphan(key);
            if(acquiredOrphan instanceof GridCatenary foundCat) return foundCat;
        }
        potentialStorage = LinkDataStorable.getAsClient(orphanLookupIDs.first().getDataStorageHolder(world));
        if(potentialStorage != null) {
            GridConnection acquiredOrphan = potentialStorage.getAndRemoveOrphan(key);
            if(acquiredOrphan instanceof GridCatenary foundCat) return foundCat;
        }
        return null;
    }

    /**
     * This method is broken out from {@link ContraptionDissassemblyPacketMixin}
     * because this code is prone to error and debugging is easier if its outside a mixin
     */
    public static void replaceEndsOnDisassemble(ClientGrid grid, LinkDataStorable.Client storage, StructureTransform transform, AbstractContraptionEntity ace) {
        if(storage == null) return;
        if(storage.getAll().isEmpty()) return;
        final ObjectSet<GridCatenary> catenaryStorage = storage.getAll();
        if(catenaryStorage == null || catenaryStorage.isEmpty()) return;
        // iterator.next() throws an error due to what i think was a race condition
        // unimi's objectset foreach does not use an iterator so it can be avoided like so:
        final List<GridCatenary> cats = new ArrayList<>(catenaryStorage.size());
        catenaryStorage.forEach(cat -> cats.add(cat));
        // we need to iterate over a copy of the storage since the following loop modifies the storage contents
        for(GridCatenary cat : cats) {
            if(cat == null) continue;
            if(cat.getStart() instanceof ContraptionUUID cu && cu.getUUID().equals(ace.getUUID()))
                cat.replaceStart(grid.getWorld(), cu.toVoxel(transform), null);
            else if(cat.getEnd() instanceof ContraptionUUID cu && cu.getUUID().equals(ace.getUUID()))
                cat.replaceEnd(grid.getWorld(), cu.toVoxel(transform), null);
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
        fixDataScopes(world);
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

    public CatenaryModel<?> getOrCreateModel(LevelReader world) {
        if(catenary != null) return this.catenary;
        GridCatenary lookup = LinkDataStorable.getAsClient(world, this);
        if(lookup != null && lookup.catenary != null) 
            this.catenary = lookup.catenary;
        else {
            this.catenary = CatenaryAttributes.MeshInitializer.FRESH_SIMULATION_EXPRESSIVE
                .make(world, start, end, trns.getType());
            if(lookup != null) lookup.catenary = this.catenary;
        }
        return this.catenary;
    }

    public GridCatenary reinitializeModel(LevelReader world, CatenaryAttributes.MeshInitializer init) {
        CatenaryModel.disposeOf(this.catenary);
        this.catenary = init.make(world, start, end, trns.getType());
        return this;
    }

    @Override
    public void tick(LevelReader world) {
        if(!(world instanceof ClientLevel cl)) return;
        CatenaryModel<?> model = getOrCreateModel(world);
        if(!isMoving(world)) return;
        Duo<AnchorPoint> ordered = TrackedStreamable.orderedByAssertionPriority(world, start, end);
        Vec3 start = ordered.start().getPos(world);
        Vec3 end = ordered.end().getPos(world);
        if(start == null || end == null) return;
        if(model instanceof SimulatedCatenary scat) {
            // if a wire is attached to a player we assume that its in the process of being unspooled, so it can freely resize
            if(!(this.start.belongsToPlayer(world) || this.end.belongsToPlayer(world)))
                scat.lockSpan();
            if(WindManager.INSTANCE.isEnabled())
                scat.applyWind(WindManager.INSTANCE.sample(cl, getMiddle(world)));
        }
        model.setOffset(start, end);
        model.update();
    }

    @Override
    public boolean canMoveDynamically(LevelReader world) {
        if(!hasPoints() || catenary == null) return false;
        return this.start.getAddress().canMoveDynamically(world) && this.end.getAddress().canMoveDynamically(world);
    }

    public boolean isMoving(LevelReader world) {
        if(catenary == null) return canMoveDynamically(world);
        return getCatenaryAttributesOrThrow().renders() && !catenary.isResting();
    }

    @Override
    public void adjustSpan(LevelReader world, float length) {
        getOrCreateModel(world).adjustSpan(world, Math.max(0.25f, Math.min(length, trns.getType().getMaximumSpan())));
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

    public void replaceStart(ClientLevel world, GridUUID newStartAddress, @Nullable DataScope forcedScope) {
        if(!hasPoints()) throw new IllegalStateException("Couldn't replace starting address for a GridCatenary with no points!");
        LinkDataStorable.popAsClient(world, this);
        this.start.replaceAddress(newStartAddress);
        if(forcedScope == null)
            fixDataScopes(world);
        else setDataScope(forcedScope);
        tick(world);
        SidedGridDispatcher.client(world).reassertCatenary(this, ProcessMode.TRY_THEN_SCHEDULE);
        sendLevelUpdates(world);
    }

    public void replaceEnd(ClientLevel world, GridUUID newEndAddress, @Nullable DataScope forcedScope) {
        if(!hasPoints()) throw new IllegalStateException("Couldn't replace ending address for a GridCatenary with no points!");
        LinkDataStorable.popAsClient(world, this);
        this.end.replaceAddress(newEndAddress);
        if(forcedScope == null)
            fixDataScopes(world);
        else setDataScope(forcedScope);
        tick(world);
        SidedGridDispatcher.client(world).reassertCatenary(this, ProcessMode.TRY_THEN_SCHEDULE);
        sendLevelUpdates(world);
    }

    /**
     * Renders this catenary to the provided renderer feature as an extension of a LivingEntity's geometry
     * <h2>This method is not thread safe!</h2>
     * The meshing process implemented here utilizes the {@link CatenaryMeshBuffer#REUSABLE reusable mesher},
     * this mesher has vertices added and removed from it during its lifespan, so any timing issues
     * (caused by concurrent access) will result in bad vertex ordering and geometry artifacts.
     * @param owner The owner of this catenary, used for acquiring a local offset vector
     * @param offset An optional, additional offset vector to apply when rendering this wire
     * @param buffers BufferSource to push vertices to,
     * @param matrixStack PoseStach pertaining to the relevent rendering context.
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void render(LivingEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        render(owner, owner.getRopeHoldPosition(pTicks).subtract(owner.getPosition(pTicks)), buffers, matrixStack, pTicks);
    }

    /**
     * Renders this catenary to the provided renderer feature as an extension of a LivingEntity's geometry
     * <h2>This method is not thread safe!</h2>
     * The meshing process implemented here utilizes the {@link CatenaryMeshBuffer#REUSABLE reusable mesher},
     * this mesher has vertices added and removed from it during its lifespan, so any timing issues
     * (caused by concurrent access) will result in bad vertex ordering and geometry artifacts.
     * @param owner The owner of this catenary, used for acquiring a local offset vector
     * @param offset An optional, additional offset vector to apply when rendering this wire
     * @param buffers BufferSource to push vertices to,
     * @param matrixStack PoseStack pertaining to the relevent rendering context.
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void render(LivingEntity owner, Vec3 offset, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        Duo<AnchorPoint> ordered = TrackedStreamable.orderedByAssertionPriority(owner.level(), start, end);
        Vec3 startPos = ordered.start().getPos(owner.level(), pTicks);
        Vec3 endPos = ordered.end().getPos(owner.level(), pTicks);
        CatenaryMeshBuffer.REUSABLE
            .bindTo(trns).at(startPos.add(endPos).scale(0.5f)).in(owner.level())
            .render(buffers, matrixStack, getOrCreateModel(owner.level()), 
                ordered.first().getOffset().add(offset).add(endPos.subtract(startPos).scale(0.5f)), pTicks);
        CatenaryMeshBuffer.REUSABLE.reset();
    }

    /**
     * Renders this catenary to to the provided renderer feature while applying
     * the proper AnchorPoint offset with the given BlockEntity.
     * This method is designed specifically to be invoked in BlockEntityRenderer contexts.
     * @param owner The owner of this catenary, used for acquiring a local offset vector from an AnchorPoint.
     * @param buffers BufferSource to push vertices to, 
     * @param matrixStack PoseStack pertaining to the relevent rendering context.
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void render(BlockEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        Duo<AnchorPoint> ordered = TrackedStreamable.orderedByAssertionPriority(owner.getLevel(), start, end);
        Vec3 worldMid = ordered.start().getPos(owner.getLevel(), pTicks).add(ordered.end().getPos(owner.getLevel(), pTicks)).scale(0.5);
        CatenaryMeshBuffer.REUSABLE 
            .bindTo(trns).at(worldMid).in(owner.getLevel())
            .render(buffers, matrixStack, getOrCreateModel(owner.getLevel()), worldMid.subtract(Vec3.atLowerCornerOf(owner.getBlockPos())), pTicks);
        CatenaryMeshBuffer.REUSABLE.reset();
    }

    /**
     * Renders this catenary to to the provided renderer feature while applying
     * the proper AnchorPoint offset with the given BlockEntity.
     * This method is designed specifically to be invoked in BlockEntityRenderer contexts.
     * @param owner The owner of this catenary, used for acquiring a local offset vector from an AnchorPoint.
     * @param buffers BufferSource to push vertices to, 
     * @param matrixStack PoseStack pertaining to the relevent rendering context.
     * @param pTicks Partial Ticks, accessible in most rendering contexts, used for lerping.
     */
    public void render(AbstractContraptionEntity owner, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        Duo<AnchorPoint> ordered = TrackedStreamable.orderedByAssertionPriority(owner.level(), start, end);
        Vec3 startPos = ordered.start().getPos(owner.level(), pTicks);
        Vec3 endPos = ordered.end().getPos(owner.level(), pTicks);
        CatenaryModel<?> model = getOrCreateModel(owner.level());
        if(startPos == null || endPos == null) {
            renderFallback(owner.level(), ordered, buffers, matrixStack, pTicks);
            return;
        }
        Vec3 worldMid = startPos.add(endPos).scale(0.5);
        /**
         * the offset needs be updated at the framerate of the game to make sure the endpoints don't lag behind
         * when they're lerped - this is usually done by the fixed update cycle of the catenary itself.
         * idk if this sucks or not but it seems to work so ¯\_(ツ)_/¯
         */
        if(model instanceof SimulatedCatenary scat)
            scat.setOffsetContinuous(owner.level(), startPos, endPos, worldMid);
        CatenaryMeshBuffer.REUSABLE
            .bindTo(trns)
            .at(worldMid)
            .in(owner.level())
            .render(buffers, matrixStack, model, ordered.start().getOffset().add(endPos.subtract(startPos).scale(0.5)), pTicks);
        CatenaryMeshBuffer.REUSABLE.reset();
    }

    /**
     * Catenaries that involve entities (usually contraptions) may occasionally render before said entity has been
     * added to the world depending on frame timing or network lag, so we can safely fall back on the previously
     * stored catenary information instead. This is slightly more expensive, since the start and end now must be
     * computed based on the catenary's offset. It also means that the catenary may render with the incorrect 
     * local offset, where the endpoints don't quite line up, but this edge case is transient and shouldn't 
     * last long enough to be especially noticeable.
     */
    private void renderFallback(LevelReader world, Duo<AnchorPoint> ordered, MultiBufferSource buffers, PoseStack matrixStack, float pTicks) {
        if(!(getOrCreateModel(world) instanceof SimulatedCatenary scat)) return;
        CatenaryMeshBuffer.REUSABLE
            .bindTo(trns)
            .at(scat.getWorldlyMidpoint())
            .in(world)
            .render(buffers, matrixStack, scat, ordered.start().getOffset().add(scat.getWorldlyEndPoint().subtract(scat.getWorldlyStartPoint()).scale(0.5)), pTicks);
        CatenaryMeshBuffer.REUSABLE.reset();
        return;
    }

    public static void renderToSection(LevelReader world, SectionPos sectionPos, BlockPos sectionOrigin, ObjectSet<GridCatenary> catenaries, SectionRenderingContext context) {
        synchronized(catenaries) {
            // we can't use the reusable mesher here since chunk meshing occurs in parallel, but instantiating it here means that we can at least re-use this instance for every catenary in this section
            CatenaryMeshBuffer mesher = CatenaryMeshBuffer.asEmpty();
            for(GridCatenary cat : catenaries) {
                if(cat == null || !cat.hasPoints() || cat.canMoveDynamically(world) || !cat.getCatenaryAttributesOrThrow().renders()) continue;
                AnchorPoint point = (AnchorPoint)cat.getPrimaryConstruct(world);
                Vec3 startPos = point.getAddress().getPos(world, 1f);
                mesher.bindTo(cat.getTransmitter())
                    .at(startPos).in(context.getRegion())
                    .useAtlasUVs()
                    .render(context, cat.getOrCreateModel(world), CatenaryAccessor.getLocalizedOffset(world, sectionOrigin, point), 1f);
            }
            mesher.reset();
        }
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
    public float calculateSpan() {
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
        return TrackedStreamable.orderedByAssertionPriority(world, start, end).first();
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
        if(catenary == null) return "Catenary uninitialized, " + calculateSpan() + "m";
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
