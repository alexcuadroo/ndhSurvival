package dev.ndh.survival.modules.barrendero;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.barrendero.command.BarrenderoSubCommand;
import org.jspecify.annotations.Nullable;

/**
 * Módulo Barrendero: limpieza periódica de ítems en el suelo con avisos configurables.
 */
public final class BarrenderoModule implements SurvivalModule {

    private final NdhSurvivalPlugin plugin;
    private @Nullable CleanupManager cleanupManager;
    private @Nullable LoggerHelper logger;
    private @Nullable BarrenderoConfig config;

    public BarrenderoModule(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public NdhSurvivalPlugin getPlugin() {
        return plugin;
    }

    public @Nullable CleanupManager getCleanupManager() {
        return cleanupManager;
    }

    public @Nullable BarrenderoConfig getConfig() {
        return config;
    }

    @Override
    public String getId() {
        return "barrendero";
    }

    @Override
    public String getDisplayName() {
        return "Barrendero";
    }

    @Override
    public String getDescription() {
        return "Limpieza automática de items en el suelo con avisos previos en el chat.";
    }

    @Override
    public void onEnable() {
        LoggerHelper logger = new LoggerHelper(plugin);
        BarrenderoConfig config = new BarrenderoConfig(plugin.getConfig().getConfigurationSection("barrendero"));
        this.logger = logger;
        this.config = config;

        CleanupManager cleanupManager = new CleanupManager(plugin, config, logger);
        this.cleanupManager = cleanupManager;
        cleanupManager.start();

        plugin.getCommandRouter().registerSubCommand(new BarrenderoSubCommand(this));
    }

    @Override
    public void onDisable() {
        CleanupManager cleanupManager = this.cleanupManager;
        if (cleanupManager != null) {
            cleanupManager.stop();
        }
    }

    @Override
    public void onReload() {
        BarrenderoConfig config = new BarrenderoConfig(plugin.getConfig().getConfigurationSection("barrendero"));
        this.config = config;
        CleanupManager cleanupManager = this.cleanupManager;
        if (cleanupManager != null) {
            cleanupManager.reloadConfig(config);
        }
    }

    @Override
    public boolean isEnabled() {
        CleanupManager cleanupManager = this.cleanupManager;
        BarrenderoConfig config = this.config;
        return config != null && config.isEnabled() && cleanupManager != null && cleanupManager.isRunning();
    }
}
