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

public final class GamemodeSelector extends JavaPlugin implements CommandExecutor, TabCompleter {
    private SelectorManager selectorManager;

    @Override
    public void onEnable() {
        selectorManager = new SelectorManager(this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(selectorManager, this);
        getServer().getPluginManager().registerEvents(new HoverListener(selectorManager), this);
        getServer().getScheduler().runTaskTimer(this, selectorManager::tickRotation, 1L, 1L);
        getCommand("gamemode").setExecutor(this);
        getCommand("gamemode").setTabCompleter(this);
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
        if (args.length < 5 || !"set".equalsIgnoreCase(args[0])) {
            sender.sendMessage(Component.text("Usage: /gamemode set <material> <size> <server> <minimessage>"));
            return true;
        }
        String materialName = args[1];
        String sizeInput = args[2];
        String server = args[3];
        String minimessage = String.join(" ", List.of(args).subList(4, args.length));
        selectorManager.spawnSelector(player, materialName, sizeInput, server, minimessage);
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
