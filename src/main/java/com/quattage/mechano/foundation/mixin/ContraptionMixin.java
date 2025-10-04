package com.quattage.mechano.foundation.mixin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.LinkDataStorage;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity.GriddableMovementBehaviour;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity.MovingGriddableAccessor;
import com.quattage.mechano.foundation.api.blockEntity.GriddableBlockEntity.TransientStructureContainer;
import com.quattage.mechano.foundation.api.catenary.CatenaryAccess;
import com.quattage.mechano.foundation.api.entity.GriddableContraptionAttachment;
import com.quattage.mechano.foundation.api.landmark.GridCatenary;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;

import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * this mixin ensures that BlockEntity instances that were part of the world 
 * and have Grid data associated with them get preserved for long enough that 
 * the data can be moved over to contraption during its assembly process, since
 * the AbstractContraptionEntity doesn't get added until after all blocks (and BEs)
 * are destroyed.
 */
@Mixin(Contraption.class)
public abstract class ContraptionMixin implements MovingGriddableAccessor, CatenaryAccess {

    private final List<TransientStructureContainer> mechano$griddables = new ArrayList<>(11);
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
        if(!gbe.getSurrogate().isSynced(world)) return;
        mechano$griddables.add(new TransientStructureContainer(gmb, gbe, block.pos(), add));
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
        mechano$griddables.add(new TransientStructureContainer(gmb, gbe, block.pos(), targetPos));
    }

    @Inject(method="onEntityCreated", at = { @At(value = "TAIL") })
    private void mechano$onContraptionPrepare(AbstractContraptionEntity entity, CallbackInfo info) {
        invokeAssemble(entity);
        mechano$griddables.clear(); // clear the array because its contents have to be re-acquired later
    }

    @Inject(method = "onEntityInitialize", at = { @At(value = "HEAD") })
    private void mechano$onEntityInit(Level world, AbstractContraptionEntity entity, CallbackInfo info) {
        if(!world.isClientSide) return;
        Contraption cast = (Contraption)(Object)this;
        if(cast.presentBlockEntities == null || cast.presentBlockEntities.isEmpty()) 
            return;
        GriddableContraptionAttachment data = new GriddableContraptionAttachment(entity);
        data.getAnchors(); // apply aliased addresses to all constituant griddables and cache the result
        entity.setData(MechanoData.ANCHOR_ATTACHMENT, data);
    }

    @Override
    public @Nullable ObjectSet<GridCatenary> getCatenaries() {
        if(entity == null) return null;
        LinkDataStorage.Client storage = LinkDataStorage.getAsClient(entity, false);
        if(storage == null) return null;
        return storage.getAll();
    }

    @Override
    public List<TransientStructureContainer> getTransientGriddables() {
        return mechano$griddables;
    }
}
