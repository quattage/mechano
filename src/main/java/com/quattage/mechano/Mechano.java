package com.quattage.mechano;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.quattage.mechano.foundation.block.hitbox.HitboxCache;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataGenContext;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Mechano.ID)
public class Mechano {

    public static final String ID = "mechano";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final HitboxCache HITBOXES = new HitboxCache();

    public Mechano(IEventBus modBus, ModContainer container) {
        ModLoadingContext ctx = ModLoadingContext.get();
        REGISTRATE.registerEventListeners(modBus);
        MechanoBlocks.register(modBus);
        MechanoBlockEntities.register(modBus);
        MechanoItems.register(modBus);
        MechanoPackets.register(modBus);
        MechanoTags.register(modBus);
        MechanoSettings.init(modBus);
        MechanoGroups.register(modBus);
        MechanoDataAttachments.register(modBus);
        modBus.addListener(EventPriority.LOWEST, MechanoData::collect);
    }

    public void commonSetup(FMLCommonSetupEvent event) {

    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static ResourceLocation defer(DataGenContext<?, ?> ctx, String append) {
        return defer(ctx, append, ctx.getId().getPath());
    }

    public static ResourceLocation defer(DataGenContext<?, ?> ctx, String append, String realName) {
        String resource = ctx.getId().getNamespace() + ":block/" + append + "/" + realName;
        return ResourceLocation.fromNamespaceAndPath(ID, resource);
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
}
