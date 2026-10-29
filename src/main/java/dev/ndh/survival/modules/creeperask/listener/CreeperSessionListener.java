package dev.ndh.survival.modules.creeperask.listener;

import dev.ndh.survival.modules.creeperask.gui.AskInventoryHolder;
import dev.ndh.survival.modules.creeperask.gui.GuiManager;
import dev.ndh.survival.modules.creeperask.session.SessionManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CreeperSessionListener implements Listener {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final JavaPlugin plugin;
    private final SessionManager sessionManager;

    public CreeperSessionListener(JavaPlugin plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AskInventoryHolder) {
            HumanEntity whoClicked = event.getWhoClicked();
            if (whoClicked instanceof Player player) {
                event.setCancelled(true);
                if (event.getRawSlot() == GuiManager.ACCEPT_SLOT) {
                    this.sessionManager.accept(player);
                    player.closeInventory();
                } else if (event.getRawSlot() == GuiManager.DENY_SLOT) {
                    if (this.sessionManager.deny(player)) {
                        this.plugin.getServer().broadcast(MINI_MESSAGE.deserialize(
                                "<red><player></red> <gray>es un cagón, no se animó a ser <yellow>explotado</yellow></gray>",
                                Placeholder.unparsed("player", player.getName())));
                    }

                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AskInventoryHolder
                && event.getRawSlots().stream().anyMatch((slot) -> slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AskInventoryHolder
                && event.getPlayer() instanceof Player player
                && this.sessionManager.hasSession(player.getUniqueId())) {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (player.isOnline() && this.sessionManager.hasSession(player.getUniqueId())) {
                    GuiManager.open(player);
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.sessionManager.deny(event.getPlayer());
    }
}
