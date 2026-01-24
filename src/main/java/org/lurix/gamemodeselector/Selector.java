package org.lurix.gamemodeselector;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;

public final class Selector {
    private final ItemDisplay itemDisplay;
    private final Interaction interaction;
    private final float baseScale;
    private final Material material;
    private String serverName;
    private float rotation;

    public Selector(
            ItemDisplay itemDisplay,
            Interaction interaction,
            float baseScale,
            Material material,
            String serverName
    ) {
        this.itemDisplay = itemDisplay;
        this.interaction = interaction;
        this.baseScale = baseScale;
        this.material = material;
        this.serverName = serverName;
    }

    public ItemDisplay itemDisplay() {
        return itemDisplay;
    }

    public Interaction interaction() {
        return interaction;
    }

    public float baseScale() {
        return baseScale;
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
