package dev.ndh.survival.modules.progresion.command;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.progresion.ProgresionModule;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Consultas públicas y operaciones administrativas de la temporada. */
public final class ProgresionSubCommand implements NdhSubCommand {
    private final ProgresionModule module;
    private final NdhSurvivalPlugin plugin;
    private static final List<String> PUBLIC = List.of("estado", "calendario");
    private static final List<String> ADMIN = List.of("estado", "calendario", "iniciar", "pausar", "continuar",
            "dia", "reiniciar", "recargar");

    /** Crea el comando para el módulo indicado. */
    public ProgresionSubCommand(ProgresionModule module, NdhSurvivalPlugin plugin) {
        this.module = module;
        this.plugin = plugin;
    }
    /** {@inheritDoc} */
    @Override public String getName() { return "progresion"; }
    /** {@inheritDoc} */
    @Override public String getDescription() { return "Consulta o administra la dificultad progresiva."; }
    /** {@inheritDoc} */
    @Override public String getUsage() { return "/ndh progresion <estado|calendario [dia]|iniciar|pausar|continuar|dia <1-30>|reiniciar confirmar|recargar>"; }
    /** {@inheritDoc} */
    @Override public String getPermission() { return "ndh.progresion.ver"; }
    /** Permite administrar aunque se haya revocado el permiso público. */
    @Override public boolean hasPermission(CommandSender sender) { return NdhSubCommand.super.hasPermission(sender) || admin(sender); }

    private boolean admin(CommandSender sender) { return sender.hasPermission("ndh.progresion.admin") || sender.hasPermission("ndh.admin"); }
    private void tell(CommandSender sender, String text) { sender.sendMessage(Component.text("[Progresión] " + text, NamedTextColor.GOLD)); }

    /** {@inheritDoc} */
    @Override public void execute(CommandSender sender, String label, String[] args) {
        String action = args.length == 0 ? "estado" : args[0].toLowerCase(Locale.ROOT);
        if (!PUBLIC.contains(action) && !admin(sender)) { tell(sender, "No tienes permiso administrativo."); return; }
        try {
            if (action.equals("recargar") && args.length == 1) {
                plugin.reloadConfig();
                module.onReload();
                tell(sender, "Configuración recargada.");
                return;
            }
            var service = module.service();
            if (service == null || !service.operational()) { tell(sender, "Módulo desactivado o bloqueado; consulta la consola."); return; }
            var clock = service.clock();
            if (action.equals("estado") && args.length <= 1) {
                if (!clock.started()) { tell(sender, "Temporada sin iniciar."); return; }
                long seconds = (clock.remainingMillis() + 999) / 1000;
                tell(sender, "Día " + clock.day() + (clock.paused() ? " (pausada)" : " (en curso)")
                        + (clock.day() == 30 ? " — dificultad máxima." : String.format(Locale.ROOT,
                        " — próximo día en %02d:%02d:%02d de servidor encendido.", seconds / 3600, seconds / 60 % 60, seconds % 60)));
                return;
            }
            if (action.equals("calendario") && args.length <= 2) {
                if (args.length == 2) {
                    int day = parseDay(args[1]);
                    tell(sender, "Día " + day + ": " + service.rules().changes(day));
                    tell(sender, "Acumulado: " + service.rules().accumulated(day));
                } else {
                    for (int day = 1; day <= 30; day++) tell(sender, "Día " + day + ": " + service.rules().changes(day));
                }
                return;
            }
            boolean valid = switch (action) {
                case "iniciar", "pausar", "continuar" -> args.length == 1;
                case "dia" -> args.length == 2;
                case "reiniciar" -> args.length == 2 && args[1].equalsIgnoreCase("confirmar");
                default -> false;
            };
            if (!valid) { tell(sender, "Uso: " + getUsage()); return; }
            if (action.equals("iniciar") && clock.started()) { tell(sender, "Ya existe una temporada."); return; }
            if (!action.equals("iniciar") && !action.equals("reiniciar") && !clock.started()) {
                tell(sender, "Primero inicia una temporada."); return;
            }
            service.change(action, action.equals("dia") ? parseDay(args[1]) : 1);
            tell(sender, service.operational() ? "Operación aplicada. Día " + clock.day() + "." : "Error al guardar; progresión detenida. Consulta la consola.");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            tell(sender, "No se aplicó la operación: " + exception.getMessage());
        }
    }

    private int parseDay(String value) {
        int day = Integer.parseInt(value);
        if (day < 1 || day > 30) throw new IllegalArgumentException("El día debe estar entre 1 y 30");
        return day;
    }

    /** {@inheritDoc} */
    @Override public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> options = List.of();
        if (args.length == 1) options = admin(sender) ? ADMIN : PUBLIC;
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("calendario") || admin(sender) && args[0].equalsIgnoreCase("dia")) {
                options = IntStream.rangeClosed(1, 30).mapToObj(Integer::toString).toList();
            } else if (admin(sender) && args[0].equalsIgnoreCase("reiniciar")) options = List.of("confirmar");
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
