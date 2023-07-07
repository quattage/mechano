package com.quattage.mechano;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.registry.MechanoBlocks;
import com.quattage.mechano.registry.MechanoMenus;
import com.quattage.mechano.registry.MechanoGroup;
import com.quattage.mechano.registry.MechanoItems;
import com.quattage.mechano.registry.MechanoPartials;
import com.quattage.mechano.registry.MechanoRecipes;
import com.quattage.mechano.registry.MechanoBlockEntities;
import com.simibubi.create.foundation.data.CreateRegistrate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
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
    private static final String NET_VERSION = "0.1";
    private static int netCount = 0;
    public static final String ESC = "\u001b";

    private static final Logger LOGGER = LogUtils.getLogger();
    

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(Mechano.MOD_ID);
    public static final SimpleChannel network = NetworkRegistry.ChannelBuilder
        .named(Mechano.asResource("mechanoNetwork"))
        .clientAcceptedVersions(NET_VERSION::equals)
        .serverAcceptedVersions(NET_VERSION::equals)
        .networkProtocolVersion(() -> NET_VERSION)
        .simpleChannel();
    
    private static int slowCount = 0;

    public Mechano() {
        genericSetup();
        new MechanoGroup("main");
    }

    public void genericSetup() {
        Mechano.log("loading mechano");
        IEventBus bussy = FMLJavaModLoadingContext.get().getModEventBus();
        REGISTRATE.registerEventListeners(bussy);

        MechanoBlocks.register(bussy);
        MechanoItems.register(bussy);
        MechanoMenus.register(bussy);
        MechanoBlockEntities.register(bussy);
        MechanoRecipes.register(bussy);

        bussy.addListener(this::clientSetup);
        bussy.addListener(this::postSetup);
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(MechanoRenderers::init);
        logReg("renderers");
        MechanoPartials.register(); // this will likely cause issues but it doesn't do anything yet so its fine
    }

    public void postSetup(FMLLoadCompleteEvent event) {
        // network.registerMessage(netCount++, NodeDataPacket.class, 
        //     NodeDataPacket::encode, 
        //     NodeDataPacket::decode, 
        //     NodeDataPacket::handle
        // );
    }

    public static void log(String message) {      
        String prefix = ESC + "[1;35m[quattage/" + MOD_ID + "]>> " + ESC + "[1;36m";
        String suffix = ESC + "[1;35m -" + ESC;
        System.out.println(prefix + message + suffix);
    }

    public static void logReg(String message) {      
        log("Registering " + MOD_ID + " " + message);
    }

    public static void logSlow(String text) {
        logSlow(text, 20);
    }

    public static void logSlow(String message, int ticks) {
        slowCount++;

        if(slowCount > ticks) {
            log(message);
            slowCount = 0;
        }
    }

    public static ResourceLocation asResource(String filepath) {
        return new ResourceLocation(MOD_ID, filepath.toLowerCase());
    }

    public static MutableComponent asKey(String key) {
        return Component.translatable(MOD_ID + "." + key);
    }
}
