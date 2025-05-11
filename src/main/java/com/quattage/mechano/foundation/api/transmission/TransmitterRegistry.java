package com.quattage.mechano.foundation.api.transmission;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoTransmissionTypes;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

public class TransmitterRegistry {
    
    private boolean isLoaded = false;
    private ResourceLocation[] keys = new ResourceLocation[0];
    private final Object2ObjectOpenHashMap<ResourceLocation, TransmitterType<?>> contents = new Object2ObjectOpenHashMap<>(1);

    public Transmitter get(int id) {
        TransmitterType<?> type = getRaw(id);
        return type.make();
    }

    private TransmitterType<? extends Transmitter> getRaw(int id) {
        if(!isLoaded) throw new IllegalStateException("Attempted to access TransmitterRegistry before it has finished loading!");
        if(id < 0 || id >= keys.length)
            throw new IndexOutOfBoundsException("Index " + id + " is out of bounds for a TransmitterRegistry of size " + contents.size() + "!");
        ResourceLocation key = keys[id];
        if(key == null) throw new IllegalStateException("Index " + id + " couldn't find a valid TransmitterType key!"); 
        TransmitterType<?> type = contents.get(key);
        if(type == null) throw new IllegalStateException("The key '" + key + "' mapped to index " + id + " does not exist in this registry! (This indicates a registry error, and should be reported to the Mechano devs)");
        return type;
    }

    public Transmitter get(ResourceLocation id) {
        if(!isLoaded) throw new IllegalStateException("Attempted to access TransmitterRegistry before it has finished loading!");
        Objects.requireNonNull(id);
        TransmitterType<?> type = contents.get(id);
        if(type == null) throw new IllegalStateException("No transmitter '" + id + "' exists in the registry!");
        return type.make();
    }

    public Transmitter get(CompoundTag tag) {
        if(!isLoaded) throw new IllegalStateException("Attempted to access TransmitterRegistry before it has finished loading!");
        Objects.requireNonNull(tag);
        if(!tag.contains("id")) throw new IllegalArgumentException("Couldn't find TransmitterType from tag " + tag + " - This tag doesn't contain a transmitter id!");
        Transmitter out = get(tag.getByte("id") + 128);
        if(out.needsSerialization() && tag.contains("data"))
            out.loadFrom(tag.getCompound("data"));
        return out;
    }

    public <T extends Transmitter> TransmitterType<T> register(ResourceLocation key, Supplier<? extends TransmitterType<T>> func) {
        
        if(isLoaded) throw new IllegalStateException("Cannot register new entries to a TransmitterRegistry that has already been loaded!");
        Objects.requireNonNull(key);
        Objects.requireNonNull(func);
        if(contents.size() >= 32) throw new IllegalStateException("Cannot register new entry '" + key + "' - This TransmitterRegistry is full!");

        TransmitterType<T> registryType = func.get();
        if(contents.putIfAbsent(key, registryType) != null) 
            throw new IllegalArgumentException("Duplicate TransmitterRegistry at '" + key + "'");

        ResourceLocation[] copy = new ResourceLocation[keys.length + 1];
        System.arraycopy(keys, 0, copy, 0, keys.length);
        registryType.packedIndex = (byte)(keys.length - 128);
        copy[keys.length] = key;
        this.keys = copy;

        return registryType;
    }

    public void register(IEventBus modBus) {
        if(keys.length != contents.size()) 
            throw new IllegalStateException("An error occured when building this Transmitter registry - The key array does not match the size of the contents set! (" + keys.length + " != " + contents.size());
        isLoaded = true;
        contents.trim();

        for(ResourceLocation type : contents.keySet()) {
            Mechano.LOGGER.info("TYPE: " + type);
        }
    }





    @Override
    public String toString() {
        String out = "TransmitterRegistry[\n";
        for(int x = 0; x < keys.length; x++) 
            out += "\t" + x + ": (" + keys[x] + ", " + contents.get(keys[x]) + ")\n";
        return out + "]";
    }   






    public static class TransmitterType<T extends Transmitter> { 

        private byte packedIndex = -1;
        private final Function<Byte, T> defaultConstructor;
        @Nullable private final StreamCodec<ByteBuf, T> streamCodec;

        public static final StreamCodec<ByteBuf, Transmitter> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public Transmitter decode(ByteBuf buffer) {
                TransmitterType<?> type = MechanoTransmissionTypes.REGISTRY.getRaw(buffer.readByte() + 128);
                return type.streamCodec == null ? type.make() : type.streamCodec.decode(buffer);
            }
            @Override 
            @SuppressWarnings("unchecked")
            public void encode(ByteBuf buffer, Transmitter value) {
                buffer.writeByte(value.packedIndex);
                TransmitterType<?> type = MechanoTransmissionTypes.REGISTRY.getRaw(value.packedIndex + 128);
                if(type.streamCodec != null)
                    ((StreamCodec<ByteBuf, Transmitter>)type.streamCodec).encode(buffer, value);
            }
        };
    

        protected TransmitterType(Function<Byte, T> defaultConstructor, StreamCodec<ByteBuf, T> streamCodec) {
            this.defaultConstructor = defaultConstructor;
            this.streamCodec = streamCodec;
        }

        public byte getRegistryIndex() {
            return packedIndex;
        }

        @Override
        public int hashCode() {
            return packedIndex;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(!(obj instanceof TransmitterType<?> that)) return false;
            return this.packedIndex == that.packedIndex;
        }

        public T make() {
            return defaultConstructor.apply(packedIndex);
        }

        /**
         * Gets a bitmask for this particular TransmitterType out of the
         * 32 possible transmitters that can be reigstered. Individual BlockEntities
         * can use this bitmask as a fast means of enforcing compatability standards.
         * For example, a "high voltage" block entity may only host connections
         * via a handful of "high voltage" transmitters.
         * @return The bitmask, 32 bits long. By default, this is 1 shifted by the numerical index of this transmitter.
         */
        public int bitmask() {
            return 1 << packedIndex;
        }
    }


    public static class TransmitterTypeBuilder<T extends Transmitter> {

        @Nullable private StreamCodec<ByteBuf, T> streamCodec = null;
        private final Function<Byte, T> defaultCtor;

        public TransmitterTypeBuilder(Function<Byte, T> defaultCtor) {
            this.defaultCtor = defaultCtor;
        } 

        public TransmitterTypeBuilder<T> writesToNetwork(StreamCodec<ByteBuf, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        public TransmitterType<T> build() {
            return new TransmitterType<T>(defaultCtor, streamCodec);
        }
    }
}
