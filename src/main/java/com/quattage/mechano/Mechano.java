package com.quattage.mechano;

import com.google.common.eventbus.Subscribe;
import com.mojang.logging.LogUtils;
import com.quattage.mechano.registry.MechanoBlocks;
import com.quattage.mechano.registry.MechanoItems;
import com.quattage.mechano.registry.MechanoPartials;
import com.quattage.mechano.registry.MechanoRecipes;
import com.quattage.mechano.registry.MechanoRenderers;
import com.quattage.mechano.registry.MechanoTileEntities;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Mechano.MOD_ID)
public class Mechano {
    public static final String MOD_ID = "mechano";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final NonNullSupplier<CreateRegistrate> REGISTRATE = CreateRegistrate.lazy(Mechano.MOD_ID);
    public static final IEventBus BUSSY = FMLJavaModLoadingContext.get().getModEventBus();
    private static final String PROTOCOL = "1";
	public static final SimpleChannel NETWORK = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(MOD_ID, "main"))
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .networkProtocolVersion(() -> PROTOCOL)
            .simpleChannel();
    
    public Mechano() {
        BUSSY.addListener(this::clientMisc);
        BUSSY.addListener(this::postInit);
        BUSSY.addListener(this::registerRenderers);

        MinecraftForge.EVENT_BUS.register(this);

        MechanoBlocks.register();
        MechanoItems.register();
        MechanoTileEntities.register();
        MechanoRecipes.register(BUSSY);
    }

    public void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        MechanoRenderers.register(event);
    }

    public void clientMisc(final FMLClientSetupEvent event) {
        MechanoPartials.register();
    }

    public void postInit(FMLLoadCompleteEvent event) {
        Mechano.log("Mechano Initialized :)");
    }

    public static void log(String message) {
        Mechano.LOGGER.info(message);
    }

    public static CreateRegistrate getRegistrate() {
        return REGISTRATE.get();
    }

    public static ResourceLocation toResource(String filepath) {
        return new ResourceLocation(MOD_ID, filepath);
    }
}
