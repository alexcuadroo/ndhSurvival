package dev.ndh.survival.modules.jointg.listener;

import dev.ndh.survival.modules.jointg.telegram.TelegramNotifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/** Envía eventos a Telegram sin modificar los mensajes del servidor. */
public final class TelegramListener implements Listener {
    private final Plugin plugin;
    private final TelegramNotifier notifier;

    /**
     * Crea el listener de Telegram.
     * @param plugin plugin propietario
     * @param notifier servicio de notificaciones
     */
    public TelegramListener(Plugin plugin, TelegramNotifier notifier) {
        this.plugin = plugin;
        this.notifier = notifier;
    }

    /** @param event entrada del jugador */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        notifier.notifyJoin(plugin, event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        notifier.notifyQuit(plugin, event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String playerName = event.getEntity().getName();

        Component deathMessageComponent = event.deathMessage();
        String reason = "died";
        if (deathMessageComponent != null) {
            reason = PlainTextComponentSerializer.plainText().serialize(deathMessageComponent);
        }

        var loc = event.getEntity().getLocation();
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";

        notifier.notifyDeath(plugin, playerName, reason, world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
