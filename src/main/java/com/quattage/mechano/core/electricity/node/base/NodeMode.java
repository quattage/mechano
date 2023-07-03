package com.quattage.mechano.core.electricity.node.base;

import java.util.Locale;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;

public enum NodeMode implements StringRepresentable{
    INSERT(true, false),
    EXTRACT(false, true),
    BOTH(true, true);

    private final boolean isInput;
    private final boolean isOutput;

    private NodeMode(boolean isInput, boolean isOutput) {
        this.isInput = isInput;
        this.isOutput = isOutput;
    }

    public CompoundTag writeTo(CompoundTag in) {
        in.putString("NodeMode", getSerializedName());
        return in;
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

    public static NodeMode cycle(NodeMode input) {
        int pos = input.ordinal();
        pos += 1;
        if(pos >= NodeMode.values().length) pos = 0;
        return NodeMode.values()[pos];
    }

    public static NodeMode fromNbt(CompoundTag in) {
        if(in.contains("NodeMode")) {
            String mode = in.getString("NodeMode");
            if(mode.equals("insert")) return from(true, false);
            if(mode.equals("extract")) return from(false, true);
            if(mode.equals("both")) return from(true, true);
        }
        throw new IllegalArgumentException("CompoundTag " + in + " doesn't contain relevent values to read!");
    }

    public static NodeMode from(boolean isInput, boolean isOutput) {
        if(isInput && isOutput)
            return BOTH;
        else {
            if(isInput) return INSERT;
            else if (isOutput) return EXTRACT;
        }
        throw new IllegalArgumentException("No such NodeMode: {in: " + isInput + " out: " + isOutput + "}");
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
