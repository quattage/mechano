
package com.quattage.mechano.infrastructure.datagen;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class DynamicStateGenerator extends SpecialBlockStateGen {

    private final @Nullable EnumProperty<?> typeDelegate;
    private @Nullable String[] customIn;
    private @Nullable String[] customSub;


    public DynamicStateGenerator(EnumProperty<?> typeDelegate) {
        this.typeDelegate = typeDelegate;
    }

    public DynamicStateGenerator() {
        this.typeDelegate = null;
        this.customIn = null;
        this.customSub = null;
    }

    public DynamicStateGenerator in(String... customIn) {
        this.customIn = customIn;
        return this;
    }

    public DynamicStateGenerator sub(String... customSub) {
        this.customSub = customSub;
        return this;
    }

    @Override
    protected int getXRotation(BlockState state) {
        return DirectionTransformer.getStateRotation(state).getX();
    }

    @Override
    protected int getYRotation(BlockState state) {
        return DirectionTransformer.getStateRotation(state).getY();
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx,
        RegistrateBlockstateProvider provider, BlockState state) {

        String typeName = (typeDelegate == null) ? "base" : 
            state.getValue(typeDelegate).getSerializedName();
        Mechano.LOGGER.info("Getting model " + typeName + " for state " + state);

        String orientSuffix = 
            (DirectionTransformer.isDistinctionRequired(state) &&
            DirectionTransformer.isHorizontal(state)) 
            ? "_side" : "";

        if(customIn == null && customSub == null)
            return provider.models().getExistingFile(Mechano.asResource("block/" + ctx.getName() + "/" + (typeName + orientSuffix)));
        return provider.models().getExistingFile(Mechano.extend(ctx, "block", customIn, customSub, typeName + orientSuffix));
    }
}
