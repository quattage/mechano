package com.quattage.mechano.foundation.api.transmitter;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.catenary.CatenaryAttributes.CatenaryAttributeHolder;
import com.quattage.mechano.foundation.catenary.CatenaryModelProvider;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
        registryType.packedIndex = (byte)(keys.length - 128);
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






    public static class TransmitterType<T extends Transmitter<?>> { 

        private static final float minimumDistance = 0.16f;

        private byte packedIndex = -1;

        private final Supplier<T> defaultConstructor;
        public final @Nullable StreamCodec<ByteBuf, T> streamCodec;

        private final boolean canSameBlock;
        private final boolean ignoresLimits;
        private final int maxDistance;
        public final CatenaryAttributeHolder defaults;
        
        private ResourceLocation textureLocation = null;
        private ResourceLocation atlasLocation = null;
        private TextureAtlasSprite atlasSprite = null;

        public static final StreamCodec<ByteBuf, TransmitterType<?>> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public TransmitterType<?> decode(ByteBuf buffer) {
                TransmitterType<?> type = INSTANCE.getRaw(buffer.readByte() + 128);
                if(type.streamCodec != null) type.streamCodec.decode(buffer);
                return type;
            }
            @Override 
            public void encode(ByteBuf buffer, TransmitterType<?> value) {
                buffer.writeByte(value.packedIndex);
            }
        };
    

        protected TransmitterType(Supplier<T> defaultConstructor, StreamCodec<ByteBuf, T> streamCodec, boolean canSameBlock, boolean ignoresLimits, int maxDistance, CatenaryAttributeHolder defaults, ResourceLocation tex) {
            this.defaultConstructor = defaultConstructor;
            this.streamCodec = streamCodec;
            this.canSameBlock = canSameBlock;
            this.ignoresLimits = ignoresLimits;
            this.maxDistance = maxDistance;
            this.defaults = defaults;
            this.textureLocation = tex;
        }

        /**
         * @return <code>true</code> if this TransmitterType supports
         * making connections between different anchors within the same
         * block.
         */
        public boolean supportsSameBlockConnections() {
            return canSameBlock;
        }

        public int getIndex() {
            return packedIndex + 128;
        }

        /**
         * @return the maximum distance (in meters) that 
         * a single wire of this type can span
         */
        public int getMaximumSpan() {
            return maxDistance;
        }

        /**
         * If this TransmitterType ignores limits, connections
         * made with it will not increment the connection count
         * on client-sided anchors, and will not fail if the anchor doesn't have room.
         * @return <code>true</code> if this TransmitterType doesn't care about 
         * anchor connector limits.
         */
        public boolean ignoresLimits() {
            return this.ignoresLimits;
        }

        /**
         * @return the minimum distance (in meters) that 
         * a single wire of this type can span. For now,
         * minimum distance is just a constant <code>(0.16)</code>
         * (or 16 centimeters)
         */
        public float getMinDistance() {
            return supportsSameBlockConnections() ? 0 : TransmitterType.minimumDistance;
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
            return defaultConstructor.get();
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

        public CompoundTag writeTo(CompoundTag in) {
            in.putByte("trnsid", packedIndex);
            return in;
        }

        @Override
        public String toString() {
            ResourceLocation trnsKey = null;
            try { trnsKey = TransmitterRegistry.INSTANCE.getKey(this); }
            catch(Exception e) { return "mechano:transmission_acquisition_error@" + getClass().getSimpleName(); };
            return trnsKey.toString();
        }

        @OnlyIn(Dist.CLIENT)
        public ResourceLocation getTextureLocation() {
            return textureLocation != null ? textureLocation : CatenaryModelProvider.MISSING_TEX;
        }

        @OnlyIn(Dist.CLIENT)
        @SuppressWarnings("deprecation")
        public TextureAtlasSprite getAtlasSprite() {
            atlasSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(atlasLocation);
            return atlasSprite;
        }

        /**
         * Called by the {@link CatenaryModelProvider} before
         * {@link #applyResourceReloadResult} to Nullify all 
         * resource related data in this TransmitterType before
         * reloading it. This prevents stale resources from
         * persisting longer than they should.
         */
        @OnlyIn(Dist.CLIENT)
        public void unloadResource() {
            this.textureLocation = null; 
            this.atlasLocation = null;
            this.atlasSprite = null;
        }

        /**
         * Called by the {@link CatenaryModelProvider} to update this
         * Transmitter's {@link #defaults catenary attributes}
         * @param model The model cooresponding to this TransmitterType,
         * as it is supplied by {@link net.minecraft.server.packs.resources.SimplePreparableReloadListener#apply}
         */
        @OnlyIn(Dist.CLIENT)
        public void applyResourceReloadResult(CatenaryModelProvider.ModelDefinition model) {
            this.textureLocation = model.getTexture();
            this.atlasLocation = model.getAtlas();
            this.atlasSprite = null;
        }

        public RenderType getMaterial() {
            return defaults.getShaderFor(this);
        }
    }


    public static class TransmitterTypeBuilder<T extends Transmitter<?>> {

        @Nullable private StreamCodec<ByteBuf, T> streamCodec = null;
        private final Supplier<T> defaultCtor;

        private int maxDistance = 16;
        private boolean canSameBlock = false;
        private boolean ignoresLimits = false;

        private CatenaryAttributeHolder defaults = CatenaryAttributes.DEFAULT;
        private ResourceLocation tex = null;

        public TransmitterTypeBuilder(Supplier<T> defaultCtor) {
            this.defaultCtor = defaultCtor;
        } 

        /**
         * Call this method while constructing your TransmitterType if you want this
         * particular type to serlaize additional data to a codec. You supply the codec - 
         * it would traditionally be located somewhere in your Transmitter<?> subclass
         * and serialize data pertaining to it. This allows additional data
         * to be sent via packets, should such a thing be necessary. If you
         * don't want this, either don't call this method, or just supply <code>null</code>.
         * @param streamCodec 
         */
        public TransmitterTypeBuilder<T> writesToNetwork(@Nullable StreamCodec<ByteBuf, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        /**
         * Call this method if this TransmitterType should
         * be able to connect between different anchors within
         * the same block. Traditionally, this isn't allowed,
         * since it would be mostly useless for the player
         * to be able to do this. But some internal 
         * TransmitterTypes require this functionality.
         */
        public TransmitterTypeBuilder<T> supportsSameBlockConnections() {
            this.canSameBlock = true;
            return this;
        }

        /**
         * Call this method to adjust the maximum distance
         * (in meters) that individual wires of this type
         * are permitted to span when placed by the player.
         */
        public TransmitterTypeBuilder<T> maximumSpannedDistance(int maxDistance) {
            this.maxDistance = Math.max(2, maxDistance);
            return this;
        }

        /**
         * Call this method to make this TransmitterType ignore all restrictions
         * placed on connections. If this method is called, this TransmitterType
         * can be placed on anything, regardless of compatability or max connection count.
         */
        public TransmitterTypeBuilder<T> ignoreConnectionLimits() {
            this.ignoresLimits = true;
            return this;
        }

        /**
         * Define a custom set of {@link CatenaryAttributeHolder catenary attributes}
         * for the rendering pipeline of this TransmitterType's associated catenaries
         */
        public TransmitterTypeBuilder<T> withAttributes(CatenaryAttributeHolder defaults) {
            this.defaults = defaults;
            return this;
        }

        public TransmitterTypeBuilder<T> deferTextureTo(ResourceLocation loc) {
            this.tex = loc;
            return this;
        }

        public TransmitterType<T> build() {
            return new TransmitterType<T>(defaultCtor, streamCodec, canSameBlock, ignoresLimits, maxDistance, defaults, tex);
        }
    }
}
