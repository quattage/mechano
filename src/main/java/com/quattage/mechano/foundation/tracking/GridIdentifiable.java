package com.quattage.mechano.foundation.tracking;

import org.jetbrains.annotations.ApiStatus;

import com.quattage.mechano.api.Grid;

public interface GridIdentifiable<T extends GridUUID> {

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
        if(uuid.hasBindings() || uuid == null) 
            throw new NullPointerException("GridIdentifiable '" + this.getClass().getSimpleName() + " failed to provide a vlaid UUID!");
        return uuid;
    }
}
