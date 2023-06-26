package com.quattage.mechano.core.nbt;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/**
 * The TagManager class represents a map of CompoundTags and a boolean value. The Boolean value keeps track of 
 * whether or not the tag needs to be synced between the client and the server. It's sort of a hacky way to avoid having to write
 * S2C packets just for clients to pull relevent data for things like observe UIs or renderers.
 */
public class TagManager {

    private boolean syncableChanged = false;
    private final CompoundTag data;
    private final List<String> syncedTags = new ArrayList<String>();
    
    public TagManager(CompoundTag nbt) {
        data = nbt;
    }

    public TagManager() {
        data = new CompoundTag();
    }   

    /***
     * If any syncable value has been added or updated
     * @return True if any syncable value has changed.
     */
    public boolean needsSyncing() {
        return !syncedTags.isEmpty() && syncableChanged;
    }


    /***
     * Adds a new tag to this TagManger via direct instance of a Tag object.
     * If you intend to add simple primitive values, use {@link #put(String, int) put(String, value)} instead
     * @param id The ID of this tag
     * @param tag The tag to add
     * @param needsSynced Whether or not to sync this tag from the server to the client
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, Tag tag, boolean needsSynced) {
        if(data.contains(id)) {
            data.put(id, tag);
            if(needsSynced) {
                syncedTags.add(id);
                syncableChanged = true;
            }
            return false;
        }
        data.put(id, tag);
        if(needsSynced) {
            syncedTags.add(id);
            syncableChanged = true;
        }
        return true;
    }


    /***
     * Adds a new int to the stored CompoundTag
     * @param id The ID of this tag
     * @param value The value to add
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, int value) {
        return put(id, IntTag.valueOf(value), false);
    }

    /***
     * Adds a new String to the stored CompoundTag
     * @param id The ID of this tag
     * @param value The value to add
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, String value) {
        return put(id, StringTag.valueOf(value), false);
    }

    /***
     * Adds a new float to the stored CompoundTag
     * @param id The ID of this tag
     * @param value The value to add
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, float value) {
        return put(id, FloatTag.valueOf(value), false);
    }

    /***
     * Adds a new boolean to the stored CompoundTag
     * @param id The ID of this tag
     * @param value The value to add
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, boolean value) {
        return put(id, ByteTag.valueOf(value), false);
    }


    /***
     * Adds a new int to the stored CompoundTag
     * @param id The ID of this tag
     * @param value The value to add
     * @param synced Whether or not to sync this tag from the server to the client
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, int value, boolean synced) {
        return put(id, IntTag.valueOf(value), synced);
    }

    /***
     * Adds a new String to the stored CompoundTag
     * @param id The ID of this tag
     * @param value The value to add
     * @param synced Whether or not to sync this tag from the server to the client
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, String value, boolean synced) {
        return put(id, StringTag.valueOf(value), synced);
    }

    /***
     * Adds a new float to the stored CompoundTag
     * @param id The ID of this tag
     * @param value The value to add
     * @param synced Whether or not to sync this tag from the server to the client
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, float value, boolean synced) {
        return put(id, FloatTag.valueOf(value), synced);
    }

    /***
     * Adds a new boolean to the stored CompoundTag
     * @param id The ID of this tag
     * @param value The value to add
     * @param synced Whether or not to sync this tag from the server to the client
     * @return True if this tag was added new, False if this tag was updated.
     */
    public boolean put(String id, boolean value, boolean synced) {
        return put(id, ByteTag.valueOf(value), synced);
    }

    public boolean isSynced(String id) {
        return syncedTags.contains(id);
    }

    public int getInt(String id) {
        return data.getInt(id);
    }

    public String getString(String id) {
        return data.getString(id);
    }

    public boolean getBool(String id) {
        return data.getBoolean(id);
    }

    public float getFloat(String id) {
        return data.getFloat(id);
    }

    /***
     * Adds only SyncableTags stored in this TagManager to the given CompoundTag
     * @param nbt CompoundTag to transform
     * @param reset Set to true to unmark all Syncable tags now that they've been updated
     * @return the transformed CompoundTag
     */
    public CompoundTag writeSyncable(CompoundTag nbt, boolean reset) {
        for(String key : data.getAllKeys()) {
            if(syncedTags.contains(key))
                nbt.put(key, data.get(key));
        }
        if(reset && syncableChanged) syncableChanged = false;
        return nbt;
    }

    /***
     * Adds only SyncableTags stored in this TagManager to the given CompoundTag.
     * Once SyncableTags have been written, needsSyncing() will return false until a Syncable Tag
     * has been updated again.
     * @param nbt CompoundTag to write
     * @return the writeed CompoundTag
     */
    public CompoundTag writeSyncable(CompoundTag nbt) {
        return writeSyncable(nbt, true);
    }

    /***
     * Puts all Tags from the given CompoundTag into this TagManager
     * @param nbt CompoundTag to get values from
     */
    public void readFrom(CompoundTag nbt) {
        for(String key : nbt.getAllKeys()) {
            Tag tag = nbt.get(key);
            data.put(key, tag);
        }
    }

    /***
     * Adds all of the Tags stored in this TagManager to the given CompoundTag
     * @param nbt CompoundTag to write
     * @return the writeed CompoundTag
     */
    public CompoundTag writeAll(CompoundTag nbt) {
        return nbt.merge(data);
    }
}