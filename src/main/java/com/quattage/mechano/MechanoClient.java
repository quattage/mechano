package com.quattage.mechano;

import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.quattage.mechano.foundation.electricity.rendering.WireTextureProvider;

import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Mechano.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MechanoClient {
    
    public static final WireTextureProvider WIRE_TEXTURE_PROVIDER = new WireTextureProvider();

    @SubscribeEvent
    public static void onReisterReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(WIRE_TEXTURE_PROVIDER);
    }
}
