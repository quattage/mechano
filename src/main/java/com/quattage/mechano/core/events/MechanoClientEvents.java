package com.quattage.mechano.core.events;

import com.quattage.mechano.content.block.simple.diagonalGirder.DiagonalGirderWrenchBehavior;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class MechanoClientEvents {

    @SubscribeEvent
	public static void onTick(ClientTickEvent event) {
		if (!isGameActive())
			return;

        Minecraft instance = Minecraft.getInstance();
		//Level world = instance.level;
		if (event.phase == Phase.START) {
			// starting phase events
		}

        DiagonalGirderWrenchBehavior.tick(instance);
    }

    protected static boolean isGameActive() {
		return !(Minecraft.getInstance().level == null || Minecraft.getInstance().player == null);
	}
}
