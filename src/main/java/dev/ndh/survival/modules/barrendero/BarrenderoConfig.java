package dev.ndh.survival.modules.barrendero;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class BarrenderoConfig {
    private final boolean enabled;
    private final int intervalSeconds;
    private final int ageSeconds;
    private final double radius;
    private final List<String> worlds;

    private final boolean messagesEnabled;
    private final int warnBeforeSeconds;
    private final String prefix;
    private final String cleanupMessage;
    private final String warningMessage;

    public BarrenderoConfig(@Nullable ConfigurationSection cfg) {
        this.enabled = cfg == null || cfg.getBoolean("enabled", true);
        this.intervalSeconds = Math.max(1, cfg != null ? cfg.getInt("interval-seconds", 60) : 60);
        this.ageSeconds = Math.max(0, cfg != null ? cfg.getInt("age-seconds", 15) : 15);
        this.radius = Math.max(0.0, cfg != null ? cfg.getDouble("radius", 0.0) : 0.0);
        this.worlds = cfg != null ? cfg.getStringList("worlds") : List.of();

        this.messagesEnabled = cfg == null || cfg.getBoolean("messages.enabled", true);
        this.warnBeforeSeconds = Math.max(0, cfg != null ? cfg.getInt("messages.warn-before-seconds", 30) : 30);
        this.prefix = cfg != null ? cfg.getString("messages.prefix", "[SERVER]") : "[SERVER]";
        this.cleanupMessage = cfg != null ? cfg.getString("messages.cleanup-message",
                "<green>{prefix} <white>Eliminados <yellow>{removed} <white>items del suelo.")
                : "<green>{prefix} <white>Eliminados <yellow>{removed} <white>items del suelo.";
        this.warningMessage = cfg != null ? cfg.getString("messages.warning-message",
                "<gold>{prefix} <yellow>En <red>{seconds} <yellow>segundos se realizará una limpieza del suelo.")
                : "<gold>{prefix} <yellow>En <red>{seconds} <yellow>segundos se realizará una limpieza del suelo.";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public int getAgeSeconds() {
        return ageSeconds;
    }

    public double getRadius() {
        return radius;
    }

    public boolean isWorldAllowed(String name) {
        return worlds.isEmpty() || worlds.contains(name);
    }

    public boolean isMessagesEnabled() {
        return messagesEnabled;
    }

    public int getWarnBeforeSeconds() {
        return warnBeforeSeconds;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCleanupMessage() {
        return cleanupMessage;
    }

    public String getWarningMessage() {
        return warningMessage;
    }
}
