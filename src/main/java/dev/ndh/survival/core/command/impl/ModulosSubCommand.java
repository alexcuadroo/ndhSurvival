package dev.ndh.survival.core.command.impl;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.command.NdhSubCommand;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

/** Permite administrar todos los módulos en caliente con nombres en español. */
public final class ModulosSubCommand implements NdhSubCommand {
    private static final Map<String, String> MODULES = Map.of(
            "creeper", "creeperask", "inventarios", "invsee", "telegram", "jointg", "saludo", "saludo",
            "barrendero", "barrendero", "estadisticas", "sharestats", "rtp", "rtp", "progresion", "progresion");
    private final NdhSurvivalPlugin plugin;

    /**
     * Crea el control administrativo.
     * @param plugin plugin propietario de los módulos
     */
    public ModulosSubCommand(NdhSurvivalPlugin plugin) { this.plugin = plugin; }
    /** {@inheritDoc} */
    @Override public String getName() { return "modulos"; }
    /** {@inheritDoc} */
    @Override public String getDescription() { return "Activa o desactiva módulos en caliente y guarda el cambio."; }
    /** {@inheritDoc} */
    @Override public String getUsage() { return "/ndh modulos [<modulo> <activar|desactivar|estado>]"; }
    /** {@inheritDoc} */
    @Override public String getPermission() { return "ndh.admin"; }

    /** {@inheritDoc} */
    @Override public void execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender)) {
            tell(sender, "No tienes permiso administrativo.");
            return;
        }
        if (args.length == 0) {
            MODULES.keySet().stream().sorted().forEach(name -> showState(sender, name));
            return;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        String id = MODULES.get(name);
        if (id == null || args.length != 2) { tell(sender, "Uso: " + getUsage()); return; }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("estado")) { showState(sender, name); return; }
        if (!action.equals("activar") && !action.equals("desactivar")) {
            tell(sender, "Uso: " + getUsage()); return;
        }
        try {
            plugin.getModuleManager().setEnabled(id, action.equals("activar"));
            showState(sender, name);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            tell(sender, exception.getMessage());
        }
    }

    private void showState(CommandSender sender, String name) {
        plugin.getModuleManager().getModule(MODULES.get(name)).ifPresent(module ->
                tell(sender, name + ": " + (module.isEnabled() ? "activo" : "inactivo")));
    }

    private void tell(CommandSender sender, String message) {
        sender.sendMessage(Component.text("[Módulos] " + message, NamedTextColor.GOLD));
    }

    /** {@inheritDoc} */
    @Override public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!hasPermission(sender)) return List.of();
        List<String> options = args.length == 1 ? MODULES.keySet().stream().sorted().toList()
                : args.length == 2 && MODULES.containsKey(args[0].toLowerCase(Locale.ROOT))
                ? List.of("activar", "desactivar", "estado") : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
