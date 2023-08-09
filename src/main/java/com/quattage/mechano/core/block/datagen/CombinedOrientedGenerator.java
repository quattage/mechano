package com.quattage.mechano.core.block.datagen;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.core.block.CombinedOrientedBlock;
import com.quattage.mechano.core.block.orientation.CombinedOrientation;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.ModelFile;

public class CombinedOrientedGenerator extends SpecialBlockStateGen {
    
    private String path;

    public CombinedOrientedGenerator(String sub, String type) {
        this.path = "block/" + sub + "/" + type;
    }

    @Override
    protected int getXRotation(BlockState state) {
        return getOrientation(state).getLocalUp() == Direction.DOWN ? 180 : 0;
    }

    @Override
    protected int getYRotation(BlockState state) {
        return horizontalAngle(getOrientation(state).getLocalForward());
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, 
        RegistrateBlockstateProvider provider, BlockState state) {
            
        String suffix = "";
        if(getOrientation(state).getLocalUp().getAxis().isHorizontal()) suffix = "_side";
        
        BlockModelBuilder modelBuilder = provider.models()
            .withExistingParent(path + suffix, provider.modLoc(path + suffix));
        
        return modelBuilder;
    }

    public CombinedOrientation getOrientation(BlockState state) {
        return state.getValue(CombinedOrientedBlock.ORIENTATION);
    }
}
