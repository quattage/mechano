package com.quattage.mechano.core.block.datagen;

import javax.annotation.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.DirectionTransformer;
import com.quattage.mechano.core.block.SimpleOrientedBlock;
import com.quattage.mechano.core.block.VerticallyOrientedBlock;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ModelFile;

public class DynamicStateGenerator extends SpecialBlockStateGen {

    private final @Nullable EnumProperty<?> typeDelegate;
    private final @Nullable String customFolder;


    public DynamicStateGenerator(EnumProperty<?> typeDelegate, String customFolder) {
        this.typeDelegate = typeDelegate;
        this.customFolder = customFolder;
    }

    public DynamicStateGenerator(EnumProperty<?> typeDelegate) {
        this.typeDelegate = typeDelegate;
        customFolder = null;
    }

    public DynamicStateGenerator(String customFolder) {
        this.typeDelegate = null;
        this.customFolder = customFolder;
    }

    public DynamicStateGenerator() {
        this.typeDelegate = null;
        customFolder = null;
    }

    @Override
    protected int getXRotation(BlockState state) {
        return DirectionTransformer.getUp(state) == Direction.DOWN ? 180 : 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        return horizontalAngle(DirectionTransformer.getForward(state));
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx,
        RegistrateBlockstateProvider provider, BlockState state) {

        String typeName = (typeDelegate == null) ? "base" : 
            state.getValue(typeDelegate).getSerializedName();

        String orientSuffix = 
            (!DirectionTransformer.isDistinctionRequired(state) &&
            DirectionTransformer.isHorizontal(state)) 
            ? "_side" : "";

        if(customFolder == null)
            return provider.models().getExistingFile(Mechano.extend(ctx, 
                typeName + orientSuffix));

        return provider.models().getExistingFile(Mechano.extend(ctx, 
            customFolder, typeName + orientSuffix));

    }
}
