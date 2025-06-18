package com.quattage.mechano.foundation.api.landmark.classifier;

import java.lang.ref.WeakReference;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.MechanoData;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.AnchorPointable;
import com.quattage.mechano.foundation.api.anchor.DispatchedAnchorNode;
import com.quattage.mechano.foundation.api.landmark.DiscriminatorData;
import com.quattage.mechano.foundation.helper.VectorHelper;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class EntityUUID extends GridUUID {

    private UUID uuid;
    private int index;
    private WeakReference<Entity> entityRef = new WeakReference<>(null);

    public EntityUUID(UUID uuid, int index) {
        this.uuid = uuid;
        this.index = index;
    }

    public EntityUUID(CompoundTag tag) {
        this.uuid = tag.getUUID("uid");
        this.index = tag.getByte("i");
    }

    public EntityUUID(ByteBuf buffer) {
        this.uuid = new UUID(buffer.readLong(), buffer.readLong());
        this.index = buffer.readByte();
    }

    public EntityUUID(Dynamic<?> dyn) {
        this.uuid = new UUID(dyn.get("uida").asLong(0), dyn.get("uidb").asLong(0));
        this.index = dyn.get("i").asInt(0);
    }

    @Override
    public @Nullable AnchorPoint getAnchor(ClientLevel world) {
        Entity owner = world.entityStorage.getEntityGetter().get(uuid);
        if(owner == null) return null;
        AnchorPointable holder = owner.getCapability(MechanoData.ANCHOR_CAPABILITY);
        if(holder == null) return null;
        return holder.getAnchor(index);
    }
    @Override
    public @Nullable AnchorPointable getHolder(LevelReader world) {
        Entity owner = null;
        if(world instanceof ClientLevel cl) owner = cl.entityStorage.getEntityGetter().get(uuid);
        else if(world instanceof ServerLevel sl) owner = sl.getEntity(uuid);
        if(owner == null) return null;
        return owner.getCapability(MechanoData.ANCHOR_CAPABILITY);
    }

    @Override
    public @Nullable DispatchedAnchorNode getSurrogate(LevelReader world) {
        Entity owner = null;
        if(world instanceof ClientLevel cl) owner = cl.entityStorage.getEntityGetter().get(uuid);
        else if(world instanceof ServerLevel sl) owner = sl.getEntity(uuid);
        if(owner == null) return null;
        AnchorPointable holder = owner.getCapability(MechanoData.ANCHOR_CAPABILITY);
        return holder == null ? null : holder.getSurrogate();
    }

    @OnlyIn(Dist.CLIENT)
    public Entity getEntity() {
        ClientLevel world = Minecraft.getInstance().level;
        if(world == null)
            throw new IllegalStateException("Can't get Entity at '" + this + "' - This EntityUUID has not been loaded into a world!");
        return world.entityStorage.getEntityGetter().get(uuid);
    }


    @Override
    public GridUUID indexedCopy(int index) {
        return new EntityUUID(this.uuid, index);
    }

    @Override
    public DiscriminatorData getDiscriminatorType() {
        return DiscriminatorData.ENTITY;
    }

    @Override
    public BlockPos getBlockPos(LevelReader world) {
        return VectorHelper.toBlockPos(getEntity().getPosition(1));
    }

    @Override
    public Vec3 getPos(LevelReader world) {
        return getPos(world, 1);
    }

    @Override
    public Vec3 getPos(LevelReader world, float pTicks) {
        return getEntity().getPosition(pTicks);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float ox, float oy, float oz) {
        return getOffsetPos(world, ox, oy, oz);
    }

    @Override
    public Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz) {
        return getEntity().getRopeHoldPosition(pTicks);
    }

    @Override
    public int getIndex() {
        return index;
    }

    public Entity getEntity(LevelReader world) {
        if(entityRef.refersTo(null)) {
            Entity e = null;
            if(world.isClientSide())
                e = ((ClientLevel)world).entityStorage.getEntityGetter().get(uuid);
            else e = ((ServerLevel)world).getEntity(uuid);
            if(e == null)
                throw new IllegalStateException("Couldn't find entity at " + uuid + "!");
            entityRef = new WeakReference<>(null);
        }
        return entityRef.get();
    }

    @Override
    public void writeTo(CompoundTag tag) {
        tag.putUUID("uid", uuid);
        tag.putByte("i", (byte)index);
    }

    @Override
    public void writeTo(ByteBuf buffer) {
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
        buffer.writeByte((byte)index);
    }

    @Override
    public void writeTo(RecordBuilder<?> builder) {
        builder.add("uida", uuid.getMostSignificantBits(), Codec.LONG);
        builder.add("uidb", uuid.getLeastSignificantBits(), Codec.LONG);
        builder.add("i", (byte)index, Codec.BYTE);
    }
}
