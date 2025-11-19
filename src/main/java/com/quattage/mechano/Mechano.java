package com.quattage.mechano;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.quattage.mechano.infrastructure.datagen.MechanoDatagen;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataGenContext;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod(Mechano.ID)
public class Mechano {

    public static final String ID = "mechano";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(Mechano.ID)
        .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final Gson GSON = new GsonBuilder().setLenient().create();

    public Mechano(IEventBus modBus, ModContainer container) {
        ModLoadingContext ctx = ModLoadingContext.get();
        Mechano.REGISTRATE.registerEventListeners(modBus);
        MechanoBlocks.register(modBus);
        MechanoBlockEntities.register(modBus);
        MechanoEntities.register(modBus);
        MechanoItems.register(modBus);
        MechanoPackets.register(modBus);
        MechanoSounds.register(modBus);
        MechanoTags.register(modBus);
        MechanoGroups.register(modBus);
        MechanoData.register(modBus);
        modBus.addListener(EventPriority.HIGHEST, MechanoDatagen::collectHighPriority);
        modBus.addListener(EventPriority.LOWEST, MechanoDatagen::collectLowPriority);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(Mechano.ID, path);
    }

    public static ResourceLocation defer(DataGenContext<?, ?> ctx, String append) {
        return Mechano.defer(ctx, append, ctx.getId().getPath());
    }

    public static ResourceLocation defer(DataGenContext<?, ?> ctx, String append, String realName) {
        String resource = ctx.getId().getNamespace() + ":block/" + append + "/" + realName;
        return ResourceLocation.fromNamespaceAndPath(Mechano.ID, resource);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String rootType, String item) {
        return ResourceLocation.fromNamespaceAndPath(ctx.getId().getNamespace(), rootType + "/" + ctx.getId().getPath() + "/" + item);
    }

    public static ResourceLocation extend(DataGenContext<?, ?> ctx, String rootType, String[] in, String[] sub, String item) {
        String path = rootType;
        if(in != null && in.length > 0) {
            for(String s : in) 
                path += "/" + s;
            path += "/" + ctx.getName();
        }
        if(sub != null && sub.length > 0) {
            for(String s : sub) 
                path += "/" + s;
            path += "/" + item;
        }
        return ResourceLocation.fromNamespaceAndPath(ctx.getId().getNamespace(), path);
    }

    public static LangBuilder lang() {
        return new LangBuilder(Mechano.ID);
    }
}
