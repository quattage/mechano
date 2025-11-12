package com.quattage.mechano.foundation.tracking;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.tracking.GridUUID.EntityUUID;
import com.quattage.mechano.foundation.tracking.GridUUID.VoxelUUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.neoforged.neoforge.registries.DeferredHolder;

public enum DataSourceIdentifier implements StringRepresentable {
    
    VOXEL(VoxelUUID.class),
    ENTITY(EntityUUID.class),
    CHUNK(null),
    LEVEL(null);

    private static final String PREFIX = "type";

    public static final StreamCodec<? super RegistryFriendlyByteBuf, GridUUID> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GridUUID value) {
            buffer.writeInt(value.getSourceScope().ordinal());
            value.write(buffer);
        }
        @Override
        public GridUUID decode(RegistryFriendlyByteBuf buffer) {
            return DataSourceIdentifier.values()[buffer.readInt()].instantiate(buffer);
        }
    };

    public static final Codec<GridUUID> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<T> encode(GridUUID input, DynamicOps<T> ops, T prefix) {
            DataSourceIdentifier  type = input.getSourceScope();
            RecordBuilder<T> builder = ops.mapBuilder();
            builder.add(DataSourceIdentifier.PREFIX, type.ordinal(), Codec.INT);
            try { input.write(builder); } catch (Exception e) {
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
            int ordinal = dyn.get(DataSourceIdentifier.PREFIX).asInt(-1);
            DataSourceIdentifier[] types = DataSourceIdentifier.values();
            if(ordinal < 0 || ordinal >= types.length)
                return DataResult.error(() -> "Ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            DataSourceIdentifier type = types[ordinal];
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
            b -> b.persistent(DataSourceIdentifier.CODEC).networkSynchronized(DataSourceIdentifier.STREAM_CODEC)
    );

    public static CompoundTag write(GridUUID addr, CompoundTag tag) {
        tag.putInt(DataSourceIdentifier.PREFIX, addr.getSourceScope().ordinal());
        addr.write(tag);
        return tag;
    }

    public static ByteBuf write(GridUUID addr, ByteBuf buffer) {
        buffer.writeInt(addr.getSourceScope().ordinal());
        addr.write(buffer);
        return buffer;
    }

    public static void write(GridUUID addr, RecordBuilder<?> builder) {
        builder.add(DataSourceIdentifier.PREFIX, addr.getSourceScope().ordinal(), Codec.INT);
        addr.write(builder);
    }

    public static GridUUID read(CompoundTag tag) {
        int ordinal = tag.getInt(DataSourceIdentifier.PREFIX);
        DataSourceIdentifier[] types = DataSourceIdentifier.values();
        if(ordinal < 0 || ordinal >= types.length) {
            Mechano.LOGGER.error("Discriminator couldn't determine type from " + tag +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            return types[0].instantiate(tag);
        }
        return types[ordinal].instantiate(tag);
    }

    public static GridUUID read(ByteBuf buffer) {
        int ordinal = buffer.readInt();
        DataSourceIdentifier[] types = DataSourceIdentifier.values();
        if(ordinal < 0 || ordinal >= types.length) {
            Mechano.LOGGER.error("Discriminator couldn't determine type from " + buffer +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            return types[0].instantiate(buffer);
        }
        return types[ordinal].instantiate(buffer);
    }

    public static GridUUID read(Dynamic<?> dyn) {
        int ordinal = dyn.get(DataSourceIdentifier.PREFIX).asInt(-1);
        DataSourceIdentifier[] types = DataSourceIdentifier.values();
        if(ordinal < 0 || ordinal >= types.length) {
            Mechano.LOGGER.error("Discriminator couldn't determine type from " + dyn +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            return types[0].instantiate(dyn);
        }
        return types[ordinal].instantiate(dyn);
    }


    public static void clearReferences() {
        for(int x = 0; x < DataSourceIdentifier.values().length; x++)
            DataSourceIdentifier.values()[x].clear();
    }

    private final Class<? extends GridUUID> clazz; 
    private WeakReference<Constructor<? extends GridUUID>> tagCtor = new WeakReference<>(null);;
    private WeakReference<Constructor<? extends GridUUID>> byteBufCtor = new WeakReference<>(null);
    private WeakReference<Constructor<? extends GridUUID>> dynamicCtor = new WeakReference<>(null);;

    <R extends GridUUID> DataSourceIdentifier(Class<R> clazz) {
        this.clazz = clazz;
    }

    protected void clear() {
        byteBufCtor.clear();
        dynamicCtor.clear();
        tagCtor.clear();
    }

    private GridUUID instantiate(CompoundTag tag) {
        try {
            if(tagCtor.refersTo(null))
                tagCtor = new WeakReference<>(clazz.getDeclaredConstructor(CompoundTag.class));
            return tagCtor.get().newInstance(tag);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this 
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
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this 
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
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this 
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













    public static interface ScopeSpecifier {
        DataSourceIdentifier getSourceScope();
    }
}