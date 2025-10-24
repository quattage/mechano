package com.quattage.mechano.api.anchor;

import static com.quattage.mechano.Mechano.lang;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.quattage.mechano.api.LinkDataStorage.DataScope;
import com.quattage.mechano.api.entity.GriddableEntityAttachment;
import com.quattage.mechano.api.griddable.Griddable;
import com.quattage.mechano.api.identifier.ContraptionUUID;
import com.quattage.mechano.api.identifier.EntityUUID;
import com.quattage.mechano.api.identifier.GridUUID;
import com.quattage.mechano.api.switchboard.TrackedConstruct;
import com.quattage.mechano.api.transmitter.TransmitterType;
import com.quattage.mechano.foundation.block.orientation.CombinedOrientation;
import com.quattage.mechano.foundation.block.orientation.DirectionTransformer;
import com.quattage.mechano.foundation.block.orientation.OrientationUpdatable;
import com.quattage.mechano.foundation.math.VectorOperations;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/**
 * An AnchorPoint is the client-sided mirror implementation of the
 * {@link com.quattage.mechano.api.landmark.GridNode GridNode},
 * built specifically to store transformation and hitbox data in
 * world-space.
 */
@OnlyIn(Dist.CLIENT)
public class AnchorPoint implements TrackedConstruct, OrientationUpdatable {

    private GridUUID address;
    private byte[] data;
    private boolean enabled;
    private Vector3f offset;
    public int bitmask;

    @OnlyIn(Dist.CLIENT)
    public static @Nullable AnchorPoint getLocal(@Nullable Player player) {
        if(player == null) player = Minecraft.getInstance().player;
        Griddable<?> points = GriddableEntityAttachment.of(player, true);
        if(points == null) return null;
        AnchorCollection anchors = points.getAnchors();
        if(anchors == null) return null;
        AnchorPoint out = anchors.get(0);
        return out != null && out.hasRoom() ? out : null;
    }

    public AnchorPoint(GridUUID addr, float px, float py, float pz, float size, boolean enabled, int maxc) {
        Objects.requireNonNull(addr);
        this.address = addr;
        this.data = new byte[]{
            packMeasurement(px),
            packMeasurement(py),
            packMeasurement(pz),
            packMeasurement(Math.max(0.001f, size)),
            Byte.MIN_VALUE,
            (byte)(Math.max(0, Math.min(maxc, 255)) - 128)
        };
        this.offset = getRaw();
        this.enabled = enabled;
        this.bitmask = 0xFFFFFFFF;
    }

    public AnchorPoint initializeFrom(CompoundTag tag) {
        this.data = tag.getByteArray("data");
        this.enabled = tag.getBoolean("e");
        this.bitmask = tag.getInt("bm");
        return this;
    }

    public void setConectionCount(byte connections) {
        this.data[4] = connections;
    }

    public int getIndex() {
        return address.getIndex();
    }

    public boolean existsIn(LevelReader world) {
        if(world == null || !world.isClientSide()) return false;
        return address.hasAnchorIn((ClientLevel)world);
    }

    public Vec3 getPos(LevelReader world) {
        return getPos(world, 1);
    }

    public Vec3 getPos(LevelReader world, float pTicks) {
        return address.getOffsetPos(world, pTicks, offset.x, offset.y, offset.z);
    }

    /**
     * @return The raw Vec3 offset of this AnchorPoint from its BlockPos
     * @see {@link #getRealPosition} to get the actual in-world position of this AnchorPoint
     */
    public Vec3 getOffset() {
        if(getAddress() instanceof ContraptionUUID cuid) {
            BlockPos cpos = cuid.getContraptionOffset();
            return new Vec3(cpos.getX() + offset.x, cpos.getY() + offset.y, cpos.getZ() + offset.z);
        }
        return new Vec3(offset.x, offset.y, offset.z);
    }

    /**
     * This method is used to determine whether or not the provided transfer transmitter
     * can be used with this AnchorPoint. Useful for enforcing tiers or types
     * of wires/connectors that should coorespond with one another.
     * @param tfp Transmitter to compare
     * @return <code>true</code> if the provided TFP can interact with this AnchorPoint
     */
    public boolean isCompatableWith(TransmitterType<?> type) {
        if(!type.getCatenaryAttributableOrThrow().shouldApplyRestrictions()) return true;
        return (bitmask & type.bitmask()) != 0;
    }

    private Vector3f getRaw() {
        return new Vector3f(unpackMeasurement(data[0]) / 16f, unpackMeasurement(data[1]) / 16f, unpackMeasurement(data[2]) / 16f);
    }

    public float getSize() {
        return unpackMeasurement(data[3]) / 16f;
    }

