package org.lurix.gamemodeselector;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

public final class JoinEntityListener implements Listener {
    private final JoinEntityManager joinEntityManager;

    public JoinEntityListener(@NotNull JoinEntityManager joinEntityManager) {
        this.joinEntityManager = joinEntityManager;
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
        JoinEntityData data = joinEntityManager.getJoinEntity(entity);
        if (data == null) {
            return;
        }
        event.setCancelled(true);
        joinEntityManager.connect(player, data.targetServer());
    }
}
