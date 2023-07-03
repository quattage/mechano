package com.quattage.mechano.core.util.nbt;

import net.minecraft.nbt.CompoundTag;

/** 
 * The DynamicTag class is a simple management class wrapped on top of CompoundTag, designed to provide an easy and quick
 * way to implement CompoundTags on BlockEntities.
 */
public class DynamicTag<T> {

    private final String id;
    private T storedData;
    private boolean shouldSync;

    /***
     * Creates a new DynamicTag object of type T.
     * Acceptable values are INT, BOOL, STRING, FLOAT.
     * @param storedData
     * @param id
     */
    public DynamicTag(T storedData, String id) {
        if(isValid(storedData)) {
            this.storedData = storedData;
            this.id = id;
            this.shouldSync = false;
        } else 
            throw getInvalidError();
    }

    public boolean isSynced() {
        return shouldSync;
    }

    /***
     * Tells implementations that this value should always be synced to all clients
     */
    public DynamicTag<T> setSynced() {
        this.shouldSync = true;
        return this;
    }

    /***
     * Tells implemetations whether or not the server should always update this tag to all clients
     * @param shouldSync True/False: should this value be synced or not
     */
    public void setSynced(boolean shouldSync) {
        this.shouldSync = shouldSync;
    }

    @SuppressWarnings("unchecked")
    public boolean set(int value) {
        if(isInteger()) {
            storedData = (T)Integer.valueOf(value);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean set(String value) {
        if(isString()) {
            storedData = (T)value;
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean set(float value) {
        if(isFloat()) {
            storedData = (T)Float.valueOf(value);
            return true;
        } 
        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean set(boolean value) {
        if(isBool()) {
            storedData = (T)Boolean.valueOf(value);
            return true;
        }
        return false;
    }

    /***
     * Compares two DynamicTags by their String ID
     * @param nbt DynamicTag for comparison
     * @return True if IDs are the same
     */
    public boolean equals(DynamicTag<?> nbt) {
        return getId().equals(nbt.getId());
    }

    /***
     * Get the value of this DynamicTag as a CompoundTag
     * @return a new CompoundTag with the value of this DynamicTag
     */
    public CompoundTag asCompound() {
        CompoundTag nbt = new CompoundTag();
        if(isString()) {
            nbt.putString(id, (String)storedData);
            return nbt;
        }

        if(isBool()) {
            nbt.putBoolean(id, (boolean)storedData);
            return nbt;
        }
        
        if(isInteger()) {
            nbt.putInt(id, (int)storedData);
            return nbt;
        }

        if(isFloat()) {
            nbt.putFloat(id, (float)storedData);
            return nbt;
        }
        throw getInvalidError();
    }

    /***
     * Gets the value of this DynamicTag
     * @return A wrapper (Integer, Float, Boolean, String) with the value of this DynamicTag
     */
    public T asWrapped() {
        if(isValid(storedData)) return storedData;
        throw getInvalidError();
    }

    private IllegalArgumentException getInvalidError() {
        return new IllegalArgumentException(
                "Data must be FLOAT, INTEGER, BOOLEAN, or STRING, was passed '" 
                + getTypeName() + "'!");
    }

    private boolean isValid(T data) {
        return data instanceof String ||
            data instanceof Boolean ||
            data instanceof Integer ||
            data instanceof Float;
    }

    public boolean isString() {
        return storedData instanceof String;
    }

    public boolean isBool() {
        return storedData instanceof Boolean;
    }

    public boolean isInteger() {
        return storedData instanceof Integer;
    }

    public boolean isFloat() {
        return storedData instanceof Float;
    }

    public String getId() {
        return id;
    }

    public String getTypeName() {
        return storedData.getClass().getTypeName().toUpperCase();
    }

    public String toString() {
        return "[" + id + ": " + getTypeName() + " -> " + storedData + "]";
    }
}
