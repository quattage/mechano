package com.quattage.experimental_tables.registry;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import com.quattage.experimental_tables.ExperimentalTables;
import com.quattage.experimental_tables.content.block.HConnectorBlock;
import com.quattage.experimental_tables.content.block.InductorBlock;
import com.quattage.experimental_tables.content.block.LConnectorBlock;
import com.quattage.experimental_tables.content.block.UpgradeBlock;
import com.quattage.experimental_tables.content.block.WideTableBlock;
import com.simibubi.create.Create;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;

import com.mrh0.createaddition.groups.ModGroup;



public class ModBlocks {
    // VANILLA BULLSHIT
    public static final Block TOOL_STATION = registerBlock("tool_station",
        new WideTableBlock(null, FabricBlockSettings.of(Material.METAL).nonOpaque()), ItemGroup.DECORATIONS);
    
    public static final Block FORGE_UPGRADE = registerBlock("tool_forge",
        new UpgradeBlock(FabricBlockSettings.of(Material.METAL).nonOpaque()), ItemGroup.DECORATIONS);

    
    // REGISTRATE
    private static final CreateRegistrate REGISTRATE = ExperimentalTables.registrate().creativeModeTab(() -> ModGroup.MAIN);
    public static final BlockEntry<InductorBlock> INDUCTOR = REGISTRATE.block("inductor", InductorBlock::new)
        .initialProperties(SharedProperties::stone)
        .item()
        .transform(customItemModel())
        .register();
    public static final BlockEntry<LConnectorBlock> LOW_VOLTAGE_CONNECTOR_BLOCK = REGISTRATE.block("lv_connector", LConnectorBlock::new)
        .initialProperties(SharedProperties::stone)
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<HConnectorBlock> HIGH_VOLTAGE_CONNECTOR_BLOCK = REGISTRATE.block("hv_connector", HConnectorBlock::new)
        .initialProperties(SharedProperties::stone)
        .item()
        .transform(customItemModel())
        .register();
    

    //
    public static void register() {
        ExperimentalTables.LOGGER.info("Registering Mod Blocks");
        Create.REGISTRATE.addToSection(LOW_VOLTAGE_CONNECTOR_BLOCK, AllSections.KINETICS);
        Create.REGISTRATE.addToSection(HIGH_VOLTAGE_CONNECTOR_BLOCK, AllSections.KINETICS);
    }

    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registry.BLOCK, new Identifier(ExperimentalTables.MOD_ID, name), block);
    }
    
    private static Item registerBlockItem(String name, Block block, ItemGroup group) {
        Item item = Registry.register(Registry.ITEM, new Identifier(ExperimentalTables.MOD_ID, name),
            new BlockItem(block, new FabricItemSettings().group(group)));
        return item;
    }
}
