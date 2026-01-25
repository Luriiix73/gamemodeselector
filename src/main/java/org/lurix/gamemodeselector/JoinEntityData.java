package org.lurix.gamemodeselector;

import java.util.Objects;
import org.bukkit.Location;

public final class JoinEntityData {
    public enum EntityType {
        TEXT_DISPLAY,
        ARMOR_STAND
    }

    private final String internalId;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;
    private final EntityType entityType;
    private final String displayText;
    private final float scale;
    private final String targetServer;
    private final boolean glowing;
    private final boolean invulnerable;
    private final boolean gravity;
    private final boolean visible;
    private final boolean marker;

    public JoinEntityData(
            String internalId,
            String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            EntityType entityType,
            String displayText,
            float scale,
            String targetServer,
            boolean glowing,
            boolean invulnerable,
            boolean gravity,
            boolean visible,
            boolean marker
    ) {
        this.internalId = Objects.requireNonNull(internalId, "internalId");
        this.world = Objects.requireNonNull(world, "world");
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.displayText = displayText == null ? "" : displayText;
        this.scale = scale;
        this.targetServer = targetServer == null ? "" : targetServer;
        this.glowing = glowing;
        this.invulnerable = invulnerable;
        this.gravity = gravity;
        this.visible = visible;
        this.marker = marker;
    }

    public String internalId() {
        return internalId;
    }

    public String world() {
        return world;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public EntityType entityType() {
        return entityType;
    }

    public String displayText() {
        return displayText;
    }

    public float scale() {
        return scale;
    }

    public String targetServer() {
        return targetServer;
    }

    public boolean glowing() {
        return glowing;
    }

    public boolean invulnerable() {
        return invulnerable;
    }

    public boolean gravity() {
        return gravity;
    }

    public boolean visible() {
        return visible;
    }

    public boolean marker() {
        return marker;
    }

    public Location toLocation(org.bukkit.World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
