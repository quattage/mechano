package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.Connector.HV.HVConnectorBlockEntity;
import com.quattage.mechano.content.block.Connector.HV.HVConnectorRenderer;
import com.quattage.mechano.content.block.Connector.LV.LVConnectorBlockEntity;
import com.quattage.mechano.content.block.Connector.LV.LVConnectorRenderer;
import com.quattage.mechano.content.block.Inductor.InductorBlockEntity;
import com.quattage.mechano.content.block.ToolStation.ToolStationBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

public class ModBlockEntities {
    // VANILLA BULLSHIT
    public static BlockEntityType<ToolStationBlockEntity> TOOL_STATION;

    // REGISTRATE
    public static final BlockEntityEntry<LVConnectorBlockEntity> LOW_VOLTAGE_CONNECTOR = Mechano.registrate()
        .tileEntity("lv_connector", LVConnectorBlockEntity::new)
        .validBlocks(ModBlocks.LOW_VOLTAGE_CONNECTOR_BLOCK)
        .renderer(() -> LVConnectorRenderer::new)
        .register();

    public static final BlockEntityEntry<HVConnectorBlockEntity> HIGH_VOLTAGE_CONNECTOR = Mechano.registrate()
        .tileEntity("hv_connector", HVConnectorBlockEntity::new)
        .validBlocks(ModBlocks.HIGH_VOLTAGE_CONNECTOR_BLOCK)
        .renderer(() -> HVConnectorRenderer::new)
        .register();
    public static final BlockEntityEntry<InductorBlockEntity> INDUCTOR = Mechano.registrate()
        .tileEntity("inductor", InductorBlockEntity::new)
        .validBlocks(ModBlocks.INDUCTOR)
        .register();


    public static void register() {
        TOOL_STATION = Registry.register(Registry.BLOCK_ENTITY_TYPE, 
            Mechano.newResource("tool_station"),
            FabricBlockEntityTypeBuilder.create(ToolStationBlockEntity::new,
                ModBlocks.TOOL_STATION).build(null));
    }
}
