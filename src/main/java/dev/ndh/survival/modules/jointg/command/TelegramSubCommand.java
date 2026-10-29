package dev.ndh.survival.modules.jointg.command;

import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.jointg.JoinTgModule;
import dev.ndh.survival.modules.jointg.telegram.TelegramNotifier;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Subcomando para administrar las notificaciones de Telegram (/ndh telegram).
 */
public final class TelegramSubCommand implements NdhSubCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final JoinTgModule module;

    public TelegramSubCommand(JoinTgModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "telegram";
    }

    @Override
    public List<String> getAliases() {
        return List.of("jointg", "tg");
    }

    @Override
    public String getDescription() {
        return "Administra los avisos y notificaciones de Telegram.";
    }

    @Override
    public String getUsage() {
        return "/ndh telegram <activar|desactivar|estado|recargar>";
    }

    @Override
    public @Nullable String getPermission() {
        return "jointg.reload";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("estado") || args[0].equalsIgnoreCase("status")) {
            TelegramNotifier notifier = module.getNotifier();
            boolean tgActive = notifier != null && notifier.isEnabled();
            sender.sendMessage(MINI_MESSAGE.deserialize(
                    "<yellow>[Telegram] Notificaciones: "
                            + (tgActive ? "<green>ACTIVAS</green>" : "<red>DESACTIVADAS</red>")));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("recargar") || sub.equals("reload")) {
            module.getPlugin().reloadConfig();
            module.onReload();
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>[Telegram] Configuración recargada correctamente.</green>"));
            return;
        }

        if (sub.equals("activar") || sub.equals("on") || sub.equals("desactivar") || sub.equals("off")) {
            boolean enableTg = sub.equals("activar") || sub.equals("on");
            module.getPlugin().getConfig().set("jointg.enabled", enableTg);
            module.getPlugin().getConfig().set("jointg.telegram-enabled", enableTg);
            module.getPlugin().saveConfig();
            module.onReload();
            sender.sendMessage(MINI_MESSAGE.deserialize(
                    "<yellow>[Telegram] Notificaciones de Telegram "
                            + (module.isEnabled() ? "<green>activadas</green>." : "<red>desactivadas</red> (revisa la configuración).")));
            return;
        }

        sender.sendMessage(MINI_MESSAGE.deserialize("<yellow>Uso: /ndh telegram <activar|desactivar|estado|recargar></yellow>"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("activar", "desactivar", "estado", "recargar").stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
