package com.quattage.mechano.foundation.electricity.builder;

import com.quattage.mechano.foundation.electricity.core.anchor.AnchorTransform;
import com.quattage.mechano.foundation.electricity.core.anchor.NodeMode;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/***
 * A builder class that makes creating ElectricNode instances safer
 * and somewhat more intuitive.
 */
public class ElectricNodeBuilder {
    
    private NodeBankBuilder activeBuilder;
    private final BlockEntity target;
    private NodeMode mode = NodeMode.BOTH;
    private AnchorTransform location;
    private int maxConnections = 1;
    private float size = 4/16f;

    public ElectricNodeBuilder(NodeBankBuilder activeBuilder, BlockEntity target) {
        this.activeBuilder = activeBuilder;
        this.target = target;
    }

    /***
     * Set the mode of this ElectricNode.
     * <strong>Case-insensitive.</strong> <p>
     * Acceptable values: <p>
     * <code> "INSERT", "EXTRACT", "BOTH", "NONE"  </code> <p>
     * Also accepts shorthands: <p>
     * <code> "I", "O", "B", "N" </code> <p>
     * 
     * You can also directly pass a NodeMode object.
     * 
     * @param mode String mode to set. More specifically, the name of the Enum property in {@link com.quattage.mechano.foundation.electricity.core.node.NodeMode NodeMode}
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder mode(String modeS) {
        modeS = modeS.toUpperCase();
        if(modeS.equals("INSERT") || modeS.equals("I")) this.mode = NodeMode.INSERT;
        else if(modeS.equals("EXTRACT") || modeS.equals("O")) this.mode = NodeMode.EXTRACT;
        else if(modeS.equals("NONE") || modeS.equals("N")) this.mode = NodeMode.NONE;
        else this.mode = NodeMode.BOTH;
        return this;
    }

    /***
     * Set the mode of this ElectricNode.
     * <strong>Case-insensitive.</strong> <p>
     * Acceptable values: <p>
     * <code> "INSERT", "EXTRACT", "BOTH", "NONE"  </code> <p>
     * Also accepts shorthands: <p>
     * <code> "I", "O", "B", "N" </code> <p>
     * 
     * You can also directly pass a NodeMode object.
     * 
     * @param mode String mode to set. More specifically, the name of the Enum property in {@link com.quattage.mechano.foundation.electricity.core.node.NodeMode NodeMode}
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder mode(NodeMode mode) {
        this.mode = mode;
        return this;
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
     * @param defaultFacing (Optional) - The orientation your model is in within BlockBench. You shouldn't
     * need to use this most of the time.
     * @param x x Offset from center (as an int or double)
     * @param y y Offset from center (as an int or double)
     * @param z z Offset from center (as an int or double)
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder at(int x, int y, int z) {
        location = new AnchorTransform(target.getBlockPos(), x, y, z, size, Direction.NORTH);
        return this;
    }

    /***
     * The local offset of this node. Relative to the northern bottom corner of the block. <p>
     * This is based on pixel measurements (out of 16) rather than raw vectors. For example, if you wanted
     * to place a node on the center of the block (which would normally be 0.5, 0.5, 0.5), you
     * would use:
     * <pre>
     * ElectricNodeBuilder.at(8, 8, 8);
     * </pre>
     * Since pixel measurements are used here, you can just copy/paste coordinates directly from Blockbench.
     * @param defaultFacing (Optional) - The orientation your model is in within BlockBench. You shouldn't
     * need to use this most of the time.
     * @param x x Offset from center (as an int or double)
     * @param y y Offset from center (as an int or double)
     * @param z z Offset from center (as an int or double)
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder at(double x, double y, double z) {
        location = new AnchorTransform(target.getBlockPos(), x, y, z, size, Direction.NORTH);
        return this;
    }

    /***
     * The local offset of this node. Relative to the northern bottom corner of the block. <p>
     * This is based on pixel measurements (out of 16) rather than raw vectors. For example, if you wanted
     * to place a node on the center of the block (which would normally be 0.5, 0.5, 0.5), you
     * would use:
     * <pre>
     * ElectricNodeBuilder.at(8, 8, 8);
     * </pre>
     * Since pixel measurements are used here, you can just copy/paste coordinates directly from Blockbench.
     * @param defaultFacing (Optional) - The orientation your model is in within BlockBench. You shouldn't
     * need to use this most of the time.
     * @param x x Offset from center (as an int or double)
     * @param y y Offset from center (as an int or double)
     * @param z z Offset from center (as an int or double)
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder at(int x, int y, int z, Direction defaultFacing) {
        location = new AnchorTransform(target.getBlockPos(), x, y, z, size, defaultFacing);
        return this;
    }

    /***
     * The local offset of this node. Relative to the northern bottom corner of the block. <p>
     * This is based on pixel measurements (out of 16) rather than raw vectors. For example, if you wanted
     * to place a node on the center of the block (which would normally be 0.5, 0.5, 0.5), you
     * would use:
     * <pre>
     * ElectricNodeBuilder.at(8, 8, 8);
     * </pre>
     * Since pixel measurements are used here, you can just copy/paste coordinates directly from Blockbench.
     * @param defaultFacing (Optional) - The orientation your model is in within BlockBench. You shouldn't
     * need to use this most of the time.
     * @param x x Offset from center (as an int or double)
     * @param y y Offset from center (as an int or double)
     * @param z z Offset from center (as an int or double)
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder at(double x, double y, double z, Direction defaultFacing) {
        location = new AnchorTransform(target.getBlockPos(), x, y, z, size, defaultFacing);
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

    public ElectricNodeBuilder size(float size) {
        location = new AnchorTransform(location, size);
        return this;
    }

    public NodeBankBuilder build() {
        return activeBuilder.add(location, mode, maxConnections);
    };
}
