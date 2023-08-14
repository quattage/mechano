package com.quattage.mechano.content.block.integrated.toolStation;

import com.quattage.mechano.Mechano;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.ModelFile;

public class ToolStationGenerator extends SpecialBlockStateGen {

    @Override
    protected int getXRotation(BlockState state) {
        return 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        return horizontalAngle(state.getValue(HorizontalDirectionalBlock.FACING).getOpposite());
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, 
        RegistrateBlockstateProvider provider, BlockState state) {

        return provider.models().getExistingFile(Mechano.extend(ctx, 
            state.getValue(ToolStationBlock.MODEL_TYPE).getSerializedName()));
    }
}
