
package com.quattage.mechano;

import com.quattage.mechano.content.block.power.alternator.rotor.AbstractRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.BigRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.SmallRotorBlock;
import com.quattage.mechano.content.block.power.alternator.rotor.dummy.BigRotorDummyBlock;
import com.quattage.mechano.content.block.power.alternator.slipRingShaft.SlipRingShaftBlock;
import com.quattage.mechano.content.block.power.alternator.stator.AbstractStatorBlock;
import com.quattage.mechano.content.block.power.alternator.stator.BigStatorBlock;
import com.quattage.mechano.content.block.power.alternator.stator.SmallStatorBlock;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.ConnectorTier0Block;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.ConnectorTier1Block;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.ConnectorTier2Block;
import com.quattage.mechano.content.block.power.transfer.connector.tiered.AbstractConnectorBlock;
import com.quattage.mechano.foundation.block.orientation.DynamicStateGenerator;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;

import static com.quattage.mechano.Mechano.REGISTRATE;
import static com.quattage.mechano.Mechano.UPGRADES;
import static com.quattage.mechano.Mechano.HITBOXES;
import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.pickaxeOnly;

public class MechanoBlocks {

    static {
		REGISTRATE.setCreativeTab(MechanoGroups.MAIN_TAB);
	}

    // public static final BlockEntry<UpgradeBlock> FORGE_UPGRADE = REGISTRATE.block("tool_forge", UpgradeBlock::new)
    //     .initialProperties(CommonProperties::dense)
    //     .properties(props -> props
    //         .sound(SoundType.NETHERITE_BLOCK)
    //         .noOcclusion()
    //     )
    //     .transform(pickaxeOnly())
    //     .blockstate((ctx, prov) -> prov.horizontalBlock(ctx.getEntry(), prov.models()
    //         .getExistingFile(defer(ctx, "tool_station", "forge")), 180))
    //     .simpleItem()
    //     .register();

    // public static final BlockEntry<ToolStationBlock> TOOL_STATION = REGISTRATE.block("tool_station", ToolStationBlock::new)
    //     .initialProperties(CommonProperties::wooden)
    //     .properties(props -> props
    //         .sound(SoundType.WOOD)
    //         .noOcclusion()
    //     )
    //     .transform(pickaxeOnly())
    //     .blockstate(new ToolStationGenerator()::generate)
    //     .item()
    //     .transform(customItemModel("tool_station", "base"))
    //     .register();

