package com.quattage.mechano.foundation.tracking;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.api.grid.Griddable;
import com.quattage.mechano.foundation.tracking.GridUUID.EntityUUID;
import com.quattage.mechano.foundation.tracking.GridUUID.VoxelUUID;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * A central class for managing the serialization and representation of various data sources.
 * Simultaneously represents a discriminator for {@link GridUUID} serialization, as well
 * as a loosely defined set of {@link Griddable} source types. Griddables can be attached to
 * {@link Entity entities}, {@link BlockEntity block entities}, or even the {@link LevelReader level}
 * (theoretically), and these all need ways to distinguish between one another, since these objects
 * are stored differently by the Level and have different needs.
 */
public enum UUIDSourceType implements StringRepresentable {
    
    VOXEL(VoxelUUID.class, BlockEntity.class),
    CHUNK(VoxelUUID.class, LevelChunk.class),
    CONTRAPTION(EntityUUID.class, AbstractContraptionEntity.class),
    ENTITY(EntityUUID.class, Entity.class);

    private static final String PREFIX = "type";

    public static final StreamCodec<? super RegistryFriendlyByteBuf, GridUUID> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GridUUID value) {
            buffer.writeInt(value.getSourceScope().ordinal());
            value.write(buffer);
        }
        @Override
        public GridUUID decode(RegistryFriendlyByteBuf buffer) {
            return UUIDSourceType.values()[buffer.readInt()].createUUID(buffer);
        }
    };

    public static final Codec<GridUUID> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<T> encode(GridUUID input, DynamicOps<T> ops, T prefix) {
            UUIDSourceType  type = input.getSourceScope();
            RecordBuilder<T> builder = ops.mapBuilder();
            builder.add(UUIDSourceType.PREFIX, type.ordinal(), Codec.INT);
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
            int ordinal = dyn.get(UUIDSourceType.PREFIX).asInt(-1);
            UUIDSourceType[] types = UUIDSourceType.values();
            if(ordinal < 0 || ordinal >= types.length)
                return DataResult.error(() -> "Ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
            UUIDSourceType datasource = types[ordinal];
            try {
                GridUUID newInstance = datasource.createUUID(dyn);
                return DataResult.success(Pair.of(newInstance, input));
            } catch (Exception e) {
                String message = "Unknown error occured while decoding UUID type '" + datasource + "'";
                Mechano.LOGGER.error(message);
                e.printStackTrace();
                return DataResult.error(() -> message);
            }
        }
    };

    public static void forEachSourceType(Consumer<UUIDSourceType> cons) {
        for(int x = 0; x < UUIDSourceType.values().length; x++) {
            UUIDSourceType type = UUIDSourceType.values()[x];
            cons.accept(type);
        }
    }

    public static CompoundTag write(GridUUID addr, CompoundTag tag) {
        tag.putInt(UUIDSourceType.PREFIX, addr.getSourceScope().ordinal());
        addr.write(tag);
        return tag;
    }

    public static ByteBuf write(GridUUID addr, ByteBuf buffer) {
        buffer.writeInt(addr.getSourceScope().ordinal());
        addr.write(buffer);
        return buffer;
    }

    public static void write(GridUUID addr, RecordBuilder<?> builder) {
        builder.add(UUIDSourceType.PREFIX, addr.getSourceScope().ordinal(), Codec.INT);
        addr.write(builder);
    }

    public static GridUUID read(CompoundTag tag) {
        int ordinal = tag.getInt(UUIDSourceType.PREFIX);
        UUIDSourceType[] types = UUIDSourceType.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + tag 
                +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(tag);
    }

    public static GridUUID read(ByteBuf buffer) {
        int ordinal = buffer.readInt();
        UUIDSourceType[] types = UUIDSourceType.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + buffer 
                + " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(buffer);
    }

    public static GridUUID read(Dynamic<?> dyn) {
        int ordinal = dyn.get(UUIDSourceType.PREFIX).asInt(-1);
        UUIDSourceType[] types = UUIDSourceType.values();
        if(ordinal < 0 || ordinal >= types.length) {
            throw new IllegalStateException("Discriminator couldn't determine type from " + dyn 
                +  " - ordinal '" + ordinal + "' is out of range for enum of length " + types.length);
        }
        return types[ordinal].createUUID(dyn);
    }

    /**
     * This class uses reflection, and so stores
     * references to the various constructors in each
     * {@link GridUUID} class. This method clears those
     * weakly-referenced constructors from the discriminator.
     */
    public static void clearReferences() {
        for(int x = 0; x < UUIDSourceType.values().length; x++)
            UUIDSourceType.values()[x].clear();
    }

    private final Class<? extends GridUUID> clazz; 
    private final Class<? extends IAttachmentHolder> referent;
    private WeakReference<Constructor<? extends GridUUID>> tagCtor = new WeakReference<>(null);;
    private WeakReference<Constructor<? extends GridUUID>> byteBufCtor = new WeakReference<>(null);
    private WeakReference<Constructor<? extends GridUUID>> dynamicCtor = new WeakReference<>(null);;

    UUIDSourceType(Class<? extends GridUUID> clazz, Class<? extends IAttachmentHolder> referent) {
        this.clazz = clazz;
        this.referent = referent;
    }

    public Class<? extends GridUUID> getUUIDClass() {
        return clazz;
    }

    public boolean permits(IAttachmentHolder holder) {
        return referent.isAssignableFrom(holder.getClass());
    }

    protected void clear() {
        byteBufCtor.clear();
        dynamicCtor.clear();
        tagCtor.clear();
    }

    private GridUUID createUUID(CompoundTag tag) {
        if(clazz == null) throw new NullPointerException("Cannot instantiate GridUUID '" + this + "' because this enum member hasn't been configured!");
        try {
            if(tagCtor.refersTo(null)) tagCtor = new WeakReference<>(clazz.getDeclaredConstructor(CompoundTag.class));
            return tagCtor.get().newInstance(tag);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + tag.getClass().getName());
        }
    }

    private GridUUID createUUID(ByteBuf buffer) {
        if(clazz == null) throw new NullPointerException("Cannot instantiate GridUUID " + this + " because this enum member hasn't been configured!");
        try {
            if(byteBufCtor.refersTo(null)) byteBufCtor = new WeakReference<>(clazz.getDeclaredConstructor(ByteBuf.class));
            return byteBufCtor.get().newInstance(buffer);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
            e.printStackTrace();
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this + " - Something went wrong!");
        } catch (InstantiationException | NoSuchMethodException e)  {
            throw new IllegalStateException("Couldn't instantiate GridUUID '" + this 
                + " - Class " + clazz.getName() + "' doesn't have a constructor that accepts a " + buffer.getClass().getName());
        }
    }

    private GridUUID createUUID(Dynamic<?> dyn) {
        if(clazz == null) throw new NullPointerException("Cannot instantiate GridUUID " + this + " because this enum member hasn't been configured!");
        try {
            if(dynamicCtor.refersTo(null)) dynamicCtor = new WeakReference<>(clazz.getDeclaredConstructor(Dynamic.class));
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
        UUIDSourceType getSourceScope();
    }
}