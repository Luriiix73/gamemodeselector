package org.lurix.gamemodeselector;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;

public final class Selector {
    private final ItemDisplay itemDisplay;
    private final TextDisplay textDisplay;
    private final Interaction interaction;
    private final float baseScale;
    private final String minimessage;
    private final Material material;
    private String serverName;
    private float rotation;

    public Selector(
            ItemDisplay itemDisplay,
            TextDisplay textDisplay,
            Interaction interaction,
            float baseScale,
            String minimessage,
            Material material,
            String serverName
    ) {
        this.itemDisplay = itemDisplay;
        this.textDisplay = textDisplay;
        this.interaction = interaction;
        this.baseScale = baseScale;
        this.minimessage = minimessage;
        this.material = material;
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

    public String minimessage() {
        return minimessage;
    }

    public Material material() {
        return material;
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

    public Location location() {
        return itemDisplay.getLocation().clone();
    }
}
