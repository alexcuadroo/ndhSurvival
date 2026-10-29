package dev.ndh.survival.modules.creeperask;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.creeperask.command.CreeperSubCommand;
import dev.ndh.survival.modules.creeperask.listener.CreeperExplosionListener;
import dev.ndh.survival.modules.creeperask.listener.CreeperSessionListener;
import dev.ndh.survival.modules.creeperask.session.SessionManager;
import dev.ndh.survival.modules.creeperask.task.CreeperProximityTask;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

/**
 * Módulo CreeperAsk: pide permiso en un menú antes de que un creeper pueda explotar cerca de un jugador.
 */
public final class CreeperAskModule implements SurvivalModule {

    private final NdhSurvivalPlugin plugin;
    private @Nullable SessionManager sessionManager;
    private @Nullable BukkitTask proximityTask;
    private boolean globallyEnabled = true;

    public CreeperAskModule(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "creeperask";
    }

    @Override
    public String getDisplayName() {
        return "CreeperAsk";
    }

    @Override
    public String getDescription() {
        return "Pide permiso antes de que un creeper explote cerca de un jugador.";
    }

    @Override
    public void onEnable() {
        this.globallyEnabled = plugin.getConfig().getBoolean("creeperask.enabled", true);
        SessionManager sessionManager = new SessionManager(plugin);
        this.sessionManager = sessionManager;

        plugin.getServer().getPluginManager().registerEvents(new CreeperSessionListener(plugin, sessionManager), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CreeperExplosionListener(sessionManager), plugin);

        this.proximityTask = new CreeperProximityTask(plugin, this, sessionManager).runTaskTimer(plugin, 10L, 10L);

        plugin.getCommandRouter().registerSubCommand(new CreeperSubCommand(this));
    }

    @Override
    public void onDisable() {
        BukkitTask proximityTask = this.proximityTask;
        if (proximityTask != null) {
            proximityTask.cancel();
            this.proximityTask = null;
        }
        SessionManager sessionManager = this.sessionManager;
        if (sessionManager != null) {
            sessionManager.releaseAll();
        }
    }

    @Override
    public void onReload() {
        boolean configured = plugin.getConfig().getBoolean("creeperask.enabled", true);
        setGloballyEnabled(configured);
    }

    @Override
    public boolean isEnabled() {
        return globallyEnabled;
    }

    public void setGloballyEnabled(boolean globallyEnabled) {
        this.globallyEnabled = globallyEnabled;
        SessionManager sessionManager = this.sessionManager;
        if (!globallyEnabled && sessionManager != null) {
            sessionManager.releaseAll();
        }
        plugin.getConfig().set("creeperask.enabled", globallyEnabled);
        plugin.saveConfig();
    }
}
