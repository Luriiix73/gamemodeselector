package org.lurix.gamemodeselector;

import java.util.List;
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
        var command = getCommand("selector");
        if (command == null) {
            getLogger().severe("Command 'selector' not found. Check plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
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
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /selector set <material> <size> <server> <minimessage>"));
            sender.sendMessage(Component.text("Usage: /selector remvove"));
            return true;
        }
        if ("set".equalsIgnoreCase(args[0])) {
            if (args.length < 5) {
                sender.sendMessage(Component.text("Usage: /selector set <material> <size> <server> <minimessage>"));
                return true;
            }
            String materialName = args[1];
            String sizeInput = args[2];
            String server = args[3];
            String minimessage = String.join(" ", List.of(args).subList(4, args.length));
            selectorManager.spawnSelector(player, materialName, sizeInput, server, minimessage);
            return true;
        }
        if ("remvove".equalsIgnoreCase(args[0])) {
            selectorManager.removeNearestSelector(player);
            return true;
        }
        sender.sendMessage(Component.text("Usage: /selector set <material> <size> <server> <minimessage>"));
        sender.sendMessage(Component.text("Usage: /selector remvove"));
        return true;
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