    /** 
     * Check whether or not this AnchorPoint can support more connections
     * @return <code>true</code> if <code>current connections < max connections</code>
     * @see {@link AnchorPoint#getCurrentConnections()}
     * @see {@link AnchorPoint#getMaxConnections()}
     */
    public boolean hasRoom() {
        return getCurrentConnections() < getMaxConnections();
    }

    /**
     * @return The amount of connections that this AnchorPoint is currently pointsing
     */
    public int getCurrentConnections() {
        return data[4] + 128;
    }

    /**
     * @return The maximum possible amount of connections that this AnchorPoint could points
     */
    public int getMaxConnections() {
        return data[5] + 128;
    }

    /**
     * Sets the current number of connections pointsed by this
     * AnchorPoint to zero.
     */
    public void resetCurrentConnections() {
        data[4] = Byte.MIN_VALUE;
    }

    @Override
    public void updateOrientation(CombinedOrientation dir) {
        offset = VectorOperations.rotate(getRaw(), dir);
    }

    private static float unpackMeasurement(byte in) {
        return (in + 128) * (48f / 255f) - 16f;
    }

    private static byte packMeasurement(float in) {
        return (byte)((Math.max(-16, Math.min(in, 32)) + 16f) * (255f / 48f) - 128f);
    }

    /**
     * Builds a new AABB based on this hitbox's current offset 
     * and size. <p>
     * Note that this hitbox may be out of date if the AnchorPoint has moved.
     * To ensure that this is not the case, a call to {@link AnchorPoint#updateOrientation}
     * should be made at some point to reflect the change to this AnchorPoint's position.
     * @param useSize if <code>false<code>, the returned AABB will have a size of 0.
     * @return A new AABB describing this AnchorPoint's hitbox
     */
    public AABB makeHitbox(LevelReader world, boolean useSize, float pTicks) {
        float size = useSize ? getSize() : 0;
        BlockPos pos = address.getBlockPos(world);
        return new AABB(
            (pos.getX() + offset.x) - size,
            (pos.getY() + offset.y) - size,
            (pos.getZ() + offset.z) - size,
            (pos.getX() + offset.x) + size,
            (pos.getY() + offset.y) + size,
            (pos.getZ() + offset.z) + size
        );
    }

    /**
     * Evaluates whether or not this AnchorPoint is attached to any Player entity
     * @param world
     * @return <code>true</code> if this AnchorPoint's address points to a Player entity instance
     */
    public boolean belongsToPlayer(LevelReader world) {
        if(!(address instanceof EntityUUID euid)) return false;
        Griddable<?> points = euid.getOrFindGriddable(world);
        if(points == null) return false;
        return points.getSource() instanceof Player;
    }


    public boolean isEnabled() {
        return this.enabled;
    }

    public GridUUID getAddress() {
        return address;
    }

    public float distanceTo(LevelReader world, AnchorPoint other) {
        return (float)getPos(world, 1).distanceTo(other.getPos(world, 1));
    }

    public float distanceTo(Player player) {
        return (float)player.getEyePosition().distanceTo(getPos(player.level(), 1));
    }

    /**
     * Enables/disables this AnchorPoint. A disabled AnchorPoint
     * is hidden from view and cannot be interacted with by the player.
     * Use this method as a way to update this AnchorPoint to reflect 
     * BlockState or BlockEntity changes that may visually or functionally 
     * obscure AnchorPoints. 
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Writes information stored in this AnchorPoint to a tooltip string
     * for display to the player
     * @param tooltip
     */
    public void writeInfoToTooltip(List<Component> tooltip) {
        lang().text(getCurrentConnections() + "/" + getMaxConnections()).forGoggles(tooltip);;
    }

    /**
     * Performs an intersection test with this 
     * AnchorPoint's hitbox to determine whether
     * the player is targeting this AnchorPoint
     * @param ray Raycast to test
     * @return <code>true</code> if <code>ray</code> is intersecting this AnchorPoint
     */
    public boolean isIntersecting(LevelReader world, VectorOperations.Ray ray) {
        return makeHitbox(world, true, 1).clip(ray.start, ray.end).isPresent();
    }

    public CompoundTag writeTo(CompoundTag tag) {
        tag.putByteArray("data", data);
        tag.putBoolean("e", enabled);
        tag.putInt("bm", bitmask);
        return tag;
    }

