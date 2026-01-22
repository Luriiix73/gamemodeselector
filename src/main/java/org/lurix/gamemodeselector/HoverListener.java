package org.lurix.gamemodeselector;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class HoverListener implements Listener {
    private final SelectorManager selectorManager;

    public HoverListener(SelectorManager selectorManager) {
        this.selectorManager = selectorManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        selectorManager.updateHover(player, selectorManager.rayTraceSelector(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        selectorManager.updateHover(player, selectorManager.rayTraceSelector(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        selectorManager.updateHover(event.getPlayer(), null);
    }
}
