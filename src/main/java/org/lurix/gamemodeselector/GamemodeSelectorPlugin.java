package org.lurix.gamemodeselector;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class GamemodeSelectorPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private SelectorManager selectorManager;

    @Override
    public void onEnable() {
        selectorManager = new SelectorManager(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(selectorManager, this);
        getServer().getPluginManager().registerEvents(new HoverListener(selectorManager), this);
        getServer().getScheduler().runTaskTimer(this, selectorManager::tickRotation, 1L, 1L);
        registerCommand("selector");
        registerCommand("gamemode");
        selectorManager.cleanupSpawnedEntities();
        selectorManager.loadSelectors();
    }

    @Override
    public void onDisable() {
        if (selectorManager != null) {
            selectorManager.shutdown();
        }
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Nur Spieler können dieses Kommando nutzen."));
            return true;
        }
        if (selectorManager == null) {
            sender.sendMessage(Component.text("Selector-Manager ist nicht verfügbar."));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /selector set <material> <size>"));
            sender.sendMessage(Component.text("Usage: /selector add (<server>)"));
            sender.sendMessage(Component.text("Usage: /selector remove"));
            sender.sendMessage(Component.text("Usage: /selector clear"));
            return true;
        }
        if ("remove".equalsIgnoreCase(args[0]) || "remvove".equalsIgnoreCase(args[0])) {
            selectorManager.removeNearestSelector(player);
            return true;
        }
        if ("clear".equalsIgnoreCase(args[0])) {
            selectorManager.clearAllSelectors();
            sender.sendMessage(Component.text("Alle Gamemode-Selectoren entfernt."));
            return true;
        }
        if ("add".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage(Component.text("Usage: /selector add (<server>)"));
                return true;
            }
            String serverName = args[1];
            if (serverName.startsWith("(") && serverName.endsWith(")") && serverName.length() > 2) {
                serverName = serverName.substring(1, serverName.length() - 1);
            }
            selectorManager.setNearestSelectorServer(player, serverName);
            return true;
        }
        if ("set".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /selector set <material> <size>"));
                return true;
            }
            String materialName = args[1];
            String sizeInput = args[2];
            selectorManager.spawnSelector(player, materialName, sizeInput);
            return true;
        }
        sender.sendMessage(Component.text("Usage: /selector set <material> <size>"));
        sender.sendMessage(Component.text("Usage: /selector add (<server>)"));
        sender.sendMessage(Component.text("Usage: /selector remove"));
        sender.sendMessage(Component.text("Usage: /selector clear"));
        return true;
    }

    private void registerCommand(String name) {
        var command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' not found. Check plugin.yml.");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        return selectorManager.tabComplete(args);
    }
}
