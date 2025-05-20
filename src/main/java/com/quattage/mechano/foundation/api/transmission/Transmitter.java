package com.quattage.mechano.foundation.api.transmission;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.foundation.api.PowerGrid;
import com.quattage.mechano.foundation.api.landmarks.GridLink;
import com.quattage.mechano.foundation.api.landmarks.GridNode;
import com.quattage.mechano.foundation.api.landmarks.GridPath;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterType;
import com.quattage.mechano.foundation.api.transmission.TransmitterRegistry.TransmitterTypeBuilder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A Transmitter describes the physical aspects
 * of the {@link GridLink} between two {@link GridNode GridNodes}. 
 * It provides a series of callbacks for responding to updates within
 * the {@link PowerGrid}, as well as an open-ended way to implement your own
 * transport paradigms. (like energy, redstone signals, items, etc.)
 */
public abstract class Transmitter<T extends Transmitter<?>> {

    // public static final StreamCodec<ByteBuf, Transmitter<?>> STREAM_CODEC = new StreamCodec<>() {
    //         @Override
    //         public Transmitter<?> decode(ByteBuf buffer) {
    //             TransmitterType<?> type = MechanoTransmissionTypes.REGISTRY.getRaw(buffer.readByte() + 128);
    //             if(type.streamCodec != null) type.streamCodec.decode(buffer);
    //             return type;
    //         }
    //         @Override 
    //         public void encode(ByteBuf buffer, Transmitter<?> value) {
    //             buffer.writeByte(value.packedIndex);
    //         }
    //     };

    /**
     * This method is the entrypoint for building TransmitterTypes for the {@link TransmitterRegistry}. 
     * This allows transmitters to attach, serialize, and stream arbitrary data via packets.
     * @param <T>
     * @param defaultCtor The constructor for your transmitter. It is expected to take a singular Byte value,
     * which is supplied internally to store the transmitter's index so that its associated type can be looked up later.
     * @return
     */
    public static <T extends Transmitter<?>> TransmitterTypeBuilder<T> builder(Supplier<T> defaultCtor) {
        return new TransmitterTypeBuilder<T>(defaultCtor);
    }

    protected boolean enabled = true;

    public void writeTo(CompoundTag in) {}
    public void loadFrom(CompoundTag in) {}


    public abstract TransmitterType<T> getType();

    /**
     * Tells internal systems whether or not this Transmitter serializes any extraneous data.
     * This data can be completely arbitrary. To actually do the serialization, override 
     * {@link Transmitter#writeTo writeTo} and {@link Transmitter#readFrom loadFrom}
     * @return <code>true</code> if this Transmitter serializes any additional data to NBT.
     */
    public abstract boolean needsSerialization();

    /**
     * A transmitter's cost can be thought of in literal terms as the resistance of a wire.
     * A high cost value means that energy is less inclined to use this transmitter when being 
     * transmitted. Energy will search for {@link GridLink GridLinks} with a lower cost over 
     * this one. (think 'path of least resistance') A cost of zero represents a perfect conductor.
     * @return A positive number representing this Transmitter's priority over others.
     */
    public abstract int getCost();

    /**
     * A callback for when a connection is about to be
     * made using this transmitter. This is useful for 
     * sequencing server-sided events related to this item. <p>

     * This method is called just before the provided {@link GridLink} is
     * validated and added to the relevent {@link PowerGrid}. This means
     * that this GridLink instance does not yet exist in the PowerGrid at the time of 
     * invocation. It's best not to store a reference to this instance anywhere, 
     * as it can easily be made stale by internal systems or by returning 
     * <code>false</code> here
     * 
     * Only called on the server.
     * 
     * @param world World to operate within
     * @param creator The player that created the connection
     * @param conection The connection that was made
     */
    public abstract void onConnectionCreated(Level world, GridLink connection); 

    /**
     * Called just after a connection is removed from its associated {@link PowerGrid}.
     * 
     * At the time of invocation, the provided {@link GridLink} instance has already
     * been removed from the grid, so it is guaranteed to be stale.
     * Useful for defining item drops for a destroyed wire. <p>
     *
     * Only called on the server.
     * 
     * @param world World to operate within
     * @param destroyer The player that destroyed the connection
     * @param connection The connection about to be destroyed. 
     * @return <code>true</code> if removing the connection should proceed
     */
    public abstract void onConnectionDestroyed(Level world, @Nullable Player destroyer, GridLink connection);

    /**
     * Compare this Transmitter to another based on arbitrary characteristics
     * defined by both. Transmitters are compared against one another to determine
     * which one represents the lowest "quality" connection in a {@link GridPath}.
     * 
     * Implementations should override this method if they intend to define some
     * kind of transfer rate or equivalent functionality - Lower transfer rates
     * should take priority over higher ones in order to create realistic bottlenecks.
     * 
     * By default, this implementation only compares the cost defined by
     * {@link Transmitter#getCost getCost} - highest cost wins.
     * @param other
     * @return
     */
    public Transmitter<?> compareTo(Transmitter<?> other) {
        if(this.enabled && !other.enabled) return this;
        if(!this.enabled  && other.enabled) return other;
        if(other.getCost() > this.getCost()) return other;
        return this;
    }

    /**
     * An arbitrary switch to turn this transmitter on/off.
     * Used by path indexing to determine whether or not 
     * paths that use this transmitter are traversible
     * @return <code>true</code> if this Transmitter is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof Transmitter that)) return false;
        return this.getType().equals(that.getType());
    }

    public boolean is(TransmitterType<?> type) {
        return this.hashCode() == type.hashCode();
    }

    public int hashCode() {
        return getType().hashCode();
    }
}
