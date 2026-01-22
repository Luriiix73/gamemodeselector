package org.lurix.gamemodeselector;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

public final class Selector {
    private final ItemDisplay itemDisplay;
    private final TextDisplay textDisplay;
    private final float baseScale;
    private final String serverName;
    private float rotation;

    public Selector(ItemDisplay itemDisplay, TextDisplay textDisplay, float baseScale, String serverName) {
        this.itemDisplay = itemDisplay;
        this.textDisplay = textDisplay;
        this.baseScale = baseScale;
        this.serverName = serverName;
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

    public String serverName() {
        return serverName;
    }

    public float rotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
}