    @Override
    public String toString() {
        return address == null ? "AnchorPoint[NULL ADDRESS]" : "AnchorPoint[" + address + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return address.equals(obj);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public void sendToClientsTracking(ServerLevel world, CustomPacketPayload packet) {
        throw new UnsupportedOperationException("AnchorPoints can't send packets since they're client-sided only!");
    }

    @Override
    public boolean isBeingTrackedBy(ServerPlayer player) {
        throw new UnsupportedOperationException("AnchorPoints can't evaluate tracking since they're client-sided only!");
    }

    @Override
    public boolean isInsideOf(LevelReader world, ChunkPos chunk) {
        return address.isInsideOf(world, chunk);
    }

    @Override
    public boolean isInsideOf(LevelReader world, SectionPos section) {
        return address.isInsideOf(world, section);
    }

    @Override
    public int getSectionY(LevelReader world) {
        return address.getSectionY(world);
    }

    @Override
    public void sendLevelUpdates(Level world) {
        address.sendLevelUpdates(world);
    }

    @Override
    public boolean isInFrustum(LevelReader world, @NotNull Frustum view) {
        return address.isInFrustum(world, view);
    }

    @Override
    public IAttachmentHolder getDataStorageHolder(LevelReader world) {
        return address.getDataStorageHolder(world);
    }

    public void replaceAddress(GridUUID newAddress) {
        this.address = newAddress;
    }

    @Override
    public String describeDataScope(LevelReader world) {
        return address.describeDataScope(world);
    }

    @Override
    public DataScope getDataScope(LevelReader world) {
        return address.getDataScope(world);
    }

    @Override
    public int getPriority() {
        return address.getPriority();
    }

    @Override
    public float getMass(LevelReader world) {
        return address.getMass(world);
    }


    /**
     * A fluent-ish builder for instantiating AnchorPoints in BlockEntities
     */
    public static class Builder<T extends AnchorCollection> {

        private float offx = 0f;
        private float offy = 0f;
        private float offz = 0f;
        private float size = 2;
        private boolean enabled;
        private int max = 2;
        private final T destination;

        public Builder(T prev) {
            Objects.requireNonNull(prev);
            this.destination = prev;
            this.enabled = true;
        }

        /**
         * The position of this anchor relative to the northern bottom corner of the block. <p>
         * This is based on pixel measurements (out of 16) rather than raw vectors. For example, 
         * if you wanted to place a node on the center of the block, you would use:
         * <pre> AnchorPointBuilder.at(8, 8, 8); </pre> 
         * <strong>Some things to note when placing anchors:</strong>
         * <ul>
         * <li>The coordinate system used by anchors mirrors the json model data structure, so you can copy coordinates directly from Blockbench.</li>
         * <li>The coordinates you supply must be in the range of (-16, 32)</li>
         * <li>Half-pixel measurements are permitted, but may be {@link AnchorPoint#packMeasurement(float) quantized unpredictably} when the anchor is constructed.</li>
         * <li>The anchor's hitbox can be partially obscured by the block itself, but not entirely, or it won't be selectable by the player.</li>
         * <li>The position you supply is automatically rotated along with the block it belongs to, so coordinates should always be supplied assuming the block is in its default blockstate.</li>
         * </ul>
         * @param x 
         * @param y 
         * @param z 
         * @return
         */
        public Builder<T> at(float x, float y, float z) {
            this.offx = x;
            this.offy = y;
            this.offz = z;
            return this;
        }

        /**
         * Radius (in pixels, between 0 and 32) of the hitbox and anchor cuboid
         * @param size
         * @return
         */
        public Builder<T> radius(float size) {
            this.size = size;
            return this;
        }

        /**
         * The maximum amount of connections that this
         * anchor can support at the same time, up to 256.
         * @param max
         * @return
         */
        public Builder<T> connections(int max) {
            this.max = max;
            return this;
        }

        /**
         * <code>true</code> if this anchor is instantiated with its
         * "enabled" value set to false by default, which will make it
         * invisible and disable interactions with it.
         * @return
         */
        public Builder<T> hiddenByDefault() {
            this.enabled = false;
            return this;
        }

        /**
         * Adds the AnchorPoint 
         */
        public T addTo(Griddable<?> points) {
            Objects.requireNonNull(points);
            GridUUID addr = points.createSupplementaryAddress();
            AnchorPoint newAnchor = make(addr.indexedCopy(destination.size()));
            destination.add(newAnchor);
            return destination;
        }

        /**
         * Adds a new AnchorPoint by copying the address
         * from the previously added AnchorPoint. 
         */
        public T add() {
            if(destination.isEmpty())
                throw new IllegalStateException("Couldn't add AnchorPoint with piggybacking add operation becuase the backing collection contained no prevously instantiated AnchorPoints!");
            AnchorPoint last = destination.get(destination.size() - 1);
            AnchorPoint newAnchor = make(last.getAddress().indexedCopy(destination.size()));
            destination.add(newAnchor);
            return destination;
        }

        protected AnchorPoint make(GridUUID override) {
            Objects.requireNonNull(override);
            return new AnchorPoint(override, offx, offy, offz, size, enabled, max);
        }
    }
}
