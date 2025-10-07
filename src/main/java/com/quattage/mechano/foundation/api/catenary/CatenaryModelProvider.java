package com.quattage.mechano.foundation.api.catenary;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.transmitter.TransmitterRegistry;
import com.quattage.mechano.foundation.api.transmitter.TransmitterType;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class CatenaryModelProvider extends SimplePreparableReloadListener<Map<TransmitterType<?>, CatenaryModelProvider.ModelDefinition>> {

    public static final ResourceLocation MISSING_TEX = Mechano.asResource("textures/block/catenary/missing.png");
    public static final ResourceLocation MISSING_ATLAS = Mechano.asResource("block/catenary/missing");

    public static final class ModelDefinition {
        public String texture;
        public ResourceLocation getTexture() {
            ResourceLocation rl = ResourceLocation.bySeparator(texture, ':');
            return ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath() + ".png");
        }
        public ResourceLocation getAtlas() {
            ResourceLocation rl = ResourceLocation.bySeparator(texture.replace("textures/", ""), ':');
            return ResourceLocation.fromNamespaceAndPath(rl.getNamespace(), rl.getPath());
        }
    }

    @Override
    protected Map<TransmitterType<?>, ModelDefinition> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<TransmitterType<?>, ModelDefinition> out = new HashMap<>();
        TransmitterRegistry.INSTANCE.forEachEntry((loc, trns) -> {
            trns.unloadResource();
            if(trns.getCatenaryAttributableOrThrow().getModelType().extruder == null) return;
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(loc.getNamespace(), "models/block/catenary/" + loc.getPath() + ".json");
            manager.getResource(location).ifPresentOrElse(resource -> {
                try(Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    ModelDefinition model = Mechano.GSON.fromJson(reader, ModelDefinition.class);
                    out.put(trns, model);
                } catch(IOException e) {
                    Mechano.LOGGER.warn("Failed to load TransmitterType model resource for '" 
                        + loc + "' - Potential malformed json");
                }
            }, () -> {
                Mechano.LOGGER.warn("Failed to load TransmitterType model resource for '" 
                + loc + "' - The location (" + location + ") couldn't be found!"); 
            });
        });
        Mechano.LOGGER.info("Loaded " + TransmitterRegistry.INSTANCE.size() + " transmitter model resources.");
        return out;
    }

    @Override
    protected void apply(Map<TransmitterType<?>, ModelDefinition> map, ResourceManager manager, ProfilerFiller profiler) {
        map.forEach(TransmitterType::applyResourceReloadResult);
    }
}
