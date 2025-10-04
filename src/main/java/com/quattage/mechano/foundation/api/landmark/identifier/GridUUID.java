package com.quattage.mechano.foundation.api.landmark.identifier;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.switchboard.TrackedConstruct;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Barebones implementation template for hashables that parent themselves
 * to an arbitrary construct, like a block or an entity
 */
public abstract class GridUUID implements Comparable<GridUUID>, TrackedConstruct {

    public static final int MAX_SHARED_OCCUPANCY = 8; 

    public static boolean areAsymmetricallyEqual(GridUUID thisStart, GridUUID thisEnd, GridUUID thatStart, GridUUID thatEnd) {
        return (thisStart.equals(thatStart) && thisEnd.equals(thatEnd)) 
            || (thisStart.equals(thatEnd) && thisEnd.equals(thatStart));
    }

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
    public Vec3 getPos(LevelReader world) { return getPos(world, 1); }
    public abstract Vec3 getPos(LevelReader world, float pTicks);
    // TODO remove this method in favor of explicitly defined offsets for entity AnchorPoints
    public Vec3 getOffsetPos(LevelReader world, float ox, float oy, float oz) { return getOffsetPos(world, 1, ox, oy, oz); }
    public abstract Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz);
    public abstract int getIndex();
    public final GridUUID copy() { return indexedCopy(getIndex()); }
    public abstract GridUUID indexedCopy(int index);
    
    /**
     * Applies the given force to the construct (e.g. block, entity, contraption, etc)
     * that this UUID refers to. This method should only be called on the logical server. <p>
     * If this method is called on a UUID that isn't attached to a movable
     * construct, e.g. a block, this method will not do anything. Additionally,
     * due to how Minecraft handles player movement, this method will not apply
     * forces to ServerPlayers.
     * @param world World to operate within
     * @param force force to apply
     * @param retainVelocity (Optional, defaults to <code>true</code>) - If <code>true</code> 
     * the force will be added to the current velocity, if <code>false</code> the force will replace the current velocity
     */
    public void applyForceToAttachment(LevelReader world, Vector3f force) { applyForceToAttachment(world, force, true); }
    
    /**
     * Applies the given force to the construct (e.g. block, entity, contraption, etc)
     * that this UUID refers to. This method should only be called on the logical server. <p>
     * If this method is called on a UUID that isn't attached to a movable
     * construct, e.g. a block, this method will not do anything. Additionally,
     * due to how Minecraft handles player movement, this method will not apply
     * forces to ServerPlayers.
     * @param world World to operate within
     * @param force force to apply
     * @param retainVelocity (Optional, defaults to <code>true</code>) - If <code>true</code> 
     * the force will be added to the current velocity, if <code>false</code> the force will replace the current velocity
     */
    public void applyForceToAttachment(LevelReader world, Vector3f force, boolean retainVelocity) {}

    /**
     * Gets the current velocity of the construct (e.g. block, entity, contraption, etc)
     * that this UUID refers to. If this UUID is not attached to a movable construct,
     * or the movable construct could not be found, this method will return {@link Vec3#ZERO}
     * @param world World to operate within
     * @return {@link Vec3} velocity of the attachment construct
     */
    public Vec3 getAttachmentVelocity(LevelReader world) { return Vec3.ZERO; }

    /**
     * Gets the {@link AnchorPoint} associated with this UUID. If no AnchorPoint
     * could be found (e.g. the UUID is invalid, the block/entity has been removed,
     * or the AnchorPoint does not exist) this method will return null.
     * @param world
     * @return {@link AnchorPoint} at this UUID
     */
    @OnlyIn(Dist.CLIENT) 
    public abstract @Nullable AnchorPoint getAnchor(ClientLevel world);
    public abstract @Nullable Griddable<?> getOrFindGriddable(LevelReader world);
    public abstract @Nullable SurrogateNode getSurrogate(LevelReader world);

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
    
    @Override
    public int getPriority() {
        return getDiscriminatorType().ordinal();
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
