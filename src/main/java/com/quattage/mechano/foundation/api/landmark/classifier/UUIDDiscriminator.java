package com.quattage.mechano.foundation.api.landmark.classifier;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.Locale;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.registries.DeferredHolder;

public enum UUIDDiscriminator implements StringRepresentable {

    // do not reorder these or god will smite you
    VOXEL(VoxelUUID.class),
    ENTITY(EntityUUID.class),
    DOMAIN(null),
    WORLDLY(null);

    private static final String PREFIX = "type";

    public static final Codec<byte[]> B64 = Codec.STRING.xmap(
        s -> Base64.getDecoder().decode(s),
        bytes -> Base64.getEncoder().encodeToString(bytes)
    );

    public static final StreamCodec<? super RegistryFriendlyByteBuf, GridUUID> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GridUUID value) {
            buffer.writeInt(value.getDiscriminatorType().ordinal());
            value.writeTo(buffer);
        }
        @Override
        public GridUUID decode(RegistryFriendlyByteBuf buffer) {
            return UUIDDiscriminator.values()[buffer.readInt()].instantiate(buffer);
        }
    };

    public static final Codec<GridUUID> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<T> encode(GridUUID input, DynamicOps<T> ops, T prefix) {
            UUIDDiscriminator  type = input.getDiscriminatorType();
            RecordBuilder<T> builder = ops.mapBuilder();
            builder.add(PREFIX, type.ordinal(), Codec.INT);
            try {
                input.writeTo(builder);
            } catch (Exception e) {
                String message = "Unknown error occured while encoding UUID type '" + type + "'";
                Mechano.LOGGER.error(message);
                e.printStackTrace();
                return DataResult.error(() -> message);
            }
            return builder.build(prefix);
        }
        @Override
        public <T> DataResult<Pair<GridUUID, T>> decode(DynamicOps<T> ops, T input) {
            Dynamic<T> dyn = new Dynamic<>(ops, input);
            int ordinal = dyn.get(PREFIX).asInt(-1);
            UUIDDiscriminator[] types = UUIDDiscriminator.values();
            if(ordinal < 0 || ordinal >= types.length)
                return DataResult.error(() -> "Ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            UUIDDiscriminator type = types[ordinal];
            try {
                GridUUID newInstance = type.instantiate(dyn);
                return DataResult.success(Pair.of(newInstance, input));
            } catch (Exception e) {
                String message = "Unknown error occured while decoding UUID type '" + type + "'";
                Mechano.LOGGER.error(message);
                e.printStackTrace();
                return DataResult.error(() -> message);
            }
        }
    };

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GridUUID>> ATTACHMENT = 
        MechanoData.COMPONENT_REGISTRY.registerComponentType(
            "grid_identifier",
            b -> b.persistent(UUIDDiscriminator.CODEC).networkSynchronized(UUIDDiscriminator.STREAM_CODEC)
    );

    public static CompoundTag write(GridUUID addr, CompoundTag tag) {
        tag.putInt(PREFIX, addr.getDiscriminatorType().ordinal());
        addr.writeTo(tag);
        return tag;
    }

    public static ByteBuf write(GridUUID addr, ByteBuf buffer) {
        buffer.writeInt(addr.getDiscriminatorType().ordinal());
        addr.writeTo(buffer);
        return buffer;
    }

    public static void write(GridUUID addr, RecordBuilder<?> builder) {
        builder.add(PREFIX, addr.getDiscriminatorType().ordinal(), Codec.INT);
        addr.writeTo(builder);
    }

    public static GridUUID read(CompoundTag tag) {
        int ordinal = tag.getInt(PREFIX);
        UUIDDiscriminator[] types = UUIDDiscriminator.values();
        if(ordinal < 0 || ordinal >= types.length) {
            Mechano.LOGGER.error("Discriminator couldn't determine type from " + tag +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            return types[0].instantiate(tag);
        }
        return types[ordinal].instantiate(tag);
    }

    public static GridUUID read(ByteBuf buffer) {
        int ordinal = buffer.readInt();
        UUIDDiscriminator[] types = UUIDDiscriminator.values();
        if(ordinal < 0 || ordinal >= types.length) {
            Mechano.LOGGER.error("Discriminator couldn't determine type from " + buffer +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            return types[0].instantiate(buffer);
        }
        return types[ordinal].instantiate(buffer);
    }

    public static GridUUID read(Dynamic<?> dyn) {
        int ordinal = dyn.get(PREFIX).asInt(-1);
        UUIDDiscriminator[] types = UUIDDiscriminator.values();
        if(ordinal < 0 || ordinal >= types.length) {
            Mechano.LOGGER.error("Discriminator couldn't determine type from " + dyn +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            return types[0].instantiate(dyn);
        }
        return types[ordinal].instantiate(dyn);
    }


    public static void clearReferences() {
        for(int x = 0; x < values().length; x++) {
            UUIDDiscriminator type = values()[x];
            type.byteBufCtor = null;
            type.dynamicCtor = null;
            type.tagCtor = null;
        }
    }

    private final Class<? extends GridUUID> clazz; 
    private WeakReference<Constructor<? extends GridUUID>> byteBufCtor = new WeakReference<>(null);
    private WeakReference<Constructor<? extends GridUUID>> dynamicCtor = new WeakReference<>(null);;
    private WeakReference<Constructor<? extends GridUUID>> tagCtor = new WeakReference<>(null);;

    private <R extends GridUUID> UUIDDiscriminator(Class<R> clazz) {
        this.clazz = clazz;
    }

    private GridUUID instantiate(CompoundTag tag) {
        try {
            if(tagCtor.refersTo(null))
                tagCtor = new WeakReference<>(clazz.getDeclaredConstructor(CompoundTag.class));
            return tagCtor.get().newInstance(tag);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate IdentifiableType '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate IdentifiableType '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + tag.getClass().getName());
        }
    }

    private GridUUID instantiate(ByteBuf buffer) {
        try {
            if(byteBufCtor.refersTo(null))
                byteBufCtor = new WeakReference<>(clazz.getDeclaredConstructor(ByteBuf.class));
            return byteBufCtor.get().newInstance(buffer);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate IdentifiableType '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate IdentifiableType '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + buffer.getClass().getName());
        }
    }

    private GridUUID instantiate(Dynamic<?> dyn) {
        try {
            if(dynamicCtor.refersTo(null))
                dynamicCtor = new WeakReference<>(clazz.getDeclaredConstructor(Dynamic.class));
            return dynamicCtor.get().newInstance(dyn);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate IdentifiableType '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate IdentifiableType '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + dyn.getClass().getName());
        }
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }

    
}
