package dev.ndh.survival.modules.barrendero;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.Nullable;

public class CleanupManager {
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private final LoggerHelper logger;
    private BarrenderoConfig config;
    private @Nullable BukkitTask task;
    private @Nullable BukkitTask warningTask;

    public CleanupManager(JavaPlugin plugin, BarrenderoConfig config, LoggerHelper logger) {
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
    }

    public void start() {
        stop();
        if (!config.isEnabled()) {
            return;
        }

        long intervalTicks = Math.max(1, config.getIntervalSeconds()) * 20L;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, (Runnable) this::runCleanup, intervalTicks,
                intervalTicks);

        if (config.isMessagesEnabled() && config.getWarnBeforeSeconds() > 0
                && config.getWarnBeforeSeconds() < config.getIntervalSeconds()) {
            long warnDelayTicks = intervalTicks - (config.getWarnBeforeSeconds() * 20L);
            warningTask = plugin.getServer().getScheduler().runTaskTimer(plugin, (Runnable) this::sendWarning,
                    warnDelayTicks, intervalTicks);
        }

        logger.info("Tarea de limpieza programada cada " + config.getIntervalSeconds() + "s.");
    }

    public void stop() {
        BukkitTask task = this.task;
        if (task != null) {
            task.cancel();
            this.task = null;
        }
        BukkitTask warningTask = this.warningTask;
        if (warningTask != null) {
            warningTask.cancel();
            this.warningTask = null;
        }
    }

    public void reloadConfig(BarrenderoConfig config) {
        this.config = config;
        start();
    }

    private void sendWarning() {
        if (!config.isMessagesEnabled() || !config.isEnabled())
            return;
        String raw = config.getWarningMessage()
                .replace("{prefix}", config.getPrefix())
                .replace("{seconds}", String.valueOf(config.getWarnBeforeSeconds()));
        Bukkit.getServer().sendMessage(MINI.deserialize(raw));
    }

    public int runCleanup() {
        return runCleanup(false);
    }

    public int runCleanup(boolean forceAllAges) {
        int removed = 0;
        for (World world : plugin.getServer().getWorlds()) {
            if (!config.isWorldAllowed(world.getName()))
                continue;
            for (Item item : world.getEntitiesByClass(Item.class)) {
                int ticksLived = item.getTicksLived();
                if (forceAllAges || ticksLived >= config.getAgeSeconds() * 20L) {
                    item.remove();
                    removed++;
                }
            }
        }
        if (removed > 0) {
            logger.info("Eliminados " + removed + " items del suelo.");
            if (config.isMessagesEnabled()) {
                String raw = config.getCleanupMessage()
                        .replace("{prefix}", config.getPrefix())
                        .replace("{removed}", String.valueOf(removed));
                Bukkit.getServer().sendMessage(MINI.deserialize(raw));
            }
        }
        return removed;
    }

    public boolean isRunning() {
        return task != null;
    }
}
