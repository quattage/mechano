package com.quattage.mechano.foundation.api.landmark;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.LinkDataStorage.DataScope;
import com.quattage.mechano.foundation.api.SidedGridDispatcher;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;
import com.quattage.mechano.foundation.api.switchboard.GridResponse;
import com.quattage.mechano.foundation.api.switchboard.LinkResponsePacket;
import com.quattage.mechano.foundation.api.transmitter.Transmitter;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public final class GridLink extends GridConnection {

    private GridNode start;
    private GridNode end;
    private float span;

    public GridLink(LevelReader world, GridNode start, GridNode end, Transmitter<?> trns, float span) {
        super(trns); 
        Objects.requireNonNull(world);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);

        start.assertNotDestroyed("Failed to instantiate GridLink with invalid starting GridNode at" + start.getAddress() + " - This node is invalid or has been previously destroyed!");
        end.assertNotDestroyed("Failed to instantiate GridLink with invalid ending GridNode at" + end.getAddress() + " - This node is invalid or has been previously destroyed!");
        if(start.getAddress().equals(end.getAddress()))
            throw new IllegalArgumentException("Can't instantiate a GridLink where both the start and end positions are the same!");
        if(!start.getOwner().equals(end.getOwner()))
            throw new IllegalStateException("Attempted to add two nodes that don't belong to the same grid, got start: " + start.getOwner() + ", and end: " + end.getOwner());
        this.start = start;
        this.end = end;
        this.span = span;
    }

    private GridLink(GridNode start, GridNode end, Transmitter<?> trns, float span) {
        super(trns);
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        this.start = start;
        this.end = end;
        this.span = span;
    }

    @Override
    public void broadast(ServerLevel world) { broadcast(world, GridResponse.TASK_CREATE_LINK); }

    @Override
    public void broadcast(ServerLevel world, GridResponse response) {
        start.broadcast(world, response);
        end.broadcast(world, response);
        switch(response) {
            case TASK_CREATE_LINK -> {
                start.getGriddable().onConnectionCreated(start.getOwner().getWorld(), this);
                end.getGriddable().onConnectionCreated(end.getOwner().getWorld(), this);
                trns.onConnectionCreated(end.getOwner().getWorld(), this);
            }
            case TASK_DESTROY_LINK, TASK_DESTROY_LINK_LAZY -> {
                start.getGriddable().onConnectionDestroyed(start.getOwner().getWorld(), this);
                end.getGriddable().onConnectionDestroyed(end.getOwner().getWorld(), this);
                trns.onConnectionDestroyed(end.getOwner().getWorld(), null, this);
            }
            case null, default -> {
                Mechano.LOGGER.error("Respose type '" + response + "' is unsupported for braodcasting");
                return;
            }
        }
        sendToClientsTracking(world, LinkResponsePacket.of(this, response));
    }

    @Override
    public GridLink inverseCopy() {
        return new GridLink(end, start, this.getTransmitter(), span);
    }

    @Override
    public CompoundTag writeTo(CompoundTag in) {
        UUIDDiscriminator.write(end.getAddress(), in);
        end.getAddress().writeTo(in);
        trns.getType().writeTo(in);
        writeSpan(in);
        if(trns.needsSerialization()) {
            CompoundTag extras = new CompoundTag();
            trns.writeTo(extras);
            in.put("data", extras);
        }
        return in;
    }

    public GridNode getStartNode() { return start; }
    public GridNode getEndNode() { return end; }
    @Override public GridUUID getStart() { return start.getAddress(); }
    @Override public GridUUID getEnd() { return end.getAddress(); }
    @Override public boolean isClientSide() { return false; }
    @Override public String describeConnectionType() { 
        return "GridLink(" + trns.getType() + ")";
    }

    @Override
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        throw new UnsupportedOperationException("Can't evalute frustum culling status of a server-sided GridLink! This method is only designed to work on clients!");
    }

    @Override
    public float calculateSpan() {
        return this.span;
    }

    @Override
    public void adjustSpan(LevelReader world, float length) {
        this.span = length;
        return;
    }

    @Override
    public String describeDataScope(LevelReader world) {
        if(!hasPoints()) return "Destroyed";
        return "ServerGrid '" + start.getOwner().getDimensionName() + "'";
    }

    @Override
    public DataScope getDataScope(LevelReader world) {
        return DataScope.SERVER_UNKNOWN;
    }

    @Override
    public IAttachmentHolder getDataStorageHolder(LevelReader world) {
        return getPrimaryConstruct(world).getDataStorageHolder(world);
    }

    @Override
    public void tick(LevelReader world) {
        if(!canMoveDynamically(world)) return;
        Vec3 start = getStart().getPos(world);
        Vec3 end = getEnd().getPos(world);
        PhysicalMaterial phys = getCatenaryAttributableOrThrow().getPhysicalMaterial(); 
        float force = GridConnection.simulateKinematics(this, phys, start, end, world);
        if(phys.isBreakable() && (force > phys.getMaxExertion())) {
            SidedGridDispatcher.server(world).destroyLink(getStart(), getEnd());
            getCatenaryAttributableOrThrow().playBreakEffect(world, this);
        }
    }

    @Override
    public float getMaximumSpan() {
        return involvesPlayer(start.getWorld()) ? trns.getType().getMaximumSpan() : span;
    }

    @Override
    public void setDataScope(DataScope scope) {
        return;
    }
}
