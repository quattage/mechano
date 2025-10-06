package com.quattage.mechano.foundation.api.transmitter;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributes.CatenaryAttributable;
import com.quattage.mechano.foundation.api.catenary.CatenaryModelProvider;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class TransmitterType<T extends Transmitter<?>> implements CatenaryAttributable { 

    private final CatenaryAttributes.Container attributes;
    private final Supplier<T> defaultConstructor;
    public final @Nullable StreamCodec<ByteBuf, T> streamCodec;

    private ResourceLocation textureLocation = null;
    private ResourceLocation atlasLocation = null;
    private TextureAtlasSprite atlasSprite = null;
    private byte packedIndex = -1;

    public static final StreamCodec<ByteBuf, TransmitterType<?>> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TransmitterType<?> decode(ByteBuf buffer) {
            TransmitterType<?> type = TransmitterRegistry.INSTANCE.getRaw(buffer.readByte() + 128);
            if(type.streamCodec != null) type.streamCodec.decode(buffer);
            return type;
        }
        @Override 
        public void encode(ByteBuf buffer, TransmitterType<?> value) {
            buffer.writeByte(value.packedIndex);
        }
    };

    protected void onAddedToRegistry(int index) {
        if(this.packedIndex > -1) throw new IllegalStateException("Attempted to call registry add on TransmitterType that has already been added!");
        this.packedIndex = (byte)(index - 128);
    }

    protected TransmitterType(Supplier<T> defaultConstructor, StreamCodec<ByteBuf, T> streamCodec, CatenaryAttributes.Container attributes, ResourceLocation tex) {
        this.defaultConstructor = defaultConstructor;
        this.streamCodec = streamCodec;
        this.attributes = attributes;
        this.textureLocation = tex;
    }

    public int getIndex() {
        return packedIndex + 128;
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

    @Override
    public CatenaryAttributes.Container getCatenaryAttributes() {
        return this.attributes;
    }

    @OnlyIn(Dist.CLIENT)
    public ResourceLocation getTextureLocation() {
        return textureLocation != null ? textureLocation : CatenaryModelProvider.MISSING_TEX;
    }

    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("deprecation")
    public TextureAtlasSprite getAtlasSprite() {
        if(atlasSprite == null)
            atlasSprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(atlasLocation);
        return atlasSprite;
    }

    @OnlyIn(Dist.CLIENT)
    public RenderType getShader() {
        return getCatenaryAttributesOrThrow().getModelType().getMaterial(this, false);
    }

    @OnlyIn(Dist.CLIENT)
    public RenderType getShaderForChunkRendering() {
        return getCatenaryAttributesOrThrow().getModelType().getMaterial(this, true);
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
     * Transmitter's {@link #attributes catenary attributes}
     * @param model The model cooresponding to this TransmitterType,
     * as it is supplied by {@link net.minecraft.server.packs.resources.SimplePreparableReloadListener#apply}
     */
    @OnlyIn(Dist.CLIENT)
    public void applyResourceReloadResult(CatenaryModelProvider.ModelDefinition model) {
        this.textureLocation = model.getTexture();
        this.atlasLocation = model.getAtlas();
        this.atlasSprite = null;
    }

    @Override public void adjustSpan(LevelReader world, float length) {}
    @Override public float calculateSpan() { return -1; }

    public static class TransmitterTypeBuilder<T extends Transmitter<?>> {

        @Nullable private StreamCodec<ByteBuf, T> streamCodec = null;
        private final Supplier<T> defaultCtor;

        private CatenaryAttributes.Container attributes = new CatenaryAttributes.Container()
            .withModelTypeByOrdinal(5)
            .withThickness(CatenaryAttributes.Thickness.ZERO)
            .withMaterial(CatenaryAttributes.PhysicalMaterial.AIR);
        private ResourceLocation tex = null;

        public TransmitterTypeBuilder(Supplier<T> defaultCtor) {
            this.defaultCtor = defaultCtor;
        } 

        /**
         * Call this method while constructing your TransmitterType if you want this
         * particular type to serlaize additional data to a codec. You supply the codec - 
         * it would traditionally be located somewhere in your {@link Transmitter implementing subclass}
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
         * Define a custom set of {@link CatenaryAttributes.Container catenary attributes}
         * for the rendering pipeline of this TransmitterType's associated catenaries
         */
        public TransmitterTypeBuilder<T> withAttributes(CatenaryAttributes.Container attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Define a custom set of {@link CatenaryAttributes.Container catenary attributes}
         * for use in rendering and physical calculations. The attributes passed here determine
         * how the wire looks and behaves.
         */
        public TransmitterTypeBuilder<T> withAttributes(Consumer<CatenaryAttributes.Container> cons) {
            this.attributes = new CatenaryAttributes.Container();
            cons.accept(this.attributes);
            return this;
        }

        public TransmitterTypeBuilder<T> deferTextureTo(ResourceLocation loc) {
            this.tex = loc;
            return this;
        }

        public TransmitterType<T> build() {
            return new TransmitterType<T>(defaultCtor, streamCodec, attributes, tex);
        }
    }
}
