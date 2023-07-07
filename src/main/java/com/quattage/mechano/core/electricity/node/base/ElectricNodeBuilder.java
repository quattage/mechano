package com.quattage.mechano.core.electricity.node.base;

import com.quattage.mechano.core.electricity.node.NodeBankBuilder;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ElectricNodeBuilder {
    
    private NodeBankBuilder activeBuilder;
    private final BlockEntity target;
    private NodeMode mode = NodeMode.BOTH;
    private NodeLocation location;
    private String id = null;
    private int maxConnections = 1;

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
     * @param mode String mode to set. More specifically, the name of the Enum property in {@link com.quattage.mechano.core.electricity.node.base.NodeMode NodeMode}
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
     * @param mode String mode to set. More specifically, the name of the Enum property in {@link com.quattage.mechano.core.electricity.node.base.NodeMode NodeMode}
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder mode(NodeMode mode) {
        this.mode = mode;
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
    public ElectricNodeBuilder at(int x, int y, int z) {
        location = new NodeLocation(target.getBlockPos(), x, y, z, Direction.NORTH);
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
        location = new NodeLocation(target.getBlockPos(), x, y, z, Direction.NORTH);
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
        location = new NodeLocation(target.getBlockPos(), x, y, z, defaultFacing);
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
        location = new NodeLocation(target.getBlockPos(), x, y, z, defaultFacing);
        return this;
    }

    /***
     * Non-formatted, non-strict, colloquial name for this ElectricNode.
     * This ID will be serialized to and from NBT, so it's probably best to keep it brief, but it's up to you.
     * @param id ID to set this Builder to
     * @return this ElectircNodeBuilder with the modified value.
     */
    public ElectricNodeBuilder id(String id) {
        this.id = id;
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

    public NodeBankBuilder build() {
        if(id == null) throw new IllegalArgumentException("ElectricNode cannot be built - 'id' is null!");
        return activeBuilder.add(location, id, mode, maxConnections);
    };
}
