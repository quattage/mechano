package com.quattage.mechano.foundation.mixin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.gridapi.Griddable;
import com.quattage.mechano.foundation.gridapi.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.gridapi.blockEntity.GriddableBlockEntity.GriddableMovementBehaviour;
import com.quattage.mechano.foundation.gridapi.blockEntity.GriddableBlockEntity.MovingGriddableAccessor;
import com.quattage.mechano.foundation.gridapi.blockEntity.GriddableBlockEntity.TransientStructureContainer;
import com.quattage.mechano.foundation.gridapi.entity.GriddableContraptionAttachment;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * TODO pr create to make this easier
 */
@Mixin(Contraption.class)
public abstract class ContraptionMixin implements MovingGriddableAccessor {

    @Unique private final List<TransientStructureContainer> griddables = new ArrayList<>(11);
    @Unique @Nullable private GriddableContraptionAttachment deferredData = null;

    @Shadow private Map<BlockPos, BlockEntity> presentBlockEntities;
    @Shadow private AbstractContraptionEntity entity;

    @Inject(
        method = "removeBlocksFromWorld", 
        at = { @At(
            value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/world/level/Level;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"
        ) }, 
        locals = LocalCapture.CAPTURE_FAILHARD, 
        cancellable = false, remap = false
    )
    private void mechano$onBlockEntityRemoved(Level world, BlockPos offset, CallbackInfo ci,
                                    List<BoundingBox> _dummy_a_, boolean[] dummy_b_, int dummy_c_, int dummy_d_, boolean dummy_e_, Iterator<?> dummy_f_,
                                    StructureBlockInfo block, BlockPos add) {
        MovementBehaviour actor = MovementBehaviour.REGISTRY.get(block.state());
        if(!(actor instanceof GriddableMovementBehaviour gmb)) return;
        BlockEntity be = world.getBlockEntity(add);
        if(!(be instanceof GriddableBlockEntity gbe)) return;
        griddables.add(new TransientStructureContainer(gmb, gbe, block.pos(), add));
    }

    @Inject(
        method = "addBlocksToWorld",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/contraptions/StructureTransform;apply(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = false, remap = false
    )
    private void mechano$onBlockEntityReinstantiated(Level world, StructureTransform transform, CallbackInfo ci,
                                                    boolean[] dummy_a_, int dummy_b_, int dummy_c_, boolean dummy_d_, Iterator<?> dummy_e_, 
                                                    StructureBlockInfo block, BlockPos targetPos, BlockState dummy_f_, BlockState dummy_g_, 
                                                    boolean dummy_h_, BlockEntity blockEntity, CompoundTag dummy_i_) {
        MovementBehaviour actor = MovementBehaviour.REGISTRY.get(block.state());
        if(!(actor instanceof GriddableMovementBehaviour gmb)) return;
        if(!(blockEntity instanceof GriddableBlockEntity gbe)) return;
        griddables.add(new TransientStructureContainer(gmb, gbe, block.pos(), targetPos));
    }

    @Inject(method="onEntityCreated", at = { @At(value = "TAIL") })
    private void mechano$onContraptionPrepare(AbstractContraptionEntity entity, CallbackInfo info) {
        invokeAssemble(entity, deferredData);
        deferredData = null;
        griddables.clear(); // clear the array because its contents have to be re-acquired later
    }

    @Inject(method="onEntityRemoved", at = { @At(value = "TAIL") })
    private void mechano$onContraptionDispose(AbstractContraptionEntity entity, CallbackInfo info) {
        invokeDisassemble(entity);
        griddables.clear(); // clear the array just in case the contraption instance persists (i dunno how this shit works man)
    }

    @Inject(
        method="onEntityInitialize", at = { @At(value = "HEAD") },
        cancellable = false, remap = false
    )
    private void mechano$onBlockEntityReify(Level world, AbstractContraptionEntity entity, CallbackInfo info) {
        if(world.isClientSide) {
            for(BlockEntity be : presentBlockEntities.values()) {
                if(!(be instanceof GriddableBlockEntity gbe)) continue;
                gbe.applyContraptionOverride(entity);
            }
        }
    }

    @Inject(
        method="writeNBT", at = { @At(value = "TAIL") },
        cancellable = false, remap = false,
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void mechano$writeComposite(HolderLookup.Provider registries, boolean spawnPacket, CallbackInfoReturnable<CompoundTag> cir, CompoundTag nbt) {
        Griddable<?> attachment = entity.getExistingDataOrNull(MechanoData.ANCHOR_ATTACHMENT);
        if(!(attachment instanceof GriddableContraptionAttachment data)) return;
        data.writeCompositeTo(nbt);
    }

    @Inject(
        method="readNBT", at = { @At(value = "TAIL") },
        cancellable = false, remap = false
    )
    private void mechano$readComposite(Level world, CompoundTag nbt, boolean spawnData, CallbackInfo info) {
        ListTag composite = nbt.getList("GridComposite", Tag.TAG_COMPOUND);
        if(composite == null || composite.isEmpty()) {
            deferredData = null;
            return;
        }
        deferredData = new GriddableContraptionAttachment();
        deferredData.readCompositeFrom(composite);
    }

    @Override
    public List<TransientStructureContainer> getTransientGriddables() {
        return griddables;
    }
}
