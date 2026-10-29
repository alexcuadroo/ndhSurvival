package dev.ndh.survival.modules.saludo;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.saludo.command.SaludoSubCommand;
import dev.ndh.survival.modules.saludo.integration.PrefixProvider;
import dev.ndh.survival.modules.saludo.listener.SaludoListener;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;

/** Gestiona la bienvenida en Minecraft independientemente de Telegram. */
public final class SaludoModule implements SurvivalModule {
    private final NdhSurvivalPlugin plugin;
    private @Nullable SaludoListener listener;

    /** @param plugin plugin propietario */
    public SaludoModule(NdhSurvivalPlugin plugin) { this.plugin = plugin; }

    /** @return plugin propietario */
    public NdhSurvivalPlugin getPlugin() { return plugin; }

    /** {@inheritDoc} */
    @Override public String getId() { return "saludo"; }
    /** {@inheritDoc} */
    @Override public String getDisplayName() { return "Saludo"; }
    /** {@inheritDoc} */
    @Override public String getDescription() { return "Títulos y mensajes de entrada con prefijos de LuckPerms."; }

    /** {@inheritDoc} */
    @Override public void onEnable() {
        migrateLegacyConfig();
        plugin.getCommandRouter().registerSubCommand(new SaludoSubCommand(this));
        onReload();
    }

    private void migrateLegacyConfig() {
        var config = plugin.getConfig();
        if (config.contains("saludo", true) || !config.contains("jointg", true)) { return; }
        config.set("saludo.enabled", config.getBoolean("jointg.enabled", false));
        for (String key : java.util.List.of("chat-join-message", "join-title", "join-subtitle",
                "title-fade-in", "title-stay", "title-fade-out")) {
            if (config.contains("jointg." + key, true)) {
                config.set("saludo." + key, config.get("jointg." + key));
            }
        }
        plugin.saveConfig();
    }

    /** {@inheritDoc} */
    @Override public void onDisable() {
        SaludoListener current = listener;
        if (current != null) {
            HandlerList.unregisterAll(current);
            listener = null;
        }
    }

    /** {@inheritDoc} */
    @Override public void onReload() {
        onDisable();
        var config = plugin.getConfig();
        if (!config.getBoolean("saludo.enabled", false)) { return; }
        SaludoListener current = new SaludoListener(PrefixProvider.createIfAvailable(plugin),
                config.getString("saludo.join-title", ""), config.getString("saludo.join-subtitle", ""),
                Math.max(0, config.getInt("saludo.title-fade-in", 20)),
                Math.max(0, config.getInt("saludo.title-stay", 50)),
                Math.max(0, config.getInt("saludo.title-fade-out", 20)),
                config.getString("saludo.chat-join-message", ""));
        listener = current;
        plugin.getServer().getPluginManager().registerEvents(current, plugin);
    }

    /** {@inheritDoc} */
    @Override public boolean isEnabled() { return listener != null; }
}
