package com.quattage.mechano.api.switchboard.action;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.quattage.mechano.Mechano;
import com.quattage.mechano.foundation.numeric.EsoMath;

import net.createmod.catnip.theme.Color;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

public enum GridActionType implements StringRepresentable {

    RESPONSE_SUCCESS(false, new Color(116, 227, 142), new Color(116, 227, 197), new Color(116, 201, 227)),
    RESPONSE_FAIL_SOFT(true, new Color(0, 0, 0, 0.4f)),
    RESPONSE_FAIL_HARD(true, new Color(227, 100, 178), new Color(227, 100, 115), new Color(227, 149, 100)),
    TASK_GENERIC(false, (Color[])null),
    NONE(true, (Color[])null);

    private boolean isFailCase;
    private final @Nullable Color[] colors;

    GridActionType(boolean isFailCase, Color... colors) {
        this.isFailCase = isFailCase;
        this.colors = colors;
    }

    public boolean indicatesSuccess() {
        return !isFailCase;
    }

    public boolean indicatesFailure() {
        return !indicatesSuccess();
    }

    public boolean isVisible() {
        return colors != null;
    }

    public boolean isHighlighted() {
        return colors != null && colors.length > 1 && colors[0] != null;
    }

    public boolean isTask() {
        return this == TASK_GENERIC;
    }

    public Color getColor(Object obj) { 
        if(this.colors == null) 
            return GridActionType.RESPONSE_FAIL_SOFT.getColor();
        long hash = EsoMath.hash64(obj);
        int idx = (int) ((hash >>> 32) % colors.length);
        if(idx < 0) idx += colors.length; 
        return colors[idx];
    }

    public @Nullable Color getColor() {
        if(this.colors == null) 
            return GridActionType.RESPONSE_FAIL_SOFT.getColor();
        return colors[0];
    }

    public ResourceLocation asResource() {
        return Mechano.asResource(getSerializedName());
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return getSerializedName();
    }

    public static class GridActionEncodeException extends RuntimeException {
        public GridActionEncodeException(Exception prev, GridAction response) {
            super("Encountered an exception while encoding '" + response + "' (see above for more details)");
            prev.printStackTrace();
        }
    }

    public static class GridActionDecodeException extends RuntimeException {
        public GridActionDecodeException(Exception prev, GridAction response) {
            super("Encountered an exception while decoding '" + response + "' (see above for more details)");
            prev.printStackTrace();
        }
    }
}
