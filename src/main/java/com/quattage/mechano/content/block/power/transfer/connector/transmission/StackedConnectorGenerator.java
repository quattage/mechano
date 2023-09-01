package com.quattage.mechano.content.block.power.transfer.connector.transmission;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.power.transfer.connector.transmission.stacked.ConnectorStackedTier0Block;
import com.quattage.mechano.core.block.DirectionTransformer;
import com.quattage.mechano.core.block.UpgradableBlock;
import com.quattage.mechano.core.block.datagen.DynamicStateGenerator;
import com.simibubi.create.foundation.data.SpecialBlockStateGen;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.generators.ModelFile;

public class StackedConnectorGenerator extends DynamicStateGenerator {

    public StackedConnectorGenerator() {
        super();
    }

    @Override
    public <T extends Block> ModelFile getModel(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider provider,
            BlockState state) {
        
        String orientSuffix = 
            (DirectionTransformer.isDistinctionRequired(state) &&
            DirectionTransformer.isHorizontal(state))
            ? "_side" : "";

        String variantName = "";
        if(state.getBlock() instanceof UpgradableBlock block)
            variantName = block.getTierId();

        return provider.models().getExistingFile(new ResourceLocation(Mechano.MOD_ID, 
            "block/connector_stacked/" + variantName + orientSuffix
        ));
    }
}
