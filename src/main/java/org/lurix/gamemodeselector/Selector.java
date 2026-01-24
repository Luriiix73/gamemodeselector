package org.lurix.gamemodeselector;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;

public final class Selector {
    private final ArmorStand anchor;
    private final ItemDisplay itemDisplay;
    private final Interaction interaction;
    private final Location baseLocation;
    private final float baseScale;
    private final Material material;
    private String serverName;
    private float rotation;

    public Selector(
            ArmorStand anchor,
            ItemDisplay itemDisplay,
            Interaction interaction,
            Location baseLocation,
            float baseScale,
            Material material,
            String serverName
    ) {
        this.anchor = anchor;
        this.itemDisplay = itemDisplay;
        this.interaction = interaction;
        this.baseLocation = baseLocation;
        this.baseScale = baseScale;
        this.material = material;
        this.serverName = serverName;
    }

    public ArmorStand anchor() {
        return anchor;
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
        return baseLocation.clone();
    }
}
