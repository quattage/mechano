package com.quattage.mechano;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.network.MechanoPackets;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataGenContext;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Mechano.MOD_ID)
public class Mechano {
    
    public static final String MOD_ID = "mechano";
    public static final String ROOT = "com.quattage." + MOD_ID;
    public static final String ESC = "\u001b";


    private static final String NET_VERSION = "0.1";
    private static int netCount = 0;

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(Mechano.MOD_ID);

    public static final SimpleChannel network = NetworkRegistry.ChannelBuilder
        .named(Mechano.asResource("mechanoNetwork"))
        .clientAcceptedVersions(NET_VERSION::equals)
        .serverAcceptedVersions(NET_VERSION::equals)
        .networkProtocolVersion(() -> NET_VERSION)
        .simpleChannel();

    public Mechano() {
        Mechano.log("loading mechano");
        IEventBus bussy = FMLJavaModLoadingContext.get().getModEventBus();
        REGISTRATE.registerEventListeners(bussy);

        MechanoBlocks.register(bussy);
        MechanoItems.register(bussy);
        MechanoGroups.register(bussy);
        MechanoMenus.register(bussy);
        MechanoBlockEntities.register(bussy);
        MechanoRecipes.register(bussy);
        MechanoSounds.register(bussy);

        bussy.addListener(this::clientSetup);
        bussy.addListener(this::commonSetup);
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        MechanoPartials.register();
    }

    public void commonSetup(final FMLCommonSetupEvent event) {
        MechanoPackets.register();
    }

    public static void log(String message) {      
        String side = ESC + "[1;34m" + Thread.currentThread().getName() + ESC + "[1;35m";

        String prefix = ESC + "[1;35m[quattage/" + MOD_ID + "] {" + side + "} >> " + ESC + "[1;36m";
        String suffix = ESC + "[1;35m -" + ESC;
        System.out.println(prefix + message + suffix);
    }

    public static void log(Object o) {
        log("'" + o.getClass().getName() + "' -> [" + o + "]");
    }

    public static void logReg(String message) {      
        log("Registering " + MOD_ID + " " + message);
    }

    public static void logSlow(String text) {
        logSlow(text, 500);
    }

    private static long lastLog = 0;

    public static void logSlow(String message, int millis) {
        if((System.currentTimeMillis() - lastLog) > millis) {
            log(message);
            lastLog = System.currentTimeMillis();
        }
    }

    public static ResourceLocation asResource(String filepath) {
        return new ResourceLocation(MOD_ID, filepath.toLowerCase());
    }

    public static ResourceLocation defer(DataGenContext<?, ?> ctx, String append) {
        return defer(ctx, append, ctx.getId().getPath());
    }

    public static ResourceLocation defer(DataGenContext<?, ?> ctx, String append, String realName) {
        String resource = ctx.getId().getNamespace() + ":block/" + append + "/" + realName;
        Mechano.log("resource: " + resource);
        return new ResourceLocation(resource);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String folder) {
        return extend(ctx, "block", folder);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String root, String folder) {
        String path = root + "/" + ctx.getId().getPath() + "/" + folder;
        return new ResourceLocation(ctx.getId().getNamespace(), path);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String root, String sub, String folder) {
        String path = root + "/" + sub + "/" + folder;
        return new ResourceLocation(ctx.getId().getNamespace(), path);
    }

    public static MutableComponent asKey(String key) {
        return Component.translatable(MOD_ID + "." + key);
    }
}
