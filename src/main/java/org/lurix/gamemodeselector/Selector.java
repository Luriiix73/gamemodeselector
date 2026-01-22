package org.lurix.gamemodeselector;

import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

public final class Selector {
    private final ItemDisplay itemDisplay;
    private final TextDisplay textDisplay;
    private final Interaction interaction;
    private final float baseScale;
    private String serverName;
    private float rotation;

    public Selector(
            ItemDisplay itemDisplay,
            TextDisplay textDisplay,
            Interaction interaction,
            float baseScale,
            String serverName
    ) {
        this.itemDisplay = itemDisplay;
        this.textDisplay = textDisplay;
        this.interaction = interaction;
        this.baseScale = baseScale;
        this.serverName = serverName;
    }

    public ItemDisplay itemDisplay() {
        return itemDisplay;
    }

    public TextDisplay textDisplay() {
        return textDisplay;
    }

    public Interaction interaction() {
        return interaction;
    }

    public float baseScale() {
        return baseScale;
    }

    public String serverName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public float rotation() {
        return rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }
}
