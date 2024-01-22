package com.quattage.mechano.foundation.electricity.core.anchor.interaction;

import com.simibubi.create.foundation.utility.Color;

public enum AnchorInteractColor {

    NONE(new Color(110, 110, 110), new Color(196, 196, 196)),
    INSERT(new Color(241, 0, 149), new Color(255, 101, 196)),
    EXTRACT(new Color(149, 241, 0), new Color(196, 255, 101)),
    BOTH(new Color(0, 149, 241), new Color(101, 196, 255));

    private final Color passiveColor;
    private final Color highlightColor;

    private AnchorInteractColor(Color passiveColor, Color highlightColor) {
        this.passiveColor = passiveColor;
        this.highlightColor = highlightColor;
    }

    public Color get() {
        return highlightColor;
    }

    public Color get(float percent) {
        if(percent >= 1) return highlightColor;
        if(percent <= 0) return passiveColor;
        return passiveColor.copy().mixWith(highlightColor, percent);
    }
}