package org.lurix.gamemodeselector;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class JoinEntityStorage {
    private final Plugin plugin;
    private final File storageFile;

    public JoinEntityStorage(Plugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "join_entities.yml");
    }

    public Map<String, JoinEntityData> load() {
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

    public void save(Map<String, JoinEntityData> data) {
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
