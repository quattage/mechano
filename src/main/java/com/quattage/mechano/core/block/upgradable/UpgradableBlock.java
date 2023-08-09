package com.quattage.mechano.core.block.upgradable;

import java.util.ArrayList;

import net.minecraft.world.item.Item;

/***
 * Upgradable Blocks are blocks that hold an array of Upgradable block variants.
 * This list of upgraded/downgraded variants can be addressed to swap this block
 * in-place with its upgraded version. 
 */
public class UpgradableBlock extends RootUpgradableBlock {

    public UpgradableBlock(Properties properties, RootUpgradableBlock root, int iteration) {
        super(properties, root, iteration);
    }
    
    @Override
    protected Item setUpgradeItem() {
        return null;
    }

    @Override
    protected ArrayList<RootUpgradableBlock> setUpgradeTiers(ArrayList<RootUpgradableBlock> upgrades) {
        return null;
    }
}
