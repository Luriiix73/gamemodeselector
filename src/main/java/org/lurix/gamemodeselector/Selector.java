package org.lurix.gamemodeselector;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

public final class Selector {
    private final ItemDisplay itemDisplay;
    private final TextDisplay textDisplay;
    private final float baseScale;
    private final String clickCommand;
    private float rotation;

    public Selector(ItemDisplay itemDisplay, TextDisplay textDisplay, float baseScale, String clickCommand) {
        this.itemDisplay = itemDisplay;
        this.textDisplay = textDisplay;
        this.baseScale = baseScale;
        this.clickCommand = clickCommand;
    }

    public ItemDisplay itemDisplay() {
        return itemDisplay;
    }

    public TextDisplay textDisplay() {
        return textDisplay;
    }

    public float baseScale() {
        return baseScale;
    }

    public String clickCommand() {
        return clickCommand;
    }

    public float rotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
}
