package org.lurix.gamemodeselector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class JoinEntityManager implements Listener {
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

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getRightClicked(), event.getPlayer(), event);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        handleInteract(event.getRightClicked(), event.getPlayer(), event);
    }

    private void handleInteract(Entity entity, Player player, org.bukkit.event.Cancellable event) {
        JoinEntityData data = getJoinEntity(entity);
        if (data == null) {
            return;
        }
        event.setCancelled(true);
        connect(player, data.targetServer());
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

    private static final class JoinEntityStorage {
        private final Plugin plugin;
        private final File storageFile;

        private JoinEntityStorage(Plugin plugin) {
            this.plugin = plugin;
            this.storageFile = new File(plugin.getDataFolder(), "join_entities.yml");
        }

        private Map<String, JoinEntityData> load() {
            ensureStorage();
            YamlConfiguration storage = YamlConfiguration.loadConfiguration(storageFile);
            ConfigurationSection section = storage.getConfigurationSection("entities");
            Map<String, JoinEntityData> data = new LinkedHashMap<>();
            if (section == null) {
                return data;
            }
            for (String id : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(id);
                if (entry == null) {
                    continue;
                }
                data.put(id, new JoinEntityData(
                        id,
                        entry.getString("world", "world"),
                        entry.getDouble("x"),
                        entry.getDouble("y"),
                        entry.getDouble("z"),
                        (float) entry.getDouble("yaw"),
                        (float) entry.getDouble("pitch"),
                        JoinEntityData.EntityType.valueOf(entry.getString("entityType", "TEXT_DISPLAY")),
                        entry.getString("displayText", ""),
                        (float) entry.getDouble("scale", 1.0),
                        entry.getString("targetServer", ""),
                        entry.getBoolean("glowing", false),
                        entry.getBoolean("invulnerable", true),
                        entry.getBoolean("gravity", false),
                        entry.getBoolean("visible", true),
                        entry.getBoolean("marker", false)
                ));
            }
            return data;
        }

        private void save(Map<String, JoinEntityData> data) {
            ensureStorage();
            YamlConfiguration storage = new YamlConfiguration();
            ConfigurationSection section = storage.createSection("entities");
            for (JoinEntityData joinEntity : data.values()) {
                ConfigurationSection entry = section.createSection(joinEntity.internalId());
                entry.set("world", joinEntity.world());
                entry.set("x", joinEntity.x());
                entry.set("y", joinEntity.y());
                entry.set("z", joinEntity.z());
                entry.set("yaw", joinEntity.yaw());
                entry.set("pitch", joinEntity.pitch());
                entry.set("entityType", joinEntity.entityType().name());
                entry.set("displayText", joinEntity.displayText());
                entry.set("scale", joinEntity.scale());
                entry.set("targetServer", joinEntity.targetServer());
                entry.set("glowing", joinEntity.glowing());
                entry.set("invulnerable", joinEntity.invulnerable());
                entry.set("gravity", joinEntity.gravity());
                entry.set("visible", joinEntity.visible());
                entry.set("marker", joinEntity.marker());
            }
            try {
                storage.save(storageFile);
            } catch (IOException ex) {
                plugin.getLogger().warning("Join-Entities konnten nicht gespeichert werden: " + ex.getMessage());
            }
        }

        private void ensureStorage() {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Plugin-Datenordner konnte nicht erstellt werden.");
            }
            if (!storageFile.exists()) {
                YamlConfiguration storage = new YamlConfiguration();
                ConfigurationSection section = storage.createSection("entities");
                ConfigurationSection example = section.createSection("example-id");
                example.set("world", "world");
                example.set("x", 0.0);
                example.set("y", 64.0);
                example.set("z", 0.0);
                example.set("yaw", 0.0);
                example.set("pitch", 0.0);
                example.set("entityType", "TEXT_DISPLAY");
                example.set("displayText", "<green>Join Skywars</green>");
                example.set("scale", 1.0);
                example.set("targetServer", "skywars-1");
                example.set("glowing", false);
                example.set("invulnerable", true);
                example.set("gravity", false);
                example.set("visible", true);
                example.set("marker", false);
                try {
                    storage.save(storageFile);
                } catch (IOException ex) {
                    plugin.getLogger().warning("join_entities.yml konnte nicht erstellt werden: " + ex.getMessage());
                }
            }
        }
    }
}
