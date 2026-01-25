package org.lurix.gamemodeselector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class JoinEntityManager {
    private static final String JOIN_ENTITY_TAG = "join-entity";
    private final Plugin plugin;
    private final Map<UUID, JoinEntityData> entitiesByUuid = new HashMap<>();
    private final Map<String, UUID> uuidByInternalId = new HashMap<>();
    private final JoinEntityStorage storage;

    public JoinEntityManager(Plugin plugin) {
        this.plugin = plugin;
        this.storage = new JoinEntityStorage(plugin);
    }

    public void loadEntities() {
        cleanupTaggedEntities();
        entitiesByUuid.clear();
        uuidByInternalId.clear();
        Map<String, JoinEntityData> loaded = storage.load();
        for (JoinEntityData data : loaded.values()) {
            String worldName = data.world();
            World world = worldName == null ? null : plugin.getServer().getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Join-Entity Welt nicht gefunden: " + worldName);
                continue;
            }
            Entity entity = spawnEntity(world, data);
            if (entity == null) {
                plugin.getLogger().warning("Join-Entity konnte nicht gespawnt werden: " + data.internalId());
                continue;
            }
            entitiesByUuid.put(entity.getUniqueId(), data);
            uuidByInternalId.put(data.internalId(), entity.getUniqueId());
        }
    }

    public void saveEntities() {
        Map<String, JoinEntityData> data = new HashMap<>();
        for (JoinEntityData joinEntity : entitiesByUuid.values()) {
            data.put(joinEntity.internalId(), joinEntity);
        }
        storage.save(data);
    }

    public void addJoinEntity(@NotNull JoinEntityData data) {
        World world = plugin.getServer().getWorld(data.world());
        if (world == null) {
            plugin.getLogger().warning("Join-Entity Welt nicht gefunden: " + data.world());
            return;
        }
        Entity entity = spawnEntity(world, data);
        if (entity == null) {
            plugin.getLogger().warning("Join-Entity konnte nicht gespawnt werden: " + data.internalId());
            return;
        }
        entitiesByUuid.put(entity.getUniqueId(), data);
        uuidByInternalId.put(data.internalId(), entity.getUniqueId());
        saveEntities();
    }

    public void removeJoinEntity(@NotNull Entity entity) {
        JoinEntityData data = entitiesByUuid.remove(entity.getUniqueId());
        if (data != null) {
            uuidByInternalId.remove(data.internalId());
            entity.remove();
            saveEntities();
        }
    }

    public void removeJoinEntityById(@NotNull String internalId) {
        UUID uuid = uuidByInternalId.remove(internalId);
        if (uuid == null) {
            return;
        }
        JoinEntityData data = entitiesByUuid.remove(uuid);
        if (data != null) {
            Entity entity = plugin.getServer().getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
            saveEntities();
        }
    }

    @Nullable
    public JoinEntityData getJoinEntity(@NotNull Entity entity) {
        return entitiesByUuid.get(entity.getUniqueId());
    }

    public void connect(@NotNull Player player, @NotNull String targetServer) {
        if (targetServer.isBlank()) {
            player.sendMessage(net.kyori.adventure.text.Component.text("Kein Zielserver gesetzt."));
            return;
        }
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF("Connect");
            out.writeUTF(targetServer);
            player.sendPluginMessage(plugin, "BungeeCord", stream.toByteArray());
        } catch (IOException ex) {
            player.sendMessage(net.kyori.adventure.text.Component.text("Server-Verbindung fehlgeschlagen."));
        }
    }

    @Nullable
    private Entity spawnEntity(@NotNull World world, @NotNull JoinEntityData data) {
        Location location = data.toLocation(world);
        return switch (data.entityType()) {
            case TEXT_DISPLAY -> world.spawn(location, TextDisplay.class, display -> {
                display.text(MiniMessage.miniMessage().deserialize(data.displayText()));
                display.setPersistent(true);
                display.setInvulnerable(data.invulnerable());
                display.setGlowing(data.glowing());
                display.addScoreboardTag(JOIN_ENTITY_TAG);
                display.setTransformation(createTransformation(data.scale()));
            });
            case ARMOR_STAND -> world.spawn(location, ArmorStand.class, stand -> {
                stand.setPersistent(true);
                stand.setInvulnerable(data.invulnerable());
                stand.setGlowing(data.glowing());
                stand.setGravity(data.gravity());
                stand.setVisible(data.visible());
                stand.setMarker(data.marker());
                stand.addScoreboardTag(JOIN_ENTITY_TAG);
            });
        };
    }

    private Transformation createTransformation(float scale) {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        );
    }

    private void cleanupTaggedEntities() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(JOIN_ENTITY_TAG)) {
                    entity.remove();
                }
            }
        }
    }
}
