package com.quattage.mechano;

import com.quattage.mechano.foundation.api.landmark.client.AnchorGuiLayer;
import com.quattage.mechano.foundation.catenary.CatenaryModelProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

// @EventBusSubscriber(Dist.CLIENT)
public class MechanoClientEvents {

    private static final CatenaryModelProvider CATENARY_RESOURCES = new CatenaryModelProvider();

    public static boolean shouldRenderOverlay(Minecraft mc) {
        return !(mc == null || mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR);
    }

    @EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class Bus {
        @SubscribeEvent
        public static void registerLayers(RegisterGuiLayersEvent evt) {
            evt.registerAbove(VanillaGuiLayers.HOTBAR, Mechano.asResource("ancor_selection"), AnchorGuiLayer::renderOverlay);
        }
        @SubscribeEvent
        public static void registerReloadListeners(RegisterClientReloadListenersEvent evt) {
            evt.registerReloadListener(CATENARY_RESOURCES);
        }
    }
}
