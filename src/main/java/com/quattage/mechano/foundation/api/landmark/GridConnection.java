package com.quattage.mechano.foundation.api.landmark;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.quattage.mechano.foundation.api.LinkDataStorage.DataScope;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.catenary.CatenaryAttributable;
import com.quattage.mechano.foundation.api.landmark.GridConnection.ConnectionKey;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.switchboard.TrackedConstruct;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;
import com.quattage.mechano.foundation.helper.VectorHelper;

import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public abstract sealed class GridConnection implements TrackedConstruct, CatenaryAttributable permits GridLink, GridCatenary, ConnectionKey {

    protected final @NotNull Transmitter<?> trns;

    public abstract GridUUID getStart();
    public abstract GridUUID getEnd();
    public abstract boolean isClientSide();

    public static short span2Short(float span) {
        return (short)(Mth.clamp(span * 50f, 0, 65535) - Short.MAX_VALUE);
    }



    /**
     * Applies forces to the endpoints that the provided {@link GridConnection} instance
     * is attached to. This method is used to simulate the kinematic effects of tension
     * using Hooke's law. Forces applied, by default, are scaled by the relative size 
     * of each end's hitbox, and by whether or not the endpoint is capable of moving
     * in the first place (for example, an {@link AnchorPoint}) attached to a BlockEntity
     * cannot move.
     * @param link link to grab kinematic data from
     * @param mat {@Link Physicalmaterial} to use for force calculations
     * @param start starting point of the provided link
     * @param end ending point of the provided link
     * @param world world to operate within
     * @return float magnitude representing the total force applied to both endpoints
     */
    public static float simulateKinematics(GridConnection link, PhysicalMaterial mat, @Nullable Vec3 start, @Nullable Vec3 end, LevelReader world) {

        if(start == null || end == null) return 0;
        if(!mat.exertsForce()) return 0;
        Vector3f diff = start.subtract(end).toVector3f();
        float maxSpan = link.getMaximumSpan();
        float span = diff.length();
        if(span < link.getMaximumSpan()) return 0;

        // hooke's law (mostly sortof idk)
        float forceMagnitude = ((maxSpan - span) / maxSpan) * mat.getReboundForce();
        
        diff.normalize();
        float wA = link.getStart().getMass(world);
        float wB = link.getEnd().getMass(world);
        Vector3f sForce = diff.mul(-Math.abs(forceMagnitude * (wB / (wA + wB))), new Vector3f());
        // Vector3f sForceN = sForce.normalize(new Vector3f());
        Vector3f eForce = diff.mul(-Math.abs(forceMagnitude * (wA / (wB + wA))), new Vector3f());
        // Vector3f eForceN = eForce.normalize(new Vector3f());

        float sfM = sForce.length() * 20;
        if(sfM > 0.01f) link.getStart().applyForceToAttachment(world, sForce, true);
        float efM = eForce.length() * 20;
        if(efM > 0.01f) link.getEnd().applyForceToAttachment(world, eForce, true);

        if(world.isClientSide()) {
            VectorHelper.drawDebugBox(start, end);
            VectorHelper.drawDebugRay(start, sForce.normalize(new Vector3f()).mul(10), Color.RED, "RSF");
            VectorHelper.drawDebugRay(end, eForce.normalize(new Vector3f()).mul(10), Color.PURPLE, "REF");
        }

        return sfM + efM;
    }

    public static float getEuclideanDistance(LevelReader world, GridUUID a, GridUUID b) {
        Vec3 aPos = a.getPos(world);
        Vec3 bPos = b.getPos(world);
        return (float)Math.sqrt(
            Math.pow(aPos.x - bPos.x, 2) +
            Math.pow(aPos.y - bPos.y, 2) +
            Math.pow(aPos.z - bPos.z, 2)
        );
    }

    public static void sendToClientsTracking(ServerLevel world, GridUUID start, @Nullable GridUUID end, CustomPacketPayload packet, InsertionPolicy mode) {
        MinecraftServer server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        if(mode == InsertionPolicy.SYMMETRIC) {
            for(ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(start.isBeingTrackedBy(player)) {
                    if(end == null) {
                        CatnipServices.NETWORK.sendToClient(player, packet);
                        continue;
                    }
                    if(end.isBeingTrackedBy(player))
                        CatnipServices.NETWORK.sendToClient(player, packet);
                }
            }
            return;
        }
        if(mode == InsertionPolicy.SINGLE) {
            for(ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(start.isBeingTrackedBy(player))
                    CatnipServices.NETWORK.sendToClient(player, packet);
                else if(end != null && end.isBeingTrackedBy(player))
                    CatnipServices.NETWORK.sendToClient(player, packet);
            }
            return;
        }
        if(mode == InsertionPolicy.ORDERED) {
            TrackedConstruct priority = TrackedConstruct.orderedByAssertionPriority(world, start, end).first();
            for(ServerPlayer player : server.getPlayerList().getPlayers()) {
                if(priority.isBeingTrackedBy(player))
                    CatnipServices.NETWORK.sendToClient(player, packet);
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported InsertionPolicy '" + mode + "'");
    }

    public GridConnection(Transmitter<?> trns) {
        this.trns = trns;
    }

    public GridConnection fixDataScopes(LevelReader world) {
        return fixDataScopes(world, false);
    }

    public GridConnection fixDataScopes(LevelReader world, boolean save) {
        DataScope startScope = this.getStart().getDataScope(world);
        DataScope endScope = this.getEnd().getDataScope(world);
        if(save) {
            if(startScope == DataScope.BLOCKENTITY)
                this.getStart().setDataScope(DataScope.STATIC_CHUNK);
            if(endScope == DataScope.BLOCKENTITY)
                this.getEnd().setDataScope(DataScope.STATIC_CHUNK);
        } else {
            if(startScope == DataScope.STATIC_CHUNK && endScope != DataScope.STATIC_CHUNK)
                this.getStart().setDataScope(DataScope.BLOCKENTITY);
            if(startScope != DataScope.STATIC_CHUNK && endScope == DataScope.STATIC_CHUNK)
                this.getEnd().setDataScope(DataScope.BLOCKENTITY);
        }
        return this;
    }

    @Override
    public int getSectionY(LevelReader world) {
        if(!hasPoints()) throw new IllegalStateException("Can't get sectionY for connection with null point(s)!");
        TrackedConstruct primary =  getPrimaryConstruct(world);
        return primary.getSectionY(world);
    }

    @Override
    public IAttachmentHolder getDataStorageHolder(LevelReader world) {
        assertWorldly(world);
        if(!hasPoints()) throw new IllegalStateException("Can't get data storage holder for connection with null point(s)");
        return getStart().getDataStorageHolder(world);
    }

    public boolean isBeingTrackedBy(ServerPlayer player, InsertionPolicy mode) {
        if(mode == InsertionPolicy.SINGLE) return getStart().isBeingTrackedBy(player) || getEnd().isBeingTrackedBy(player);
        if(mode == InsertionPolicy.SYMMETRIC) return getStart().isBeingTrackedBy(player) && getEnd().isBeingTrackedBy(player);
        return false;
    }

    /**
     * Ensures that the given world is side-matched with this Connection instance.
     * Throws if this is not the case.
     */
    public void assertWorldly(LevelReader world) {
        if(world.isClientSide() == this.isClientSide()) return;
        if(world.isClientSide() && !this.isClientSide()) {
            throw new IllegalArgumentException("Sided mismatch encountered while removing " 
                + this + " - Server-sided links cannot be added to client holders!");
        } 
        if(!world.isClientSide() && this.isClientSide()) {
            throw new IllegalArgumentException("Sided mismatch encountered while removing " 
                + this + " - Client-sided links cannot be added to server holders!");
        }
    }

    /** 
     * @return <code>true</code> if this Connection's start and end points are not <code>null.</code>
     */
    public boolean hasPoints() {
        return getStart() != null && getEnd() != null;
    }

    /**
     * Creates a shallow copy of this GridConnection retaining all internal 
     * data, but with its start and end addresses swapped.
     * @return A new GridConnection instance
     */
    public abstract GridConnection inverseCopy();
    public abstract CompoundTag writeTo(CompoundTag in);
    public abstract String describeConnectionType();
    public boolean canTraverse() { return getStart() != null && getEnd() != null && trns != null && trns.isEnabled(); }
    public Transmitter<?> getTransmitter() { return trns; }
    public boolean startsWith(GridUUID address) { return getStart().equals(address); }
    public boolean endsWith(GridUUID address) { return getEnd().equals(address); }
    public boolean involves(GridUUID address) { return startsWith(address) || endsWith(address); }


    public boolean involvesPlayer(LevelReader world) {
        boolean startP = getStart() != null && getStart().getDataStorageHolder(world) instanceof Player;
        boolean endP = getEnd() != null && getEnd().getDataStorageHolder(world) instanceof Player;
        return startP || endP;
    }

    @Override
    public void sendLevelUpdates(Level world) {
        // if(!hasPoints() || getStart().canMoveDynamically() || getEnd().canMoveDynamically()) 
        //     return;
        // if(getStart().isInsideOf(world, new ChunkPos(getEnd().getBlockPos(world)))) {
        //     getEnd().sendChunkUpdates(world);
        //     return;
        // }
        getStart().sendLevelUpdates(world);
        getEnd().sendLevelUpdates(world);
    }

    public float calculateTraversalCost() {
        // TODO traversal cost should vary depending on whether or not
        // the pathfinding gets closer or further away from the target
        if(!canTraverse()) return Float.MAX_VALUE;
        return Math.max(0, calculateSpan() + trns.getCost());
    }

    /**
     * Updates visuals and kinematic stuff related to this GridConnection.
     * On the server, calling this method will apply constraints to attached entities.
     * On the client, this method constructs the mesh for rendering.
     * @param world 
     */
    public void tick(LevelReader world) {}

    /**
     * Gets the primary renderer/hoster for this GridConnection
     * according to the {@link TrackedConstruct#orderedByAssertionPriority assertion priority}
     * rules.
     * @param world
     * @return
     */
    public TrackedConstruct getPrimaryConstruct(LevelReader world) {
        return TrackedConstruct.orderedByAssertionPriority(world, getStart(), getEnd()).first();
    }

    @Override
    @OnlyIn(Dist.CLIENT) 
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        return hasPoints() ? (getStart().isInFrustum(world, view) && getEnd().isInFrustum(world, view)) : false;
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        return hasPoints() ? (getStart().isBeingTrackedBy(player) || getEnd().isBeingTrackedBy(player)) : false;
    }

    @Override
    public void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet) {
        if(world.isClientSide() || isClientSide()) throw new IllegalArgumentException("Attempted to send a client-bound packet as a client! YOU CAN'T DO THAT!!!!");
        sendToClientsTracking(world, getStart(), getEnd(), packet, InsertionPolicy.SYMMETRIC);
    }

    @Override
    public boolean isInsideOf(LevelReader world, SectionPos section) {
        return hasPoints() ? (getStart().isInsideOf(world, section) || getEnd().isInsideOf(world, section)) : false;
    }

    @Override
    public boolean isInsideOf(LevelReader world, ChunkPos chunk) {
        return hasPoints() ? (getStart().isInsideOf(world, chunk) || getEnd().isInsideOf(world, chunk)) : false;
    }

    @Override
    public int getPriority() {
        if(!hasPoints()) return -1;
        return getStart().getPriority() + getEnd().getPriority();
    }

    @Override
    public float getMass(LevelReader world) {
        if(!hasPoints()) return TrackedConstruct.DEFAULT_MASS;
        return getStart().getMass(world) + getEnd().getMass(world);
    }

    @Override
    public boolean equals(Object other) {
        if(other == this) return true;
        if(!(other instanceof GridConnection that)) return false;
        return GridUUID.areAsymmetricallyEqual(this.getStart(), this.getEnd(), that.getStart(), that.getEnd());
    }

    @Override
    public int hashCode() { 
        return getStart().hashCode() + getEnd().hashCode(); 
    }

    @Override
    public String toString() { 
        return describeConnectionType() + "[" + getStart() + " -> " + getEnd() + "]"; 
    }

    @Override
    public Container getCatenaryAttributable() {
        return trns.getType().getCatenaryAttributable();
    }

    public Vec3 getMiddlePos(LevelReader world) {
        if(!hasPoints()) return Vec3.ZERO;
        Vec3 startPos = getStart().getPos(world);
        Vec3 endPos = getEnd().getPos(world);
        return new Vec3((startPos.x + endPos.x) / 2d, (startPos.y + endPos.y) / 2d, (startPos.z + endPos.z) / 2d);
    }

    public BlockPos getMiddleBlockPos(LevelReader world) {
        Vec3 middle = getMiddlePos(world);
        return VectorHelper.toBlockPos(middle);
    }

    public static final class ConnectionKey extends GridConnection {

        private final GridUUID start, end;
        public ConnectionKey(GridUUID start, GridUUID end) {
            super(null);
            this.start = start;
            this.end = end;
        }

        @Override public GridConnection inverseCopy() { return new ConnectionKey(end, start); }
        @Override public GridUUID getStart() { return start; }
        @Override public GridUUID getEnd() { return end; }
        @Override public boolean isClientSide() { return false; }
        @Override public CompoundTag writeTo(CompoundTag in) { return in; }
        @Override public String describeConnectionType() { return "ConnectionKey"; }
        @Override public void adjustSpan(LevelReader world, float length) { return; }

        @Override
        public float calculateSpan() {
            return -1;
        }

        @Override
        public float getMaximumSpan() {
            return -1;
        }

        @Override
        public String describeDataScope(LevelReader world) {
            return "Key";
        }

        @Override
        public DataScope getDataScope(LevelReader world) {
            return DataScope.MOVING_ENTITY;
        }

        @Override
        public void setDataScope(DataScope scope) {
            return;
        }

        @Override
        public int getPriority() {
            return -1;
        }
    }

    public static enum InsertionPolicy { SINGLE, SYMMETRIC, ORDERED; }
}
