
package com.quattage.mechano.foundation.block.hitbox;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.block.hitbox.HitboxCache.UnbuiltHitbox;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.StringRepresentable;

import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import static com.quattage.mechano.Mechano.HITBOXES;

@EventBusSubscriber
public class HitboxProvider extends SimplePreparableReloadListener<ResourceLocation>{


    private static final HitboxProvider INSTANCE = new HitboxProvider();
    @SubscribeEvent
    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    private final Gson GSON = new GsonBuilder().setLenient().create();

    @Override
    protected ResourceLocation prepare(ResourceManager manager, ProfilerFiller profiler) {
        loadHitboxes(manager);
        return null;
    }


    /**
     * Load hitboxes using Minecraft's resource reload stuff
     * @param manager ResourceManager exposed by the ReloadListener or the MinecraftServer object
     */
    public void loadHitboxes(ResourceManager manager) {
        final Stopwatch timer = Stopwatch.createStarted();
        int count = 0;
        for(UnbuiltHitbox<? extends StringRepresentable> unbuilt : HITBOXES.getAllUnbuilt()) {
            manager.getResource(unbuilt.getResourceLocation()).ifPresentOrElse(resource -> {
                Reader reader; 
                try {
                    reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8);
                    HITBOXES.putNew(unbuilt, assembleSingleHitbox(reader));
                } catch (IOException | ClassCastException e) {
                    Mechano.LOGGER.error("Exception loading hitbox at '" + unbuilt.getResourceLocation() + 
                        "' - JSON parsing threw the following error: \n" + e.getMessage());
                }
            }, () -> 
                Mechano.LOGGER.error("Exception loading Hitbox model at '" + 
                    unbuilt.getResourceLocation() + "' - The file could not be found!"));
            count++; 
        }

        Mechano.LOGGER.info("Loaded " + count + " hitboxes in " + timer.elapsed(TimeUnit.MILLISECONDS) + " ms");
        timer.stop();
    }


    @SuppressWarnings("unchecked")
    private List<List<Float>> assembleSingleHitbox(Reader reader) {

        // i will have my way with you
        List<Map<String, Map<String, Object>>> modelFile = 
            (List<Map<String, Map<String, Object>>>)(GSON.fromJson(reader, Map.class).get("elements"));
                
        final List<List<Float>> boxes = new ArrayList<>();
        List<Float> box = new ArrayList<>();

        for(Map<String, Map<String, Object>> raw : modelFile) {
            for(Map.Entry<String, Map<String, Object>> s : raw.entrySet()) {
                if(box.isEmpty()) {
                    if(s.getKey().equals("from")) {
                        for(Object o : (ArrayList<Object>)(s.getValue()))
                            box.add(((Double)o).floatValue());
                    }
                } else {
                    if(s.getKey().equals("to")) {
                        for(Object o : (ArrayList<Object>)(s.getValue()))
                            box.add(((Double)o).floatValue());
                        boxes.add(new ArrayList<Float>(box));
                        box = new ArrayList<>();
                    }
                }
            }
        }

        return boxes;
    }


    @Override
    protected void apply(ResourceLocation pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) { }
}
