package com.quattage.mechano.foundation.electricity.builder;

import com.quattage.mechano.foundation.electricity.core.anchor.AnchorTransform;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * A builder class that makes creating ElectricNode instances safer
 * and somewhat more intuitive.
 */
public class ElectricNodeBuilder {
    
    private AnchorBankBuilder<?> activeBuilder;
    private AnchorTransform location;
    private int maxConnections = 1;

    public ElectricNodeBuilder(AnchorBankBuilder<?> activeBuilder) {
        this.activeBuilder = activeBuilder;
    }

    /***
     * The local offset of this node. Relative to the northern bottom corner of the block. <p>
     * This is based on pixel measurements (usually out of 16) rather than raw vectors. For example, 
     * if you wanted to place a node on the center of the block (which would normally be 0.5, 0.5, 0.5),
     * you would use:
     * <pre> ElectricNodeBuilder.at(8, 8, 8); </pre>
     * Since pixel measurements are used here, you can just copy/paste coordinates directly from Blockbench.
     * These pixel measurements are permitted to exist outside of a standard Minecraft block's collider. 
     * (Measurements greater than 16 are permitted) 
     * @param x x Offset from center (as an int or double)
     * @param y y Offset from center (as an int or double)
     * @param z z Offset from center (as an int or double)
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder at(int x, int y, int z) {
        location = new AnchorTransform(x, y, z);
        return this;
    }
    
    /***
     * The maximum amount of allowed connections to this ElectricNode.
     * @param max
     * @return
     */
    public ElectricNodeBuilder connections(int maxConnections) {
        this.maxConnections = maxConnections;
        return this;
    }

    public AnchorBankBuilder<?> build() {
        return activeBuilder.add(location, maxConnections);
    };
}
