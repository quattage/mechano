package com.quattage.mechano.core.electricity.node.base;

import java.util.Locale;

import com.simibubi.create.foundation.utility.Color;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;

/***
 * Stores the insertion/extraction capabilities of a specific ElectricNode in a separate object for convenience.
 * Also stores the colors tied to these modes to be accessed by client-side rendering.
 */
public enum NodeMode implements StringRepresentable {
    NONE(false, false, new Color(110, 110, 110), new Color(196, 196, 196)),
    INSERT(true, false, new Color(241, 0, 149), new Color(255, 101, 196)),
    EXTRACT(false, true, new Color(149, 241, 0), new Color(196, 255, 101)),
    BOTH(true, true, new Color(0, 149, 241), new Color(101, 196, 255));

    private final boolean isInput;
    private final boolean isOutput;
    private final Color baseColor;
    private final Color highlightColor;

    private NodeMode(boolean isInput, boolean isOutput, Color greyedOut, Color selected) {
        this.isInput = isInput;
        this.isOutput = isOutput;
        this.highlightColor = selected;
        this.baseColor = greyedOut;
    }

    public String toString() {
        return "Mode: [" + getSerializedName() + "]";
    }

    public boolean isInput() {
        return isInput;
    }


    public boolean isOutput() {
        return isOutput;
    }

    /***
     * Swaps this NodeMode <p>
     * If this NodeMode is of the "NONE" type, using 
     * this method will return the unmodified input.
     * @param input NodeMode to cycle
     */
    public static NodeMode cycle(NodeMode input) {
        if(input.equals(NodeMode.NONE)) return input; // NONE can't be cycled
        int pos = input.ordinal();
        pos += 1;
        if(pos >= NodeMode.values().length) pos = 1;
        return NodeMode.values()[pos];
    }

    /***
     * Populates a given CompoundTag with this NodeMode.
     * @param in CompoundTag to modify
     * @return The modified CompoundTag 
     */
    public CompoundTag writeTo(CompoundTag in) {
        in.putInt("mode", this.ordinal());
        return in;
    }

    /***
     * Creates a new NodeMode from the given tag.
     * @param in
     * @return The relevent NodeMode from this CompoundTag
     */
    public static NodeMode fromTag(CompoundTag in) {
        if(in.contains("mode")) {
            int mode = in.getInt("mode");
            return NodeMode.values()[mode];
        }
        throw new IllegalArgumentException("CompoundTag " + in + " doesn't contain relevent values to read!");
    }


    /***
     * Compose a new NodeMode from two boolean values
     * @param isInput Can this node input?
     * @param isOutput Can this node output?
     * @return the appropriate NodeMode for the given values
     */
    public static NodeMode from(boolean isInput, boolean isOutput) {
        if(isInput && isOutput)
            return BOTH;
        else {
            if(isInput) return INSERT;
            else if (isOutput) return EXTRACT;
        }
        return NONE;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /***
     * Gets the highlight color of this node.
     * @return
     */
    public Color getHighlightColor() {
        return highlightColor.copy();
    }

    /***
     * Gets the "greyed-out" color of this node.
     * @return
     */
    public Color getBaseColor() {
        return baseColor.copy();
    }

    /***
     * Gets the highlight color of this node mixed with its darker variant
     * @param percent Percent of color to mix (1.0 = fully bright, 0 = fully dark)
     * @return
     */
    public Color getColor(float percent) {
        if(percent >= 1) return highlightColor;
        if(percent <= 0) return baseColor;
        return baseColor.copy().mixWith(highlightColor, percent);
    }
}
