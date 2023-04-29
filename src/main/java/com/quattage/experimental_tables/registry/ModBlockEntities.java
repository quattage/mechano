package com.quattage.experimental_tables.registry;

import com.quattage.experimental_tables.ExperimentalTables;
import com.quattage.experimental_tables.content.block.entity.HConnectorBlockEntity;
import com.quattage.experimental_tables.content.block.entity.InductorBlockEntity;
import com.quattage.experimental_tables.content.block.entity.LConnectorBlockEntity;
import com.quattage.experimental_tables.content.block.entity.ToolStationBlockEntity;
import com.quattage.experimental_tables.content.block.entity.renderer.HConnectorRenderer;
import com.quattage.experimental_tables.content.block.entity.renderer.LConnectorRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {
    // VANILLA BULLSHIT
    public static BlockEntityType<ToolStationBlockEntity> TOOL_STATION;

    // REGISTRATE
    public static final BlockEntityEntry<LConnectorBlockEntity> LOW_VOLTAGE_CONNECTOR = ExperimentalTables.registrate()
        .tileEntity("lv_connector", LConnectorBlockEntity::new)
        .validBlocks(ModBlocks.LOW_VOLTAGE_CONNECTOR_BLOCK)
        .renderer(() -> LConnectorRenderer::new)
        .register();

    public static final BlockEntityEntry<HConnectorBlockEntity> HIGH_VOLTAGE_CONNECTOR = ExperimentalTables.registrate()
        .tileEntity("hv_connector", HConnectorBlockEntity::new)
        .validBlocks(ModBlocks.HIGH_VOLTAGE_CONNECTOR_BLOCK)
        .renderer(() -> HConnectorRenderer::new)
        .register();
    public static final BlockEntityEntry<InductorBlockEntity> INDUCTOR = ExperimentalTables.registrate()
        .tileEntity("inductor", InductorBlockEntity::new)
        .validBlocks(ModBlocks.INDUCTOR)
        .register();


    public static void register() {
        TOOL_STATION = Registry.register(Registry.BLOCK_ENTITY_TYPE, 
            new Identifier(ExperimentalTables.MOD_ID, "tool_station"),
            FabricBlockEntityTypeBuilder.create(ToolStationBlockEntity::new,
                ModBlocks.TOOL_STATION).build(null));
    }
}
