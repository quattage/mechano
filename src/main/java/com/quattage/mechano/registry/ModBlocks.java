package com.quattage.mechano.registry;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import com.simibubi.create.Create;
import com.simibubi.create.content.AllSections;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;

import com.mrh0.createaddition.groups.ModGroup;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.Connector.HV.HVConnectorBlock;
import com.quattage.mechano.content.block.Connector.LV.LVConnectorBlock;
import com.quattage.mechano.content.block.Inductor.InductorBlock;
import com.quattage.mechano.content.block.ToolStation.WideTableBlock;
import com.quattage.mechano.content.block.Upgrade.UpgradeBlock;

public class ModBlocks {
    // VANILLA BULLSHIT
    public static final Block TOOL_STATION = registerBlock("tool_station",
        new WideTableBlock(null, FabricBlockSettings.of(Material.METAL).nonOpaque()), ItemGroup.DECORATIONS);
    
    public static final Block FORGE_UPGRADE = registerBlock("tool_forge",
        new UpgradeBlock(FabricBlockSettings.of(Material.METAL).nonOpaque()), ItemGroup.DECORATIONS);

    
    // REGISTRATE
    private static final CreateRegistrate REGISTRATE = Mechano.registrate().creativeModeTab(() -> ModGroup.MAIN);
    public static final BlockEntry<InductorBlock> INDUCTOR = REGISTRATE.block("inductor", InductorBlock::new)
        .initialProperties(SharedProperties::stone)
        .item()
        .transform(customItemModel())
        .register();
    public static final BlockEntry<LVConnectorBlock> LOW_VOLTAGE_CONNECTOR_BLOCK = REGISTRATE.block("lv_connector", LVConnectorBlock::new)
        .initialProperties(SharedProperties::stone)
        .item()
        .transform(customItemModel())
        .register();

    public static final BlockEntry<HVConnectorBlock> HIGH_VOLTAGE_CONNECTOR_BLOCK = REGISTRATE.block("hv_connector", HVConnectorBlock::new)
        .initialProperties(SharedProperties::stone)
        .item()
        .transform(customItemModel())
        .register();
    

    //
    public static void register() {
        Mechano.LOGGER.info("Registering Mod Blocks");
        Create.REGISTRATE.addToSection(LOW_VOLTAGE_CONNECTOR_BLOCK, AllSections.KINETICS);
        Create.REGISTRATE.addToSection(HIGH_VOLTAGE_CONNECTOR_BLOCK, AllSections.KINETICS);
    }

    private static Block registerBlock(String name, Block block, ItemGroup group) {
        registerBlockItem(name, block, group);
        return Registry.register(Registry.BLOCK, Mechano.newResource(name), block);
    }
    
    private static Item registerBlockItem(String name, Block block, ItemGroup group) {
        Item item = Registry.register(Registry.ITEM,  Mechano.newResource(name),
            new BlockItem(block, new FabricItemSettings().group(group)));
        return item;
    }
}
