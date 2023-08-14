package com.quattage.mechano;

import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderWrenchBehavior;
import com.quattage.mechano.content.item.spool.ElectricNodeSpoolBehavior;
import com.quattage.mechano.core.electricity.rendering.ElectricNodeWireDebugger;
import com.quattage.mechano.core.events.ClientBehavior;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class MechanoClientEvents {

    public static final ElectricNodeSpoolBehavior SPOOL_BEHAVIOR = new ElectricNodeSpoolBehavior("SpoolLookingAtElectricNode");
    public static final DiagonalGirderWrenchBehavior GIRDER_BEHAVIOR = new DiagonalGirderWrenchBehavior("WrenchOnDiagonalGirder");
    public static final ElectricNodeWireDebugger WIRE_DEBUG_BEHAVIOR = new ElectricNodeWireDebugger("DebugMenuElectricNode");

    @SubscribeEvent
	public static void onTick(ClientTickEvent event) {
		if (!isGameActive())
			return;

		if (event.phase == Phase.START) {}

        for(ClientBehavior behavior : ClientBehavior.behaviors.values()) {
            behavior.tick();
        }
    }

    protected static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}
}
