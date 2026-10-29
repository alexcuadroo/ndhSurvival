package dev.ndh.survival.modules.rtp.listener;

import dev.ndh.survival.modules.rtp.RtpModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener encargado de cancelar warmups de teletransporte si el jugador se mueve, recibe daño o desconecta.
 */
public final class RtpListener implements Listener {

    private final RtpModule module;

    public RtpListener(RtpModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        module.handlePlayerMove(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        module.handlePlayerDamage(event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        module.handlePlayerQuit(event.getPlayer().getUniqueId());
    }
}
