package com.quattage.mechano.foundation.api.blockEntity;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorage;
import com.quattage.mechano.foundation.api.ServerGrid;
import com.quattage.mechano.foundation.api.anchor.AnchorArray;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccess;
import com.quattage.mechano.foundation.api.entity.GriddableContraptionAttachment;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.quattage.mechano.foundation.api.landmark.identifier.ContraptionUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.VoxelUUID;
import com.quattage.mechano.foundation.api.switchboard.AnchorSyncPacket;
import com.quattage.mechano.foundation.api.switchboard.GridResponse.AnchorSynchronizer;
import com.quattage.mechano.foundation.mixin.ContraptionMixin;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class GriddableBlockEntity extends ElectricBlockEntity implements Griddable<BlockEntity>, CatenaryAccess {

    // always empty on the server
    private AnchorArray anchors = AnchorArray.EMPTY;
    private final SurrogateNode surrogate = new SurrogateNode(this);

    public GriddableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // TODO this should probably be moved out of the constructor
        AnchorArray.Builder unbuiltAnchors = AnchorArray.construct(this);
        constructAnchors(unbuiltAnchors);
        this.anchors = unbuiltAnchors.confirm(getBlockPos());
    }

    @Override
    public abstract void constructAnchors(AnchorArray.Builder anchors);

    public void applyContraptionOverride(GriddableContraptionAttachment newHost, BlockPos structurePos) {
        Objects.requireNonNull(newHost);
        if(!newHost.getWorld().isClientSide) return;
        for(int x = 0; x < anchors.size(); x++) {
            AnchorPoint anchor = anchors.getByIndex(x);
            if(anchor == null) continue;
            GridUUID address = anchor.getAddress();
            if(address instanceof VoxelUUID)
                anchor.replaceAddress(new ContraptionUUID(newHost.getSource().getUUID(), structurePos, x));
        }
        surrogate.forceAddressChange(newHost.getSurrogate().getOrCreateAddress());
    }

    @Override
    public void onAnchorSynced(Level world, int index) {
        if(!world.isClientSide) return;
        invalidateRenderBoundingBox();
    }

    @Override
    public void tick() {
        if(!level.isClientSide) return;
        if(!surrogate.isSynced(level)) return;
        forEachCatenary(cat -> cat.tick((ClientLevel)level));
    }

    @Override
    protected AABB createRenderBoundingBox() {
        if(!level.isClientSide || !surrogate.isSynced(level)) 
            return super.createRenderBoundingBox();
        return AABB.INFINITE;
    }

    @Override
    public void onLoad() {
        // the anchorpoint holder is set to empty on the server despite
        // being initially populated on both sides, this is stupid and dumb!!
        // who wrote this!?? (me, i did)
        if(!level.isClientSide)
            this.anchors = AnchorArray.EMPTY;
        super.onLoad();
        anchors.updateOrientation(getBlockState());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        if(!level.isClientSide()) return null;
        LinkDataStorage.Client storage = LinkDataStorage.getAsClient(this, false);
        if(storage == null) return null;
        return storage.getAll();
    }

    @Override
    public Visual getVisual() {
        return (selected, tooltip, posX, posY, graphics) -> {
            GuiGameElement.of(getBlockState().getBlock().asItem())
			.at(posX + 10, posY - 16, 450)
			.render(graphics);
		};
    }

    @Override
    public void onRefresh(LevelReader world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onRefresh(world, pos, oldState, newState);
        anchors.updateOrientation(newState);
    }

    @Override
    public void onBlockBroken(Level world, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockBroken(world, pos, oldState, newState);
        destroySurrogate();
    }

    @Override
    public Level getWorld() {
        return getLevel();
    }

    @Override
    public AnchorArray getAnchors() {
        return anchors;
    }

    @Override
    public GridUUID createSupplementaryAddress() {
        return new VoxelUUID(getBlockPos(), 0);
    }

    @Override
    public SurrogateNode getSurrogate() {
        return surrogate;
    }

    @Override
    public String describeState() {
        return "Block '" + getBlockState().getBlock().getName().getString() + "'";
    }

    @Override
    public BlockEntity getSource() {
        return this;
    }

    @Override
    public Vec3 getSourcePosition() {
        return getBlockPos().getCenter();
    }

    @Override
    protected void saveTo(CompoundTag tag, Provider registries) {
        writeAnchorData(tag);
    }

    @Override
    protected void loadFrom(CompoundTag tag, Provider registries) {
        readAnchorData(tag);
    }





    /**
     * A MovementBehaviour that uses {@link ContraptionMixin a mixin} to 
     * detect when a Contraption is assembled/dissassembled, right as the 
     * BlockEntity is removed/added from the world. This allows {@link GriddableBlockEntity}
     * instances to hand off their data to the {@link Contraption} properly. Note that
     * all methods provided by this class are only called on the server.
     */
    public static class GriddableMovementBehaviour implements MovementBehaviour {

        /**
         * Called whenever a contraption containing a {@link GriddableBlockEntity} is almost finished
         *  binding itself to its {@link AbstractContraptionEntity}. At the time of invocation,
         * <code>blockEntity</code> is stale, since it has already been removed from the world.
         * This method is used to pull any remaining grid-adjacent data from the BE and transfrm/store
         * it in <code>contraptionEntity</code>
         * @param contraptionEntity The contraption that has been created.
         * @param blockEntity The GriddableBlockEntity that contains relevent grid information.
         * @param structurePos The contraption-space position within <code>contraptionEntity</code> that this BE is locatd at.
         */
        public void onContraptionAssembled(GriddableContraptionAttachment data, TransientStructureContainer container) {
            // Mechano.LOGGER.error("ASSEMBLY START");
            Objects.requireNonNull(data);
            if(!(data.getWorld() instanceof ServerLevel world) 
                || !container.blockEntity.surrogate.isSynced(world)) 
                    return;
            if(!ServerGrid.ALLOW_DYNAMIC_REASSERTIONS) {
                container.blockEntity.surrogate.destroy();
                AnchorSynchronizer.of(container.blockEntity.surrogate.getOrCreateAddress()).sendToClients(world);
                return;
            }
            container.blockEntity.getSurrogate().forEachAssociated(node -> {
                // GridUUID before = node.getAddress().copy();
                node.replaceHolder(world, data, new ContraptionUUID(data, container.structurePos, node.getAddress().getIndex()), true);
                data.markParticipatingSubsurrogate(container.blockEntity.getSurrogate(), container.structurePos);
                // Mechano.LOGGER.warn("Replaced " + before + " with " + node.getAddress());
            });
            // Mechano.LOGGER.error("ASSEMBLY END");
        }

        /**
         * Called right after {@link AbstractCntraptionEntity} is removed from the world and
         * cleared from its associated {@link Contraption}. This method is used to transform
         * extra data added to the {@link Contraption} back to its original, BlockEntity-compatible 
         * form.
         * @param contraptionEntity The contraptionEntity that was removed. This reference is stale, since its already been removed.
         * @param blockEntity The GriddableBlockEntity that needs to be primed with de-transfoemd contraption data.
         * {@see #onContraptionAssembled}
         */
        public void onContraptionDisassembled(StructureTransform transform, GriddableContraptionAttachment data, TransientStructureContainer container) {
            // Mechano.LOGGER.error("DISASSEMBLY START");
            Objects.requireNonNull(data);
            if(!(data.getWorld() instanceof ServerLevel world) || !ServerGrid.ALLOW_DYNAMIC_REASSERTIONS)
                return;
            data.forEachAssociated(transform, node -> {
                // GridUUID before = node.getAddress().copy();
                node.replaceHolder(world, container.blockEntity, container.blockEntity.getSurrogate().getOrCreateAddress(), false);
                CatnipServices.NETWORK.sendToClientsTrackingChunk((ServerLevel)data.getSource().level(), new ChunkPos(data.getSource().getOnPos()), new AnchorSyncPacket(AnchorSynchronizer.of(node)));
                // Mechano.LOGGER.warn("Replaced " + before + " with " + node.getAddress());
            });
            data.getSource().removeData(MechanoData.ANCHOR_ATTACHMENT);
            // Mechano.LOGGER.error("DISASSEMBLY END");
        }
    } 


    /**
     * A ducking interface for the {@Link ContraptionMixin}
     * to access transient BE instances after they're added/
     * removed to/from the world during the {@link Contraption}
     * dissassembly/assembly process.
     */
    public interface MovingGriddableAccessor {
        
        /**
         * The griddable blocks that are about to be removed or re-added 
         * when a contraption transitions between assembly states.
         * @return A list of unique {@link TransientStructureContainer} objects.
         */
        public abstract List<TransientStructureContainer> getTransientGriddables();

        public default void forEachGriddable(Consumer<TransientStructureContainer> action) {
            List<TransientStructureContainer> griddables = getTransientGriddables();
            if(griddables == null || griddables.isEmpty()) return;
            for(TransientStructureContainer container : griddables) {
                if(!TransientStructureContainer.isValid(container)) 
                    continue;
                action.accept(container);
            }
        }

        public default void invokeAssemble(AbstractContraptionEntity thisEntity) {
            GriddableContraptionAttachment newData = new GriddableContraptionAttachment(thisEntity);
            thisEntity.setData(MechanoData.ANCHOR_ATTACHMENT, newData);
            forEachGriddable(container -> container.behaviour.onContraptionAssembled(newData, container));
        }

        public default void invokeDisassemble(AbstractContraptionEntity thisEntity, StructureTransform transform) {
            Griddable<?> points = thisEntity.getExistingDataOrNull(MechanoData.ANCHOR_ATTACHMENT);
            if(!(points instanceof GriddableContraptionAttachment data)) return;
            forEachGriddable(container -> container.behaviour.onContraptionDisassembled(transform, data, container));
            // do not remove the data attachment yet because there's still operations that occur
            // later in the AbstractContraptionEntity mixin
        }
    }



    /**
     * A context container to store structure information for the brief period between when a block is removed 
     * from the world and when its associated contraption entity is added to the world. 
     */
    public static record TransientStructureContainer(GriddableMovementBehaviour behaviour, GriddableBlockEntity blockEntity, BlockPos structurePos, BlockPos realPos) {
        protected static boolean isValid(TransientStructureContainer container) {
            if(container == null) {
                Mechano.LOGGER.error(container + " failed validity checks and was skipped.");
                return false;
            }
            return container.behaviour != null && container.blockEntity != null && container.structurePos != null && container.realPos != null;
        }

        private String realPosAsString() { return "Real(" + realPos == null ? "NULL)" : (realPos.getX() + ", " + realPos.getY() + ", " + realPos.getZ() + ")"); }
        private String structurePosAsString() { return "Structure(" + structurePos == null ? "NULL)" : (structurePos.getX() + ", " + structurePos.getY() + ", " + structurePos.getZ() + ")"); }
        private String behaviourAsString() { return "Behaviour(" + behaviour == null ? "NULL)" : (behaviour.getClass().getSimpleName() + ")"); }
        private String blockEntityAsString() { return "BE(" + blockEntity == null ? "NULL)" : (blockEntity.getClass().getSimpleName() + ")"); }
        
        @Override
        public final String toString() {
            return "TransientStructureContainer[" + behaviourAsString() + ", " 
                + blockEntityAsString() + ", " + structurePosAsString() + ", " + realPosAsString() + "]";
        }
    }
}