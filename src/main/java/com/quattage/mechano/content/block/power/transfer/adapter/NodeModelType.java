package com.quattage.mechano.content.block.power.transfer.adapter;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public enum NodeModelType implements StringRepresentable {
    BASE, GROUNDED, GIRDERED, ROTORED, ROTOR_CANTED;

    public static final EnumProperty<NodeModelType> NODE_MODEL_TYPE = EnumProperty.create("model", NodeModelType.class);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }

    public static NodeModelType cycleRotor(NodeModelType type) {
        int pos = type.ordinal();
        if(pos == 4) {
            pos -= 1;
            return NodeModelType.values()[pos];
        }
        pos += 1;
        return NodeModelType.values()[pos];
    }
}