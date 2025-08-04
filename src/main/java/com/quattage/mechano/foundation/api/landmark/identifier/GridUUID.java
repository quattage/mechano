package com.quattage.mechano.foundation.api.landmark.identifier;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.switchboard.TrackedStreamable;
import com.quattage.mechano.foundation.helper.VectorHelper;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Barebones implementation template for hashables that need to reference
 * a block position and (optionally) an index value. 
 */
public abstract class GridUUID implements Comparable<GridUUID>, TrackedStreamable {

    public static final int MAX_SHARED_OCCUPANCY = 8; 

    public static int clampIndex(int index) { return clampIndex(index, true); }
    public static int clampIndex(int index, boolean warn) {
        int out = Math.max(0, Math.min(index, MAX_SHARED_OCCUPANCY - 1));
        if(warn && out != index) {
            Mechano.LOGGER.warn("Invalid GridUUID index '" + index 
                + "' was clamped to conform to range (0 -> " + MAX_SHARED_OCCUPANCY + ")");
        }
        return out;
    }

    @Override
    public abstract boolean isBeingTrackedBy(ServerPlayer player);

    public abstract UUIDDiscriminator getDiscriminatorType();
    public abstract BlockPos getBlockPos(LevelReader world);
    public abstract Vec3 getPos(LevelReader world);
    public abstract Vec3 getPos(LevelReader world, float pTicks);
    public abstract Vec3 getOffsetPos(LevelReader world, float ox, float oy, float oz);
    public abstract Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz);
    public abstract int getIndex();
    public final GridUUID copy() { return indexedCopy(getIndex()); }
    public abstract GridUUID indexedCopy(int index);
    public void applyForceToAttachment(LevelReader world, Vec3 force) { applyForceToAttachment(world, force, true); }
    public void applyForceToAttachment(LevelReader world, Vec3 force, boolean retainVelocity) {}
    public Vec3 getAttachmentVelocity(LevelReader world) { return Vec3.ZERO; }
    public void setAttachmentVelocity(LevelReader world, Vec3 vec) {}

    public abstract @Nullable AnchorPoint getAnchor(ClientLevel world);
    public abstract @Nullable Griddable<?> getAnchorPoints(LevelReader world);
    public abstract @Nullable SurrogateNode getSurrogate(LevelReader world);
    public abstract float getAttachedSizeFactor(LevelReader world);

    @Override
    public void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet) {
        MinecraftServer server = world.getServer();
        if(server == null)
            server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        for(ServerPlayer player : server.getPlayerList().getPlayers()) {
            if(isBeingTrackedBy(player))
                CatnipServices.NETWORK.sendToClient(player, packet);
        }
    }

    @Override
    public boolean isInsideOf(LevelReader world, ChunkPos chunk) {
        BlockPos pos = getBlockPos(world);
        return SectionPos.blockToSectionCoord(pos.getX()) == chunk.x 
            && SectionPos.blockToSectionCoord(pos.getZ()) == chunk.z;
    }

    @Override
    public boolean isInsideOf(LevelReader world, SectionPos section) {
        return SectionPos.of(getBlockPos(world)).equals(section);
    }

    @Override
    public int getSectionY(LevelReader world) {
        return SectionPos.blockToSectionCoord(getBlockPos(world).getY());
    }

    public boolean isAttachedToPlayer(LevelReader world) {
        return false;
    }

    public boolean isApproximately(LevelReader world, GridUUID that) {
        return VectorHelper.approxEqual(this.getPos(world), that.getPos(world));
    }

    public boolean isVeryApproximately(LevelReader world, GridUUID other) {
        return this.getBlockPos(world).equals(other.getBlockPos(world));
    }

    public boolean hasAnchorIn(ClientLevel world) {
        return getAnchor(world) != null;
    }

    public HeuristicUUID makeTrackable() {
        return new HeuristicUUID(this);
    }

    /**
     * Writes this NodeIdentifiable to a given CompoundTag. 
     * Enables this NodeIdentifiable to be serialized to as
     * arbitrary data in Entities or BlockEntities.
     * @param tag
     */
    public abstract void writeTo(CompoundTag tag);
    /**
     * Writes this NodeIdentifiable to a ButeBuf used by the 
     * {@link UUIDDiscriminator#STREAM_CODEC internal stream codec.}
     * Enables this NodeIdentifiable to be serialized to the network.
     * @param buffer
     */
    public abstract void writeTo(ByteBuf buffer);
    /**
     * Writes this NodeIdentifiable to a RecordBuilder
     * passed by the {@link UUIDDiscriminator#CODEC internal codec.}
     * Enables this NodeIdentifiable to be written to disk.
     * @param ops
     */
    public abstract void writeTo(RecordBuilder<?> builder);

    @Override
    public int compareTo(GridUUID o) {
        return getDiscriminatorType().compareTo(o.getDiscriminatorType());
    }

    public abstract boolean isUnindexed(GridUUID other);

    public String toString(LevelReader world) {
        return getDiscriminatorType() + "_uuid[" 
            + getBlockPos(world).getX() + ", " 
            + getBlockPos(world).getY() + ", " 
            + getBlockPos(world).getZ() + ", " 
            + getIndex() 
            + "]";
    }
}
