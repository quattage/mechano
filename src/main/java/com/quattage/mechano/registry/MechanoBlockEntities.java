package com.quattage.mechano.registry;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.content.block.Connector.HV.HVConnectorBlockEntity;
import com.quattage.mechano.content.block.Connector.HV.HVConnectorRenderer;
import com.quattage.mechano.content.block.Connector.LV.LVConnectorBlockEntity;
import com.quattage.mechano.content.block.Connector.LV.LVConnectorRenderer;
import com.quattage.mechano.content.block.Inductor.InductorBlockEntity;
import com.quattage.mechano.content.block.RollingWheel.RollingWheelBlockEntity;
import com.quattage.mechano.content.block.RollingWheel.RollingWheelControllerBlockEntity;
import com.quattage.mechano.content.block.ToolStation.ToolStationBlockEntity;
import com.simibubi.create.content.contraptions.base.CutoutRotatingInstance;
import com.simibubi.create.content.contraptions.base.KineticTileEntityRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.registry.Registry;

public class MechanoBlockEntities {
    // VANILLA BULLSHIT
    public static BlockEntityType<ToolStationBlockEntity> TOOL_STATION;

    // REGISTRATE
    public static final BlockEntityEntry<LVConnectorBlockEntity> LOW_VOLTAGE_CONNECTOR = Mechano.registrate()
        .tileEntity("lv_connector", LVConnectorBlockEntity::new)
        .validBlocks(MechanoBlocks.LOW_VOLTAGE_CONNECTOR_BLOCK)
        .renderer(() -> LVConnectorRenderer::new)
        .register();

    public static final BlockEntityEntry<HVConnectorBlockEntity> HIGH_VOLTAGE_CONNECTOR = Mechano.registrate()
        .tileEntity("hv_connector", HVConnectorBlockEntity::new)
        .validBlocks(MechanoBlocks.HIGH_VOLTAGE_CONNECTOR_BLOCK)
        .renderer(() -> HVConnectorRenderer::new)
        .register();
    public static final BlockEntityEntry<InductorBlockEntity> INDUCTOR = Mechano.registrate()
        .tileEntity("inductor", InductorBlockEntity::new)
        .validBlocks(MechanoBlocks.INDUCTOR)
        .register();

    public static final BlockEntityEntry<RollingWheelBlockEntity> ROLLING_WHEEL = Mechano.registrate()
		.tileEntity("rolling_wheel", RollingWheelBlockEntity::new)
		.instance(() -> CutoutRotatingInstance::new, false)
		.validBlocks(MechanoBlocks.ROLLING_WHEEL)
		.renderer(() -> KineticTileEntityRenderer::new)
		.register();

    public static final BlockEntityEntry<RollingWheelControllerBlockEntity> ROLLING_WHEEL_CONTROLLER = Mechano.registrate()
		.tileEntity("rolling_wheel_controller", RollingWheelControllerBlockEntity::new)
		.instance(() -> CutoutRotatingInstance::new, false)
		.validBlocks(MechanoBlocks.ROLLING_WHEEL_CONTROLLER)
		.renderer(() -> KineticTileEntityRenderer::new)
		.register();


    public static void register() {
        TOOL_STATION = Registry.register(Registry.BLOCK_ENTITY_TYPE, 
            Mechano.newResource("tool_station"),
            FabricBlockEntityTypeBuilder.create(ToolStationBlockEntity::new,
                MechanoBlocks.TOOL_STATION).build(null));
    }
}
