package com.quattage.mechano.foundation;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

/***
 * Allows Items or BlockItems to override their creative tab or suppress their visibility entirely.
 * Blocks/Items that do not implement this interface are added to <code>MechanoGroups.BASE</code>
 * by default.
 */
public interface CreativeTabOverridable {

    /***
     * Define an overridden creative mode tab to put this block/item into.
     * @return A DeferredHolder registry object containing the tab's registry, or null if
     * this block/item shouldn't appear in the creative menu at all.
     */
    default @Nullable DeferredHolder<CreativeModeTab, CreativeModeTab> getTab() {
        return null;
    }

    /***
     * Checks whether or not the given item is configured to be placed in the
     * provided creative mode tab
     * @param item Item to check
     * @param tab Registry object containing a creative mode tab
     * @return <code>true</code> if the provided item belongs to the tab
     */
    static boolean belongsTo(Item item, DeferredHolder<CreativeModeTab, CreativeModeTab> tab) {
        if(!(item instanceof CreativeTabOverridable cto)) return true;
        if(cto.getTab() == null) return false;
        return cto.getTab().equals(tab);
    }
}