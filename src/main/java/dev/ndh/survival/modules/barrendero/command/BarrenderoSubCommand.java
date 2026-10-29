package dev.ndh.survival.modules.barrendero.command;

import java.util.List;
import java.util.Locale;

import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.barrendero.BarrenderoConfig;
import dev.ndh.survival.modules.barrendero.BarrenderoModule;
import dev.ndh.survival.modules.barrendero.CleanupManager;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Subcomando para administrar y ejecutar la limpieza de items del suelo (/ndh
 * barrendero).
 */
public final class BarrenderoSubCommand implements NdhSubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final BarrenderoModule module;

    public BarrenderoSubCommand(BarrenderoModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "barrendero";
    }

    @Override
    public List<String> getAliases() {
        return List.of("clean", "clearlag", "limpieza");
    }

    @Override
    public String getDescription() {
        return "Limpieza automática o manual de items en el suelo.";
    }

    @Override
    public String getUsage() {
        return "/ndh barrendero <limpiar|activar|desactivar|estado|recargar>";
    }

    @Override
    public @Nullable String getPermission() {
        return "barrendero.reload";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("estado") || args[0].equalsIgnoreCase("status")) {
            boolean active = module.isEnabled();
            BarrenderoConfig cfg = module.getConfig();
            int interval = cfg != null ? cfg.getIntervalSeconds() : 60;
            int age = cfg != null ? cfg.getAgeSeconds() : 15;
            sender.sendMessage(MINI.deserialize("<yellow>[Barrendero] Estado: "
                    + (active ? "<green>ACTIVO</green>" : "<red>DESACTIVADO</red>")
                    + " <gray>(Intervalo: " + interval + "s, Edad mín: " + age + "s)</gray>"));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("recargar") || sub.equals("reload")) {
            module.getPlugin().reloadConfig();
            module.onReload();
            sender.sendMessage(MINI.deserialize("<green>[Barrendero] Configuración recargada correctamente.</green>"));
            return;
        }

        if (sub.equals("limpiar") || sub.equals("now")) {
            CleanupManager cm = module.getCleanupManager();
            if (cm == null) {
                sender.sendMessage(
                        MINI.deserialize("<red>[Barrendero] El gestor de limpieza no está disponible.</red>"));
                return;
            }
            int removed = cm.runCleanup(false);
            sender.sendMessage(MINI.deserialize("<green>[Barrendero] Limpieza manual ejecutada: <yellow>" + removed
                    + "</yellow> items eliminados.</green>"));
            return;
        }

        if (sub.equals("activar") || sub.equals("on") || sub.equals("desactivar") || sub.equals("off")) {
            boolean enable = sub.equals("activar") || sub.equals("on");
            module.getPlugin().getConfig().set("barrendero.enabled", enable);
            module.getPlugin().saveConfig();
            module.onReload();
            sender.sendMessage(MINI.deserialize("<yellow>[Barrendero] Limpieza automática "
                    + (enable ? "<green>activada</green>." : "<red>desactivada</red>.")));
            return;
        }

        sender.sendMessage(
                MINI.deserialize("<yellow>Uso: /ndh barrendero <limpiar|activar|desactivar|estado|recargar></yellow>"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias,
            String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("limpiar", "activar", "desactivar", "estado", "recargar").stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
