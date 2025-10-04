package com.quattage.mechano.foundation.api.switchboard;

import java.util.Locale;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.RecordBuilder;
import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.api.Griddable;
import com.quattage.mechano.foundation.api.LinkDataStorage.DataScope;
import com.quattage.mechano.foundation.api.anchor.AnchorPoint;
import com.quattage.mechano.foundation.api.anchor.SurrogateNode;
import com.quattage.mechano.foundation.api.landmark.GridNode;
import com.quattage.mechano.foundation.api.landmark.identifier.GridUUID;
import com.quattage.mechano.foundation.api.landmark.identifier.UUIDDiscriminator;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public enum GridResponse implements StringRepresentable {

    TASK_CREATE_LINK                     (true, false),
    TASK_REASSERT_LINK                   (true, false),
    TASK_DESTROY_LINK                    (true, false),
    TASK_DESTROY_LINK_LAZY               (true, false),
    TASK_FREE_LINK                       (true, false),
    TASK_SYNC_SINGLE                     (true, false),
    TASK_SYNC_ANCHORS                    (true, false),
    TASK_FORGET_ANCHORS                  (true, false),
    TASK_SELECT_SUCCESS                  (true, false, HighlightMode.SHOW_SUCCESS),
    TASK_SWAP_START                      (true, false),
    TASK_SWAP_END                        (true, false),
    TASK_COMPLETED                       (true, false, HighlightMode.SHOW_SUCCESS),
    FAIL_INTERACTION_CANCELLED           (false, true),
    FAIL_DESTINATION_UNSUPPORTED         (false, true),
    FAIL_HELD_INCOMPATIBLE               (false, false),
    FAIL_DESTINATION_FULL                (false, false),
    FAIL_DUPLICATE                       (false, true),
    FAIL_TOO_CLOSE                       (false, true),
    FAIL_TOO_FAR                         (false, false),
    FAIL_DIM_MISMATCH                    (false, true),
    FAIL_START_MISSING                   (false, true),
    FAIL_END_MISSING                     (false, true),
    FAIL_BOTH_ENDS_MISSING               (false, true),
    FAIL_CATENARY_NOT_FOUND              (false, true),
    FAIL_GENERIC                         (false, true, HighlightMode.SHOW_FAILURE),
    NONE                                 (false, false, HighlightMode.SHOW_PASSIVE);

    public static final StreamCodec<ByteBuf, GridResponse> STREAM_CODEC = new StreamCodec<>() {
        @Override public GridResponse decode(ByteBuf buffer) { return GridResponse.values()[buffer.readByte()]; }
        @Override public void encode(ByteBuf buffer, GridResponse value) { buffer.writeByte(value.ordinal()); }
    };

    public static void logUnhandled(@Nullable GridResponse response, @Nullable Object o) {
        Mechano.LOGGER.error(("Response type '" + response + "' is not handled ") 
            + o == null ? "!" : (" by handler in '" + o.getClass().getSimpleName() + "'!"));
    }

    private final boolean isTask;
    private final boolean shouldFailHard;
    private final HighlightMode mode;
    

    private GridResponse(boolean isTask, boolean shouldFailHard, HighlightMode mode) { 
        this.isTask = isTask; 
        this.shouldFailHard = shouldFailHard;
        this.mode = mode;
    }

    private GridResponse(boolean isTask, boolean shouldFailHard) {
        this.isTask = isTask; 
        this.shouldFailHard = shouldFailHard;
        this.mode = HighlightMode.HIDE;
    }

    /**
     * Indicates whether or not this response represents the completion
     * of a task.
     * @return If <code>false</code>, this task represents a failure.
     */
    public boolean indicatesCompletion() {
        return isTask;
    }

    /**
     * Indicates whether this response represents a hard or soft failure mode.
     * @return If <code>true</code>, implementations should reset connection progress.
     */
    public boolean shouldFailHard() {
        return shouldFailHard;
    }

    /**
     * @return the {@link HighlightMode} associated with this response
     */
    public HighlightMode getVisibility() {
        return mode; 
    }

    @Override
    public String getSerializedName() {
        return "response_" + name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }

    public ResourceLocation getKey() {
        return Mechano.asResource(getSerializedName());
    }

    /**
     * A minified version of the {@link GridNode} that can be serialized
     * to and from a StreamCodec for sending in packets. This class is used
     * in the {@Link AnchorSyncPacket}, which can be sent from this class
     * with the {@link #sendToClients helper method.} 
     */
    public static class AnchorSynchronizer extends GridUUID {

        private final GridUUID addr;
        private final byte connections;
        private final boolean enabled;

        public static boolean assertAnchorsExist(AnchorPoint startAnchor, AnchorPoint endAnchor) {
            if(startAnchor == null && endAnchor == null) {
                Mechano.LOGGER.warn("Assertion failed - Couldn't find starting or ending AnchorPoints for link (" + startAnchor + " -> " + endAnchor + ")");
                return false;
            } if(startAnchor == null) {
                Mechano.LOGGER.warn("Assertion failed - Couldn't find starting AnchorPoint for link (" + startAnchor + " -> " + endAnchor + ")");
                return false;
            } if(endAnchor == null) {
                Mechano.LOGGER.warn("Assertion failed - Couldn't find starting AnchorPoint for link (" + startAnchor + " -> " + endAnchor + ")");
                return false;
            }
            return true;
        }

        public static AnchorSynchronizer of(GridNode node) {
            return new AnchorSynchronizer(node, true);
        }

        public static AnchorSynchronizer of(GridUUID addr) {
            return new AnchorSynchronizer(addr, Byte.MIN_VALUE, true);
        }

        public static AnchorSynchronizer of(GridUUID addr, int connections) {
            return new AnchorSynchronizer(addr, (byte)(connections - 128), true);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, AnchorSynchronizer> STREAM_CODEC = StreamCodec.composite(
            UUIDDiscriminator.STREAM_CODEC, AnchorSynchronizer::getAddr,
            ByteBufCodecs.BYTE, AnchorSynchronizer::getConnections,
            ByteBufCodecs.BOOL, AnchorSynchronizer::isEnabled,
            AnchorSynchronizer::new
        );

        private AnchorSynchronizer(GridUUID addr, byte connections, boolean enabled) {
            this.addr = addr;
            this.connections = connections;
            this.enabled = enabled;
        }

        private AnchorSynchronizer(GridNode node, boolean enabled) {
            this.addr = node.getAddress();
            this.connections = node.hasLinks() ? (byte)(node.getLinkCount() - 128) : Byte.MIN_VALUE;
            this.enabled = enabled;
        }

        public void sendToClients(ServerLevel world) {
            addr.sendToClientsTracking(world, new AnchorSyncPacket(this));
        }

        private GridUUID getAddr() {
            return addr;
        }

        private byte getConnections() {
            return connections;
        }

        private boolean isEnabled() {
            return enabled;
        }

        public @Nullable AnchorPoint applyAndGet(Level world) { return applyAndGet(world, false); }
        public @Nullable AnchorPoint applyAndGet(Level world, boolean log) {
            Objects.requireNonNull(world);
            if(!world.isClientSide())
                throw new IllegalStateException("Cannot apply AnchorSyncHolder on a server-sided world!");
            Griddable<?> points = addr.getOrFindGriddable(world);
            if(points == null) {
                if(log) {
                    Mechano.LOGGER.error("Failed to apply AnchorSyncHolder to AnchorPoint at " 
                        + addr + " - No Griddable could be found at this address!");
                }
                return null;
            }
            AnchorPoint point = points.getAnchor(addr.getIndex());
            if(point == null) {
                if(log) {
                    Mechano.LOGGER.error("Failed to apply AnchorSyncHolder to AnchorPoint at " + addr 
                        + " - An Griddable could be found, but it has no AnchorPoint at the required index! (" + addr + ")");
                }
                return null;
            }
            point.setConectionCount(connections);
            point.setEnabled(enabled);
            SurrogateNode surrogate = points.getSurrogate();
            if(surrogate == null) {
                if(log) {
                    Mechano.LOGGER.error("Failed apply AnchorSyncHolder to AnchorPoint at " + addr 
                        + " - Couldn't locate a valid sorrogate node belonging to the AnchorPoint at this address!");
                }
                return null;
            }
            if(point.getCurrentConnections() > 0)
                surrogate.sync(world, null);
            else surrogate.forgetIfNeeded(world);
            points.onAnchorSynced(world, getIndex());
            return point;
        }

        // implementation deferred to internal address for convenience
        @Override public @Nullable AnchorPoint getAnchor(ClientLevel world) { return addr.getAnchor(world); }
        @Override public @Nullable Griddable<?> getOrFindGriddable(LevelReader world) { return addr.getOrFindGriddable(world); }
        @Override public @Nullable SurrogateNode getSurrogate(LevelReader world) { return addr.getSurrogate(world); }
        @Override public @Nullable IAttachmentHolder getDataStorageHolder(LevelReader world) { return addr.getDataStorageHolder(world); }
        @Override public boolean isInFrustum(LevelReader world, @NotNull Frustum view) { return addr.isInFrustum(world, view); }
        @Override public boolean isBeingTrackedBy(ServerPlayer player) { return addr.isBeingTrackedBy(player); }
        @Override public boolean canMoveDynamically(LevelReader world) { return addr.canMoveDynamically(world); }
        @Override public int getIndex() { return addr.getIndex(); }
        @Override public float getMass(LevelReader world) { return addr.getMass(world); }
        @Override public UUIDDiscriminator getDiscriminatorType() { return addr.getDiscriminatorType(); }
        @Override public GridUUID indexedCopy(int index) { return addr.indexedCopy(index); }
        @Override public BlockPos getBlockPos(LevelReader world) { return addr.getBlockPos(world); }
        @Override public String describeDataScope(LevelReader world) { return addr.describeDataScope(world) + " (Queried from AnchorSyncHolder)"; }
        @Override public Vec3 getPos(LevelReader world) { return addr.getPos(world); }
        @Override public Vec3 getPos(LevelReader world, float pTicks) { return addr.getPos(world, pTicks); }
        @Override public Vec3 getOffsetPos(LevelReader world, float ox, float oy, float oz) { return addr.getOffsetPos(world, ox, oy, oz); }
        @Override public Vec3 getOffsetPos(LevelReader world, float pTicks, float ox, float oy, float oz) { return addr.getOffsetPos(world, pTicks, ox, oy, oz); }
        @Override public void writeTo(CompoundTag tag) { throw new UnsupportedOperationException("AnchorSyncHolders cannot be written directly!"); }
        @Override public void writeTo(ByteBuf buffer) { throw new UnsupportedOperationException("AnchorSyncHolders cannot be written directly!"); }
        @Override public void writeTo(RecordBuilder<?> tag) { throw new UnsupportedOperationException("AnchorSyncHolders cannot be written directly!"); }
        @Override public void setDataScope(DataScope scope) { addr.setDataScope(scope); }
        @Override public DataScope getDataScope(LevelReader world) { return addr.getDataScope(world); }
        @Override public void sendLevelUpdates(Level world) { addr.sendLevelUpdates(world); }
        @Override public String toString() { return addr.toString(); }
        @Override public boolean isUnindexed(GridUUID other) { return addr.isUnindexed(other); }
        public GridUUID getAddress() { return addr; }
        @Override
        public int getPriority() { return addr.getPriority(); }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof GridUUID that) 
                return this.addr.equals(that);
            return false;
        }
        
        @Override
        public int hashCode() {
            return addr.hashCode();
        }
    }


    public static enum HighlightMode {
        /**
         * Shows the vanilla-style black outline around the targeted AnchorPoint
         */
        SHOW_PASSIVE,
        /**
         * Shows a green AABB drawn by Create's outliner
         */
        SHOW_SUCCESS,
        /**
         * Shows a red AABB drawn by Create's outliner
         */
        SHOW_FAILURE,
        /**
         * Shows nothing at all
         */
        HIDE;

        public boolean isVisible() {
            return this != HIDE;
        }

        public boolean isHighlighted() {
            return this == SHOW_SUCCESS || this == SHOW_FAILURE;
        }
    }
}
