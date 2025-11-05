package com.quattage.mechano.foundation.tracking;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/**
     * Allows implementing classes to assert what kind 
     * of LinkData they store, and, by extension, where 
     * internal systems should look for retrieval. 
     * This class is used particularly in the {@link TrackedConstruct}
     * interface as a wway to allow {@link GridConnections} to
     * reassert their link data to allow catenaries to smoothly
     * hand off control and rendering context to/from all of the
     * {@link IAttachmentHolder} subclasses supported by {@link LinkDataStorage}.
     */
public enum DataScope implements StringRepresentable {
    UNKNOWN,
    STATIC_CHUNK,
    BLOCKENTITY,
    MOVING_ENTITY,                // any entity (usually players)
    MOVING_ENTITY_VOXEL_DOMAIN;   // contraptions

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
    @Override
    public String toString() {
        return getSerializedName();
    }
}
