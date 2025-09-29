package com.quattage.mechano.foundation.api.transmitter;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

public class TransmitterRegistry {

    public static final TransmitterRegistry INSTANCE = new TransmitterRegistry();
    
    private boolean isLoaded = false;
    private ResourceLocation[] keys = new ResourceLocation[0];
    private final Object2ObjectOpenHashMap<ResourceLocation, TransmitterType<?>> contents = new Object2ObjectOpenHashMap<>(1);

    public Transmitter<?> get(int id) {
        TransmitterType<?> type = getRaw(id);
        return type.make();
    }

    public int size() {
        return contents.size();
    }

    public ResourceLocation getKey(TransmitterType<?> type) {
        if(!isLoaded) throw new IllegalStateException("Attempted to access TransmitterRegistry before it has finished loading!");
        Objects.requireNonNull(type);
        int index = type.getIndex();
        if(index < 0 || index >= contents.size())
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds for a TransmitterRegistry of size " + contents.size() + "!");
        ResourceLocation key = keys[index];
        if(key == null) throw new IllegalStateException("Index " + index + " isn't mapped to a valid TransmitterType key!"); 
        return key;
    }

    public TransmitterType<? extends Transmitter<?>> getRaw(int id) {
        if(!isLoaded) throw new IllegalStateException("Attempted to access TransmitterRegistry before it has finished loading!");
        if(id < 0 || id >= keys.length)
            throw new IndexOutOfBoundsException("Index " + id + " is out of bounds for a TransmitterRegistry of size " + contents.size() + "!");
        ResourceLocation key = keys[id];
        if(key == null) throw new IllegalStateException("Index " + id + " isn't mapped to a valid TransmitterType key!"); 
        TransmitterType<?> type = contents.get(key);
        if(type == null) throw new IllegalStateException("The key '" + key + "' mapped to index " + id + " does not exist in this registry! (This indicates a registry error, and should be reported to the Mechano devs)");
        return type;
    }

    public Transmitter<?> get(ResourceLocation id) {
        if(!isLoaded) throw new IllegalStateException("Attempted to access TransmitterRegistry before it has finished loading!");
        Objects.requireNonNull(id);
        TransmitterType<?> type = contents.get(id);
        if(type == null) throw new IllegalStateException("No transmitter '" + id + "' exists in the registry!");
        return type.make();
    }

    public Transmitter<?> get(CompoundTag tag) {
        if(!isLoaded) throw new IllegalStateException("Attempted to access TransmitterRegistry before it has finished loading!");
        Objects.requireNonNull(tag);
        if(!tag.contains("trnsid")) throw new IllegalArgumentException("Couldn't find TransmitterType from tag " + tag + " - This tag doesn't contain a transmitter id!");
        Transmitter<?> out = get(tag.getByte("trnsid") + 128);
        if(out.needsSerialization() && tag.contains("data"))
            out.loadFrom(tag.getCompound("data"));
        return out;
    }

    public <T extends Transmitter<?>> TransmitterType<T> register(ResourceLocation key, Supplier<? extends TransmitterType<T>> func) {
        if(isLoaded) throw new IllegalStateException("Cannot register new entries to a TransmitterRegistry that has already been loaded!");
        Objects.requireNonNull(key);
        Objects.requireNonNull(func);
        if(contents.size() >= 32) throw new IllegalStateException("Cannot register new entry '" + key + "' - This TransmitterRegistry is full!");
        TransmitterType<T> registryType = func.get();
        if(contents.putIfAbsent(key, registryType) != null) 
            throw new IllegalArgumentException("Duplicate TransmitterRegistry at '" + key + "'");
        ResourceLocation[] copy = new ResourceLocation[keys.length + 1];
        System.arraycopy(keys, 0, copy, 0, keys.length);
        registryType.onAddedToRegistry(keys.length);
        copy[keys.length] = key;
        this.keys = copy;
        return registryType;
    }

    public void register(IEventBus modBus) {
        if(keys.length != contents.size()) 
            throw new IllegalStateException("An error occured when building this Transmitter<?> registry - The key array does not match the size of the contents set! (" + keys.length + " != " + contents.size());
        isLoaded = true;
        contents.trim();
    }

    public void forEachEntry(BiConsumer<ResourceLocation, TransmitterType<?>> cons) {
        for(Map.Entry<ResourceLocation, TransmitterType<?>> entry : contents.entrySet())
            cons.accept(entry.getKey(), entry.getValue());
    }



    @Override
    public String toString() {
        String out = "TransmitterRegistry[\n";
        for(int x = 0; x < keys.length; x++) 
            out += "\t" + x + ": (" + keys[x] + ", " + contents.get(keys[x]) + ")\n";
        return out + "]";
    }   


    
}
