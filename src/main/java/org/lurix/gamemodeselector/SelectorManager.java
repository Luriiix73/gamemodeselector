package org.lurix.gamemodeselector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class SelectorManager implements Listener {
    private static final String SELECTOR_TAG = "gamemodeselector";
    private static final double LEGACY_CLEANUP_RADIUS = 1.5;
    private static final float HOVER_SCALE_MULTIPLIER = 1.5f;
    private static final double MAX_INTERACT_DISTANCE = 10.0;
    private static final float ROTATION_STEP = 0.015f;

    private final Plugin plugin;
    private final Map<UUID, Selector> selectors = new HashMap<>();
    private final Map<UUID, UUID> hoveredByPlayer = new HashMap<>();
    private boolean loading;

    public SelectorManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void spawnSelector(
            @NotNull Player player,
            @NotNull String materialName,
            @NotNull String sizeInput
    ) {
        boolean created = spawnSelectorAt(
                player.getLocation().clone(),
                materialName,
                sizeInput,
                null,
                0f,
                player
        );
        if (created) {
            player.sendMessage(Component.text("Gamemode-Selector erstellt."));
        }
    }

    public boolean spawnSelectorAt(
            @NotNull Location location,
            @NotNull String materialName,
            @NotNull String sizeInput,
            @Nullable String serverName,
            float rotation,
            @Nullable Player notifier
    ) {
        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            if (notifier != null) {
                notifier.sendMessage(Component.text("Unbekanntes Material: " + materialName));
            }
            return false;
        }
        float size;
        try {
            size = Float.parseFloat(sizeInput);
        } catch (NumberFormatException ex) {
            if (notifier != null) {
                notifier.sendMessage(Component.text("Größe muss eine Zahl sein."));
            }
            return false;
        }
        if (size <= 0.1f) {
            if (notifier != null) {
                notifier.sendMessage(Component.text("Größe muss größer als 0.1 sein."));
            }
            return false;
        }

        Location baseLocation = location.clone();
        World world = baseLocation.getWorld();
        if (world == null) {
            return false;
        }
        ItemDisplay itemDisplay = world.spawn(baseLocation, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setBillboard(Display.Billboard.FIXED);
            display.setGlowing(true);
            display.setTransformation(createTransformation(size, rotation));
            display.addScoreboardTag(SELECTOR_TAG);
        });

        float interactionSize = Math.max(0.5f, size);
        Location interactionLocation = baseLocation.clone().subtract(0, interactionSize * 0.5, 0);
        Interaction interaction = world.spawn(interactionLocation, Interaction.class, hitbox -> {
            hitbox.setInteractionWidth(interactionSize);
            hitbox.setInteractionHeight(interactionSize);
            hitbox.addScoreboardTag(SELECTOR_TAG);
        });

        Selector selector = new Selector(itemDisplay, interaction, baseLocation.clone(), size, material, serverName);
        selector.setRotation(rotation);
        selectors.put(itemDisplay.getUniqueId(), selector);
        selectors.put(interaction.getUniqueId(), selector);
        if (!loading) {
            saveSelectors();
        }
        return true;
    }

    @EventHandler
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Entity entity = event.getRightClicked();
        Selector selector = selectors.get(entity.getUniqueId());
        if (selector == null) {
            return;
        }
        event.setCancelled(true);
        handleSelection(event.getPlayer(), selector);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) {
            return;
        }
        Player player = event.getPlayer();
        Selector selector = rayTraceSelector(player);
        if (selector == null) {
            return;
        }
        event.setCancelled(true);
        handleSelection(player, selector);
    }

    public void tickRotation() {
        Set<Selector> uniqueSelectors = new HashSet<>(selectors.values());
        List<Selector> invalidSelectors = new ArrayList<>();
        for (Selector selector : uniqueSelectors) {
            ItemDisplay display = selector.itemDisplay();
            if (!display.isValid()) {
                invalidSelectors.add(selector);
                continue;
            }
            float nextRotation = selector.rotation() + ROTATION_STEP;
            selector.setRotation(nextRotation);
            float targetRotation = nextRotation;
            Player viewer = findNearestPlayer(display.getLocation());
            if (viewer != null) {
                targetRotation = (float) Math.atan2(
                        viewer.getLocation().getX() - display.getLocation().getX(),
                        viewer.getLocation().getZ() - display.getLocation().getZ()
                );
            }
            display.setTransformation(createTransformation(selector.baseScale(), targetRotation));
        }
        for (Selector selector : invalidSelectors) {
            removeSelector(selector);
        }
    }

    public void shutdown() {
        saveSelectors();
        for (Selector selector : selectors.values()) {
            selector.itemDisplay().remove();
            selector.interaction().remove();
        }
        selectors.clear();
        hoveredByPlayer.clear();
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
            List<String> materials = new ArrayList<>();
            for (Material material : Material.values()) {
                if (!material.isAir()) {
                    materials.add(material.name().toLowerCase());
                }
            }
            return materials;
        }
        if (args.length == 1) {
            return List.of("set", "add", "remove", "save", "clear");
        }
        return List.of();
    }

    public void updateHover(@NotNull Player player, @Nullable Selector selector) {
        UUID playerId = player.getUniqueId();
        UUID previous = hoveredByPlayer.get(playerId);
        UUID current = selector == null ? null : selector.itemDisplay().getUniqueId();

        if (previous != null && (current == null || !previous.equals(current))) {
            Selector previousSelector = selectors.get(previous);
            if (previousSelector != null) {
                resetHover(previousSelector);
            }
        }

        if (selector != null && (previous == null || !previous.equals(current))) {
            applyHover(player, selector);
        }

        if (current == null) {
            hoveredByPlayer.remove(playerId);
        } else {
            hoveredByPlayer.put(playerId, current);
        }
    }

    public void removeNearestSelector(@NotNull Player player) {
        Selector selector = rayTraceSelector(player);
        if (selector == null) {
            selector = findNearestSelector(player);
        }
        if (selector == null) {
            player.sendMessage(Component.text("Kein Gamemode-Selector in der Nähe gefunden."));
            return;
        }
        removeSelector(selector);
        player.sendMessage(Component.text("Gamemode-Selector entfernt."));
        saveSelectors();
    }

    public void setNearestSelectorServer(@NotNull Player player, @NotNull String serverName) {
        if (serverName.isBlank()) {
            player.sendMessage(Component.text("Server-Name darf nicht leer sein."));
            return;
        }
        Selector selector = rayTraceSelector(player);
        if (selector == null) {
            selector = findNearestSelector(player);
        }
        if (selector == null) {
            player.sendMessage(Component.text("Kein Gamemode-Selector in der Nähe gefunden."));
            return;
        }
        selector.setServerName(serverName);
        player.sendMessage(Component.text("Gamemode-Selector Ziel gesetzt: " + serverName));
        saveSelectors();
    }

    @Nullable
    public Selector rayTraceSelector(@NotNull Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                MAX_INTERACT_DISTANCE,
                entity -> entity instanceof ItemDisplay
        );
        if (result == null || result.getHitEntity() == null) {
            return null;
        }
        return selectors.get(result.getHitEntity().getUniqueId());
    }

    private void applyHover(Player player, Selector selector) {
        ItemDisplay display = selector.itemDisplay();
        display.setTransformation(createTransformation(selector.baseScale() * HOVER_SCALE_MULTIPLIER, selector.rotation()));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.4f);
    }

    private void resetHover(Selector selector) {
        ItemDisplay display = selector.itemDisplay();
        display.setTransformation(createTransformation(selector.baseScale(), selector.rotation()));
    }

    private void handleSelection(Player player, Selector selector) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.0f);
        sendToServer(player, selector.serverName());
    }

    private void sendToServer(Player player, String serverName) {
        if (serverName == null || serverName.isBlank()) {
            player.sendMessage(Component.text("Kein Server für diesen Selector gesetzt."));
            return;
        }
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(stream)) {
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", stream.toByteArray());
        } catch (IOException ex) {
            player.sendMessage(Component.text("Server-Verbindung fehlgeschlagen."));
        }
    }

    private Transformation createTransformation(float scale, float rotation) {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf().rotateY(rotation),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        );
    }

    @Nullable
    private Selector findNearestSelector(Player player) {
        Location origin = player.getEyeLocation();
        double maxDistanceSquared = MAX_INTERACT_DISTANCE * MAX_INTERACT_DISTANCE;
        Selector nearest = null;
        double nearestDistance = maxDistanceSquared;
        for (Selector selector : selectors.values()) {
            ItemDisplay display = selector.itemDisplay();
            if (!display.isValid()) {
                continue;
            }
            double distanceSquared = display.getLocation().distanceSquared(origin);
            if (distanceSquared <= nearestDistance) {
                nearestDistance = distanceSquared;
                nearest = selector;
            }
        }
        return nearest;
    }

    private void removeSelector(Selector selector) {
        selectors.remove(selector.itemDisplay().getUniqueId());
        selectors.remove(selector.interaction().getUniqueId());
        selector.itemDisplay().remove();
        selector.interaction().remove();
    }

    public void loadSelectors() {
        selectors.clear();
        hoveredByPlayer.clear();
        loading = true;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("selectors");
        if (section == null) {
            loading = false;
            return;
        }
        removeLegacyEntitiesForConfig(section);
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String worldName = entry.getString("world");
            World world = worldName == null ? null : plugin.getServer().getWorld(worldName);
            if (world == null) {
                continue;
            }
            double x = entry.getDouble("x");
            double y = entry.getDouble("y");
            double z = entry.getDouble("z");
            String materialName = entry.getString("material", "");
            String serverName = entry.getString("server", "");
            float size = (float) entry.getDouble("size", 1.0);
            float rotation = (float) entry.getDouble("rotation", 0.0);
            Location location = new Location(world, x, y, z);
            removeLegacyEntities(world, location);
            spawnSelectorAt(location, materialName, Float.toString(size), serverName, rotation, null);
        }
        loading = false;
        saveSelectors();
    }

    public void saveSelectors() {
        plugin.getConfig().set("selectors", null);
        ConfigurationSection section = plugin.getConfig().createSection("selectors");
        Set<Selector> uniqueSelectors = new HashSet<>(selectors.values());
        int index = 0;
        for (Selector selector : uniqueSelectors) {
            Location location = selector.location();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }
            ConfigurationSection entry = section.createSection(Integer.toString(index++));
            entry.set("world", world.getName());
            entry.set("x", location.getX());
            entry.set("y", location.getY());
            entry.set("z", location.getZ());
            entry.set("material", selector.material().name());
            entry.set("size", selector.baseScale());
            entry.set("server", selector.serverName());
            entry.set("rotation", selector.rotation());
        }
        plugin.saveConfig();
    }

    public void cleanupSpawnedEntities() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getScoreboardTags().contains(SELECTOR_TAG)) {
                    entity.remove();
                }
            }
        }
    }

    private void removeLegacyEntities(World world, Location location) {
        for (Entity entity : world.getNearbyEntities(location, LEGACY_CLEANUP_RADIUS, LEGACY_CLEANUP_RADIUS, LEGACY_CLEANUP_RADIUS)) {
            if (entity instanceof ItemDisplay || entity instanceof Interaction) {
                entity.remove();
            }
        }
    }

    private void removeLegacyEntitiesForConfig(ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String worldName = entry.getString("world");
            World world = worldName == null ? null : plugin.getServer().getWorld(worldName);
            if (world == null) {
                continue;
            }
            double x = entry.getDouble("x");
            double y = entry.getDouble("y");
            double z = entry.getDouble("z");
            Location location = new Location(world, x, y, z);
            removeLegacyEntities(world, location);
        }
    }

    @Nullable
    private Player findNearestPlayer(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player player : world.getPlayers()) {
            double distance = player.getLocation().distanceSquared(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    public void clearAllSelectors() {
        cleanupSpawnedEntities();
        selectors.clear();
        hoveredByPlayer.clear();
        plugin.getConfig().set("selectors", null);
        plugin.saveConfig();
    }
}
