package com.quattage.mechano.content.block.Inductor;

import com.mrh0.createaddition.energy.BaseElectricTileEntity;
import com.quattage.mechano.registry.ModBlockEntities;
import com.simibubi.create.content.contraptions.goggles.IHaveGoggleInformation;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.builder.ILoopType.EDefaultLoopTypes;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;
import software.bernie.geckolib3.util.GeckoLibUtil;

public class InductorBlockEntity extends BaseElectricTileEntity implements IHaveGoggleInformation, IAnimatable {

    private AnimationFactory factory = GeckoLibUtil.createFactory(this);
    public static final long CAPACITY = 4096, MAX_IN = 2048, MAX_OUT = 2048;
    
    public InductorBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUCTOR.get(), pos, state, CAPACITY, MAX_IN, MAX_OUT);
    }

    @Override
    public boolean isEnergyInput(Direction arg0) {
        return false;
    }

    @Override
    public boolean isEnergyOutput(Direction arg0) {
        return false;
    }

    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController<InductorBlockEntity>(this, "controller", 0, this::predicate));
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.inductor.active", EDefaultLoopTypes.LOOP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimationFactory getFactory() {
        return this.factory;
    }
}