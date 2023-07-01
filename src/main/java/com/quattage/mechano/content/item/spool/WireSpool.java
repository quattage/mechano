package com.quattage.mechano.content.item.spool;

import java.util.HashMap;

import com.quattage.mechano.registry.MechanoItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public abstract class WireSpool extends Item{

    protected final String PREFIX = "wire_";
    protected final int rate;
    protected final String name;
    protected final ItemStack emptySpoolDrop;
    protected final ItemStack rawDrop;
    public static final HashMap<String, WireSpool> spoolTypes = new HashMap<String, WireSpool>();

    /***
     * Create a new WireSpool object
     * @param properties ItemProperties
     * @param name String ID of this spool. Simple names (ex. "hookup")
     * @param rate Transfer rate (in FE per Tick) of this WireSpool's cooresponding wire type
     * @param base
     */
    public WireSpool(Properties properties) {
        super(properties);
        this.name = setName().toLowerCase();
        this.rate = setRate();
        this.emptySpoolDrop = new ItemStack(setRawDrop());
        this.rawDrop = new ItemStack(setRawDrop());
        WireSpool.addType(this);
    }

    /***
     * Add a WireSpool object to the WireSpool cache. 
     * The stored WireSpool instance is then used by {@link #get() get()} 
     * to look up WireSpool types by ID when said ID retrieved from NBT.
     * <strong>Note: You don't have to do this yourself. It's done automatically
     * by the WireSpool's  {@link #WireSpool(Properties properties) constructor}.</strong>
     * @param spool WireSpool instance to cache.
     */
    public static final void addType(WireSpool spool) {
        spoolTypes.put(spool.getId(), spool);
    }

    /***
     * Retrieve a WireSpool object from the cache. This is used 
     * @param id ID to retrieve (ex. 'wire_hookup' or 'wire_transmission')
     */
    public static final WireSpool get(String id) {
        return spoolTypes.get(id);
    }

    /***
     * The name of this Spool's wire. This is used as the ID of this spool. 
     * ex. a name of "hookup" results in an ID of "wire_hookup"
     * This is used to look up the wire's texture.
     * @return
     */
    protected abstract String setName();


    /***
     * The energy transfer rate of this WireSpool's cooresponding wire type.
     * @return Int in FE per tick. 
     */
    protected abstract int setRate();

    /***
     * The Raw item that cooresponds to this WireSpool. This is usually just a basic wire item, which is
     * applied to the spool through some process. Note that you can set this drop to be the spool itself.
     * @return the Item instance of the desired raw item. Obtained through registry.
     */
    protected abstract Item setRawDrop();

    /***
     * The"Empty" version of this WireSpool, which is given to the player when they run out of wire.
     * Normally, this would just be an empty spool.
     * Override this if you want additional behavior or a custom EmptySpool object.
     * @return the Item instance of the desired empty spool. Obtained through registry.
     */
    protected Item setEmptySpoolDrop() {
        return MechanoItems.EMPTY_SPOOL.get();
    }

    public final String getId() {
        return PREFIX + name;
    }

    public final ItemStack getEmptySpool() {
        return emptySpoolDrop;
    }
    
    public final ItemStack getrawDrop() {
        return rawDrop;
    }

    public final int getRate() {
        return rate;
    }
}
