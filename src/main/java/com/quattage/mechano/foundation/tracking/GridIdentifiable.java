package com.quattage.mechano.foundation.tracking;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.ApiStatus;

import com.quattage.mechano.api.Grid;
import com.quattage.mechano.api.grid.Griddable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelReader;

public interface GridIdentifiable<T extends GridUUID> {

    // TODO make this use a stream instead because this could have really bad iteration performance in worst case scenarios
    static Set<ServerPlayer> collectTrackers(ServerLevel world, Collection<GridIdentifiable<?>> objs) {
        Set<ServerPlayer> senders = new HashSet<>();
        for(ServerPlayer sp : world.getServer().getPlayerList().getPlayers()) {
            for(GridIdentifiable<?> id : objs) {
                Griddable<?> source = id.getTargetSource(world);
                if(source == null) continue;
                if(source.isBeingTrackedBy(sp)) {
                    senders.add(sp);
                    break;
                }
            }
        }
        return senders;
    }

    static Set<ServerPlayer> collectTrackers(ServerLevel world, GridIdentifiable<?>... objs) {
        Set<ServerPlayer> senders = new HashSet<>();
        for(ServerPlayer sp : world.getServer().getPlayerList().getPlayers()) {
            for(GridIdentifiable<?> id : objs) {
                Griddable<?> source = id.getTargetSource(world);
                if(source == null) continue;
                if(source.isBeingTrackedBy(sp)) {
                    senders.add(sp);
                    break;
                }
            }
        }
        return senders;
    }

    /**
     * Provides a (new or pre-existing) {@link GridUUID} instance 
     * that points towards this object. Can be used by the {@link Grid}
     * to look this object up. <p>
     * For API users: Use {@link #getUUIDSafe() the checked version} 
     * of this method instead.
     * @return The UUID associated with this identifiable object.
     * @see #getUUIDSafe()
     */
    T getUUID();

    /**
     * Provides a (new or pre-existing) {@link GridUUID} instance 
     * that points towards this object. Can be used by the {@link Grid}
     * to look this object up. <p>
     * This method will throw exceptions for null or invalid returns.
     * @return The UUID associated with this identifiable object. Will never be <code>null</code>
     */
    @ApiStatus.NonExtendable
    default T getUUIDSafe() {
        T uuid = getUUID();
        if(uuid == null) 
            throw new NullPointerException("GridIdentifiable '" + this.getClass().getSimpleName() + " failed to provide a vlaid UUID! (got " + uuid + ")");
        return uuid;
    }

    Griddable<?> getTargetSource(LevelReader world);
}
