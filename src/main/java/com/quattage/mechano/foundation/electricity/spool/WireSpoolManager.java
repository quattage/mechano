package com.quattage.mechano.foundation.electricity.spool;

import java.util.ArrayList;
import java.util.HashMap;

public class WireSpoolManager {
    public static final HashMap<String, WireSpool> types = new HashMap<>();

    /***
     * Add a WireSpool object to the WireSpool cache. 
     * The stored WireSpool instance is then used by {@link #get() get()} 
     * to look up WireSpool types by ID when said ID retrieved from NBT.
     * <strong>Note: You don't have to do this yourself. It's done automatically
     * by the WireSpool's  {@link #WireSpool(Properties properties) constructor}.</strong>
     * @param spool WireSpool instance to cache.
     */
    public static void addType(WireSpool spool) {
        types.put(spool.getId(), spool);
    }

    /***
     * Retrieve a WireSpool object from the cache. This is used 
     * @param id ID to retrieve (ex. 'wire_hookup' or 'wire_transmission')
     */
    public static WireSpool getType(String id) {
        return types.get(id);
    }

    /***
     * Gets the stored HashMap of spool ItemStacks.
     * @return
     */
    public static HashMap<String, WireSpool> getTypes() {
        return types;
    }

    public static WireSpool[] getTypesAsArray() {
        return types.values().toArray(new WireSpool[0]);
    }
}
