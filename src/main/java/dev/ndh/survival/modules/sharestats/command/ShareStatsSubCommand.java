package dev.ndh.survival.modules.sharestats.command;

import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.sharestats.ShareStatsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Subcomando para controlar la sincronización global de estadísticas (/ndh estadisticas).
 */
public final class ShareStatsSubCommand implements NdhSubCommand {

    private final ShareStatsModule module;

    public ShareStatsSubCommand(ShareStatsModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "estadisticas";
    }

    @Override
    public List<String> getAliases() {
        return List.of("sharestats", "stats");
    }

    @Override
    public String getDescription() {
        return "Activa, desactiva o consulta la sincronización de salud, hambre y efectos.";
    }

    @Override
    public String getUsage() {
        return "/ndh estadisticas <activar|desactivar|estado|recargar>";
    }

    @Override
    public @Nullable String getPermission() {
        return "sharestats.admin";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("estado") || args[0].equalsIgnoreCase("status")) {
            boolean active = module.isEnabled();
            sender.sendMessage(Component.text("ShareStats está ", NamedTextColor.YELLOW)
                    .append(Component.text(active ? "activado" : "desactivado",
                            active ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.text(".", NamedTextColor.YELLOW)));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("recargar") || sub.equals("reload")) {
            module.getPlugin().reloadConfig();
            module.onReload();
            sender.sendMessage(Component.text("[ShareStats] Configuración recargada correctamente.", NamedTextColor.GREEN));
            return;
        }

        if (sub.equals("activar") || sub.equals("on") || sub.equals("desactivar") || sub.equals("off")) {
            boolean shouldEnable = sub.equals("activar") || sub.equals("on");
            boolean current = module.isEnabled();
            if (current == shouldEnable) {
                sender.sendMessage(Component.text("ShareStats ya está ", NamedTextColor.YELLOW)
                        .append(Component.text(current ? "activado" : "desactivado",
                                current ? NamedTextColor.GREEN : NamedTextColor.RED))
                        .append(Component.text(".", NamedTextColor.YELLOW)));
                return;
            }

            module.setStatsEnabled(shouldEnable);
            if (shouldEnable) {
                sender.sendMessage(Component.text("ShareStats activado. Las estadísticas del primer jugador en línea se compartirán.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("ShareStats desactivado. Los jugadores conservarán sus estadísticas actuales.", NamedTextColor.RED));
            }
            return;
        }

        sender.sendMessage(Component.text("Uso: /ndh estadisticas <activar|desactivar|estado|recargar>", NamedTextColor.RED));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("activar", "desactivar", "estado", "recargar").stream()
                    .filter(option -> option.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
