package com.quattage.mechano;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.foundation.block.hitbox.HitboxCache;
import com.quattage.mechano.foundation.block.upgradable.UpgradeCache;
import com.quattage.mechano.foundation.electricity.grid.GlobalTransferGridDispatcher;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataGenContext;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.File;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;

@Mod(Mechano.MOD_ID)
public class Mechano {
    
    public static final String MOD_ID = "mechano";
    public static final String ESC = "\u001b";

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(Mechano.MOD_ID);
    public static final MechanoCapabilities CAPABILITIES = new MechanoCapabilities();
    public static final UpgradeCache UPGRADES = new UpgradeCache();

    public static final HitboxCache HITBOXES = new HitboxCache();
    
    
    public static boolean isLoaded = false;
    public static boolean IS_DEV_ENV;


    public Mechano() {
        Mechano.LOGGER.info("loading mechano");
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        MechanoSounds.register();
        REGISTRATE.registerEventListeners(modBus);

        CAPABILITIES.registerTo(forgeBus);
        MechanoSettings.init(modBus);
        MechanoBlocks.register(modBus);
        MechanoItems.register(modBus);
        MechanoBlockEntities.register(modBus);
        
        MechanoRecipes.register(modBus);
        MechanoGroups.register(modBus);
        MechanoMenus.register(modBus);
        GlobalTransferGridDispatcher.initTasks();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MechanoClient.init(modBus, forgeBus));

        modBus.addListener(EventPriority.LOWEST, MechanoData::gather);
        modBus.addListener(this::onCommonSetup);

        IS_DEV_ENV = (new File(System.getProperty( "user.dir" ) + "/IDEFlag.txt" )).exists();
        isLoaded = true;

        if(IS_DEV_ENV)
            Mechano.LOGGER.warn("IDE detected - Mechano development features enabled.");
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        Mechano.CAPABILITIES.HITBOX_PROVIDER.loadHitboxes();
        MechanoPackets.register();
    }


    public static LangBuilder lang() {
        return new LangBuilder(MOD_ID);
    }

    public static void log(String message) {      
        if(!isDevEnv()) return;
        String side = ESC + "[1;34m" + Thread.currentThread().getName() + ESC + "[1;35m";

        String time = LocalTime.now(ZoneId.of("America/Montreal")).truncatedTo(ChronoUnit.MILLIS).toString();
        String prefix = ESC + "[1;35m[{" + side + "} " + time + "] >> " + ESC + "[1;36m";
        String suffix = ESC + "[1;35m -" + ESC;
        System.out.println(prefix + message + suffix);
    }

    public static void log(Object o) {
        log("'" + o.getClass().getName() + "' -> [" + o + "]");
    }

    public static void logReg(String message) {
        log("Registering " + MOD_ID + " " + message);
    }

    public static boolean isDevEnv() {
        if(!isLoaded)
            return new File(System.getProperty( "user.dir" ) + "/IDEFlag.txt" ).exists();
        return Mechano.IS_DEV_ENV;
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
        return new ResourceLocation(resource);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String rootType, String item) {
        return new ResourceLocation(ctx.getId().getNamespace(), rootType + "/" + ctx.getId().getPath() + "/" + item);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String rootType, String[] in, String[] sub, String item) {
        
        String path = rootType;

        if(in != null) {
            for(String s : in) path += "/" + s;
        }

        path += "/" + ctx.getName();
        
        if(sub != null) {
            for(String s : sub) path += "/" + s;
        }

        path += "/" + item;

        return new ResourceLocation(ctx.getId().getNamespace(), path);
    }

    public static MutableComponent asKey(String key) {
        return Component.translatable(MOD_ID + "." + key);
    }
}