    public static final BlockEntry<SmallRotorBlock> SMALL_ROTOR = REGISTRATE.block("small_rotor", SmallRotorBlock::new)
        .initialProperties(CommonProperties::ductile)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(AbstractRotorBlock.MODEL_TYPE).in("rotor")::generate)
        .item()
        .transform(customItemModel("rotor", "small_rotor/single"))
        .register();

    public static final BlockEntry<BigRotorBlock> BIG_ROTOR = REGISTRATE.block("big_rotor", BigRotorBlock::new)
        .initialProperties(CommonProperties::ductile)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(AbstractRotorBlock.MODEL_TYPE).in("rotor")::generate)
        .item()
        .transform(customItemModel("rotor", "big_rotor/single"))
        .register();

    public static final BlockEntry<BigRotorDummyBlock> BIG_ROTOR_DUMMY = REGISTRATE.block("big_rotor_dummy", BigRotorDummyBlock::new)
        .initialProperties(BIG_ROTOR)
        .properties(props -> props
            .noLootTable()
			.noOcclusion()
            .pushReaction(PushReaction.BLOCK)
        )
        .blockstate(new DynamicStateGenerator().in("rotor")::generate)
        .transform(pickaxeOnly())
        .register();

    public static final BlockEntry<SmallStatorBlock> SMALL_STATOR = REGISTRATE.block("small_stator", SmallStatorBlock::new)
        .initialProperties(CommonProperties::dense) 
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
        )
        .transform(pickaxeOnly())
        .transform(HITBOXES.flag("stator", SmallStatorBlock.MODEL_TYPE, AbstractStatorBlock.ORIENTATION))
        .blockstate(new DynamicStateGenerator(SmallStatorBlock.MODEL_TYPE).in("stator")::generate)
        .item()
        .transform(customItemModel("stator", "small_stator/base_single"))
        .register();

    public static final BlockEntry<BigStatorBlock> BIG_STATOR = REGISTRATE.block("big_stator", BigStatorBlock::new)
        .initialProperties(SMALL_STATOR)
        .transform(pickaxeOnly())
        .transform(HITBOXES.flag("stator", BigStatorBlock.MODEL_TYPE, AbstractStatorBlock.ORIENTATION))
        .blockstate(new DynamicStateGenerator(BigStatorBlock.MODEL_TYPE).in("stator")::generate)
        .item()
        .transform(customItemModel("stator", "big_stator/base_single"))
        .register();

    public static final BlockEntry<SlipRingShaftBlock> SLIP_RING_SHAFT = REGISTRATE.block("slip_ring_shaft", SlipRingShaftBlock::new)
        .initialProperties(CommonProperties::malleable)
        .properties(props -> props
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
        )
        .transform(pickaxeOnly())
        .blockstate(new DynamicStateGenerator(SlipRingShaftBlock.MODEL_TYPE)::generate)
        .item()
        .transform(customItemModel("slip_ring_shaft", "base"))
        .register();

    public static final BlockEntry<ConnectorTier0Block> CONNECTOR_T0 = REGISTRATE.block("connector_tier_zero", ConnectorTier0Block::new)
        .initialProperties(CommonProperties::malleable)
        .transform(pickaxeOnly())
        .transform(HITBOXES.flag("connector", AbstractConnectorBlock.MODEL_TYPE, AbstractConnectorBlock.ORIENTATION))
        .blockstate(new DynamicStateGenerator(AbstractConnectorBlock.MODEL_TYPE).in("connector")::generate)
        .loot((lt, block) -> block.buildUpgradableLoot(block, lt))
        .item()
        .transform(customItemModel("connector", "connector_tier_zero/base"))
        .lang("Connector")
        .register();

    public static final BlockEntry<ConnectorTier1Block> CONNECTOR_T1 = REGISTRATE.block("connector_tier_one", ConnectorTier1Block::new)
        .initialProperties(CommonProperties::malleable)
        .transform(pickaxeOnly())
        .transform(UPGRADES.upgradesFrom(CONNECTOR_T0).withStep(1).whenClickedWith(CONNECTOR_T0))
        .transform(HITBOXES.flag("connector", AbstractConnectorBlock.MODEL_TYPE, AbstractConnectorBlock.ORIENTATION))
        .blockstate(new DynamicStateGenerator(AbstractConnectorBlock.MODEL_TYPE).in("connector")::generate)
        .loot((lt, block) -> block.buildUpgradableLoot(block, lt))
        .item()
        .transform(customItemModel("connector", "connector_tier_one/base"))
        .lang("Connector")
        .register();
    
    public static final BlockEntry<ConnectorTier2Block> CONNECTOR_T2 = REGISTRATE.block("connector_tier_two", ConnectorTier2Block::new)
        .initialProperties(CommonProperties::malleable)
        .transform(pickaxeOnly())
        .transform(UPGRADES.upgradesFrom(CONNECTOR_T1).withStep(2).whenClickedWith(CONNECTOR_T0))
        .transform(HITBOXES.flag("connector", AbstractConnectorBlock.MODEL_TYPE, AbstractConnectorBlock.ORIENTATION))
        .blockstate(new DynamicStateGenerator(AbstractConnectorBlock.MODEL_TYPE).in("connector")::generate)
        .loot((lt, block) -> block.buildUpgradableLoot(block, lt))
        .item()
        .transform(customItemModel("connector", "connector_tier_two/base"))
        .lang("Connector")
        .register();

    // public static final BlockEntry<DiagonalGirderBlock> DIAGONAL_GIRDER = REGISTRATE.block("diagonal_girder", DiagonalGirderBlock::new)
	// 	.initialProperties(CommonProperties::malleable)
	// 	.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
	// 	.transform(pickaxeOnly())
    //     .blockstate(new DynamicStateGenerator(DiagonalGirderBlock.MODEL_TYPE)::generate)
	// 	.item()
    //     .transform(customItemModel("diagonal_girder", "item"))
	// 	.register();

    // public static final BlockEntry<VoltometerBlock> VOLTOMETER = REGISTRATE.block("voltometer", VoltometerBlock::new)
	// 	.initialProperties(CommonProperties::malleable)
	// 	.properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
	// 	.transform(pickaxeOnly())
    //     .blockstate(new DynamicStateGenerator(VoltometerBlock.MODEL_TYPE)::generate)
	// 	.item()
    //     .transform(customItemModel("voltometer", "floor"))
	// 	.register();
    


    // public static final BlockEntry<TestBlock> TEST_BLOCK = REGISTRATE.block("test_block", TestBlock::new)
	// 	.initialProperties(CommonProperties::malleable)
	// 	.properties(p -> p
    //         .sound(SoundType.NETHERITE_BLOCK)
    //         .noOcclusion()
    //     )
    //     .blockstate(new DynamicStateGenerator()::generate)
	// 	.transform(pickaxeOnly())
	// 	.item()
    //     .transform(customItemModel("test_block", "base"))
	// 	.register();

    public static void register(IEventBus event) {
        Mechano.logReg("blocks");
    }

    public static class CommonProperties{

        public static Block malleable() {
            return Blocks.GOLD_BLOCK;
        }

        public static Block soft() {
            return Blocks.WHITE_WOOL;
        }

        public static Block ductile() {
            return Blocks.COPPER_BLOCK;
        }

        public static Block dense() {
            return Blocks.NETHERITE_BLOCK;
        }

        public static Block wooden() {
            return Blocks.SPRUCE_WOOD;
        }
    }
}
