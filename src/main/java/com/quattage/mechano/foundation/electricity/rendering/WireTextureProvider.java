package com.quattage.mechano.foundation.electricity.rendering;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.electricity.spool.WireSpool;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;


public class WireTextureProvider extends SimplePreparableReloadListener<Map<ResourceLocation, WireTextureProvider.JsonModel>> {

    protected static final class JsonModel {
        public Textures textures;

        protected static final class Textures {
            public String wire;

            public ResourceLocation wireTexture() {
                return new ResourceLocation(wire + ".png");
            }
        }
    }

    private static final Gson GSON = new GsonBuilder().setLenient().create();
    private static final ResourceLocation MISSING = Mechano.asResource("textures/block/wire/missing.png");
    private final Object2ObjectMap<ResourceLocation, ResourceLocation> textureCache = new Object2ObjectOpenHashMap<>(64);

    @Override
    protected Map<ResourceLocation, JsonModel> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return load(resourceManager);
    }

    public Map<ResourceLocation, JsonModel> load(ResourceManager manager) {

        Map<ResourceLocation, JsonModel> out = new HashMap<>();
        for (WireSpool spoolType : WireSpool.getAllTypes()) {
            Mechano.LOGGER.info("Searching for resource: " + modelFromSpool(spoolType));
            manager.getResource(modelFromSpool(spoolType)).ifPresentOrElse(resource -> {
                Reader reader;
                try {
                    reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8);
                    JsonModel model = GSON.fromJson(reader, JsonModel.class);
                    out.put(modelFromSpool(spoolType), model);
                    Mechano.LOGGER.info("Successfully loaded resource: " + spoolType.getSpoolName() + ".json'");

                } catch (IOException e) {
                    Mechano.LOGGER.warn("Failed to load Wire Model resource '" + spoolType.getSpoolName() + "' - Check your JSON formatting!");
                    
                }
            }, 
                () -> Mechano.LOGGER.error("Failure to load Wire Model resource: '" + spoolType.getSpoolName() + ".json' could not be found!"));
        }
        return out;
    }

    public ResourceLocation get(WireSpool spoolType) {
        ResourceLocation key = modelFromSpool(spoolType);
        if(textureCache.containsKey(key)) return textureCache.get(key);
        return MISSING;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonModel> map, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        textureCache.clear();

        map.forEach((id, entry) -> {
            textureCache.put(id, entry.textures.wireTexture());
        });
    }


    public static ResourceLocation modelFromSpool(WireSpool spool) {
        return new ResourceLocation(Mechano.MOD_ID, "models/block/wire/" + spool.getSpoolName() + ".json");
    }

    public ResourceLocation getChainTexture(ResourceLocation chainType) {
        return textureCache.getOrDefault(chainType, MISSING);
    }
}