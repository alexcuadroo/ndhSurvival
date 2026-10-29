package dev.ndh.survival.modules.jointg;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.jointg.command.TelegramSubCommand;
import dev.ndh.survival.modules.jointg.integration.PrefixProvider;
import dev.ndh.survival.modules.jointg.listener.PluginListener;
import dev.ndh.survival.modules.jointg.telegram.TelegramNotifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.jspecify.annotations.Nullable;

/**
 * Módulo JoinTelegram: saluda a los jugadores con títulos/mensajes y notifica a Telegram al entrar, salir o morir.
 */
public final class JoinTgModule implements SurvivalModule {

    private final NdhSurvivalPlugin plugin;
    private @Nullable TelegramNotifier notifier;
    private @Nullable PluginListener listenerInstance;
    private @Nullable PrefixProvider prefixProvider;

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
        return "Saluda a los jugadores y envía notificaciones a Telegram (join, quit, death, start).";
    }

    @Override
    public void onEnable() {
        prefixProvider = PrefixProvider.createIfAvailable(plugin);
        if (prefixProvider == null) {
            plugin.getComponentLogger().info("LuckPerms not found or not enabled; join prefixes will be empty.");
        }

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
        return listenerInstance != null;
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

        String chatJoinMessage = sec.getString("chat-join-message", "");
        String joinTitle = sec.getString("join-title", "<gradient:#7f6bdb:#f79459:red>Welcome <player>!</gradient>");
        String joinSubtitle = sec.getString("join-subtitle", "<gray>Enjoy your stay!");
        int fadeIn = sec.getInt("title-fade-in", 20);
        int stay = sec.getInt("title-stay", 50);
        int fadeOut = sec.getInt("title-fade-out", 20);

        PluginListener listener = new PluginListener(
                plugin,
                notifier,
                prefixProvider,
                joinTitle,
                joinSubtitle,
                fadeIn,
                stay,
                fadeOut,
                chatJoinMessage
        );
        this.listenerInstance = listener;

        plugin.getServer().getPluginManager().registerEvents(listener, plugin);

        if (announceStart) {
            notifier.notifyStart(plugin);
        }
    }
}
