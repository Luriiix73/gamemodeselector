package org.lurix.gamemodeselector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
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
    private static final float HOVER_SCALE_MULTIPLIER = 1.3f;
    private static final double MAX_INTERACT_DISTANCE = 10.0;
    private static final float ROTATION_STEP = 0.015f;

    private final Plugin plugin;
    private final Map<UUID, Selector> selectors = new HashMap<>();
    private final Map<UUID, UUID> hoveredByPlayer = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public SelectorManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void spawnSelector(
            @NotNull Player player,
            @NotNull String materialName,
            @NotNull String sizeInput,
            @NotNull String serverName,
            @NotNull String minimessage
    ) {
        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            player.sendMessage(Component.text("Unbekanntes Material: " + materialName));
            return;
        }
        float size;
        try {
            size = Float.parseFloat(sizeInput);
        } catch (NumberFormatException ex) {
            player.sendMessage(Component.text("Größe muss eine Zahl sein."));
            return;
        }
        if (size <= 0.1f) {
            player.sendMessage(Component.text("Größe muss größer als 0.1 sein."));
            return;
        }

        Location baseLocation = player.getLocation().clone();
        ItemDisplay itemDisplay = player.getWorld().spawn(baseLocation, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(material));
            display.setBillboard(Display.Billboard.FIXED);
            display.setGlowing(true);
            display.setTransformation(createTransformation(size, 0f));
        });

        float textScale = Math.max(0.5f, size * 0.6f);
        double textYOffset = size * 0.7 + 0.8;
        Location textLocation = baseLocation.clone().add(0, textYOffset, 0);
        TextDisplay textDisplay = player.getWorld().spawn(textLocation, TextDisplay.class, display -> {
            display.text(miniMessage.deserialize(minimessage));
            display.setBillboard(Display.Billboard.CENTER);
            display.setTransformation(createTextTransformation(textScale));
            display.setSeeThrough(true);
        });

        Selector selector = new Selector(itemDisplay, textDisplay, size, serverName);
        selectors.put(itemDisplay.getUniqueId(), selector);
        selectors.put(textDisplay.getUniqueId(), selector);

        player.sendMessage(Component.text("Gamemode-Selector erstellt."));
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
        for (Selector selector : selectors.values()) {
            ItemDisplay display = selector.itemDisplay();
            if (!display.isValid()) {
                continue;
            }
            float nextRotation = selector.rotation() + ROTATION_STEP;
            selector.setRotation(nextRotation);
            display.setTransformation(createTransformation(selector.baseScale(), nextRotation));
        }
    }

    public void shutdown() {
        for (Selector selector : selectors.values()) {
            selector.itemDisplay().remove();
            selector.textDisplay().remove();
        }
        selectors.clear();
        hoveredByPlayer.clear();
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            List<String> materials = new ArrayList<>();
            for (Material material : Material.values()) {
                if (!material.isAir()) {
                    materials.add(material.name().toLowerCase());
                }
            }
            return materials;
        }
        if (args.length == 1) {
            return List.of("set", "remove");
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

    private Transformation createTextTransformation(float scale) {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
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
        selectors.remove(selector.textDisplay().getUniqueId());
        selector.itemDisplay().remove();
        selector.textDisplay().remove();
    }
}
