package dev.ndh.survival.modules.jointg;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.jointg.command.TelegramSubCommand;
import dev.ndh.survival.modules.jointg.listener.TelegramListener;
import dev.ndh.survival.modules.jointg.telegram.TelegramNotifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;

/**
 * Gestiona exclusivamente las notificaciones a Telegram.
 */
public final class JoinTgModule implements SurvivalModule {

    private final NdhSurvivalPlugin plugin;
    private @Nullable TelegramNotifier notifier;
    private @Nullable TelegramListener listenerInstance;

    public JoinTgModule(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public NdhSurvivalPlugin getPlugin() {
        return plugin;
    }

    public @Nullable TelegramNotifier getNotifier() {
        return notifier;
    }

    @Override
    public String getId() {
        return "jointg";
    }

    @Override
    public String getDisplayName() {
        return "JoinTelegram";
    }

    @Override
    public String getDescription() {
        return "Envía notificaciones a Telegram (entrada, salida, muerte e inicio).";
    }

    @Override
    public void onEnable() {
        reloadModuleConfig(true);
        plugin.getCommandRouter().registerSubCommand(new TelegramSubCommand(this));
    }

    @Override
    public void onDisable() {
        if (listenerInstance != null) {
            HandlerList.unregisterAll(listenerInstance);
            listenerInstance = null;
        }
        if (notifier != null) {
            notifier.close();
            notifier = null;
        }
    }

    @Override
    public void onReload() {
        reloadModuleConfig(false);
    }

    @Override
    public boolean isEnabled() {
        TelegramNotifier current = notifier;
        return current != null && current.isEnabled();
    }

    private void reloadModuleConfig(boolean announceStart) {
        if (listenerInstance != null) {
            HandlerList.unregisterAll(listenerInstance);
            listenerInstance = null;
        }
        if (notifier != null) {
            notifier.close();
            notifier = null;
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("jointg");
        if (sec == null) {
            sec = plugin.getConfig().createSection("jointg");
        }

        if (!sec.getBoolean("enabled", false)) {
            return;
        }

        String botToken = sec.getString("bot-token", "").trim();
        String chatId = sec.getString("chat-id", "").trim();
        boolean telegramEnabled = sec.getBoolean("telegram-enabled", false);

        String joinMessage = sec.getString("join-message",
                sec.getString("message-template", "Player %player% joined"));
        String quitMessage = sec.getString("quit-message", "Player %player% left");
        String deathMessage = sec.getString("death-message",
                "Player %player% died: %reason% at %world% (%x%, %y%, %z%)");
        String startMessage = sec.getString("start-message", "Server started!");

        TelegramNotifier notifier;
        if (!telegramEnabled) {
            notifier = TelegramNotifier.disabled(plugin.getComponentLogger());
        } else if (botToken.isEmpty() || chatId.isEmpty()) {
            plugin.getComponentLogger().warn("Telegram bot token or chat ID is missing; Telegram notifications disabled.");
            notifier = TelegramNotifier.disabled(plugin.getComponentLogger());
        } else {
            notifier = TelegramNotifier.enabled(
                    plugin.getComponentLogger(),
                    botToken,
                    chatId,
                    joinMessage,
                    quitMessage,
                    deathMessage,
                    startMessage
            );
        }
        this.notifier = notifier;

        TelegramListener listener = new TelegramListener(plugin, notifier);
        this.listenerInstance = listener;

        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        if (announceStart) {
            notifier.notifyStart(plugin);
        }
    }
}
