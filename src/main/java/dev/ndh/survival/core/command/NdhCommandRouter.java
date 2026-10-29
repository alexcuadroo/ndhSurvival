package dev.ndh.survival.core.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jspecify.annotations.Nullable;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.command.impl.AyudaSubCommand;
import dev.ndh.survival.core.command.impl.EstadoSubCommand;
import dev.ndh.survival.core.command.impl.RecargarSubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Enrutador y autocompletador principal para {@code /ndh}.
 * Garantiza un autocompletado limpio con una única palabra canónica en español
 * por comando,
 * manteniendo compatibilidad de alias para la ejecución directa.
 */
public final class NdhCommandRouter implements CommandExecutor, TabCompleter {

    private final NdhSurvivalPlugin plugin;
    // Subcomandos canónicos en orden de registro
    private final Map<String, NdhSubCommand> primarySubCommands = new LinkedHashMap<>();
    // Mapeo exhaustivo de todos los nombres (canónico + alias) hacia el subcomando
    // para ejecución
    private final Map<String, NdhSubCommand> allTriggers = new LinkedHashMap<>();

    public NdhCommandRouter(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
        registerCoreSubCommands();
    }

    public void registerSubCommand(NdhSubCommand subCommand) {
        String primary = subCommand.getName().toLowerCase(Locale.ROOT);
        primarySubCommands.put(primary, subCommand);
        allTriggers.put(primary, subCommand);

        for (String alias : subCommand.getAliases()) {
            allTriggers.put(alias.toLowerCase(Locale.ROOT), subCommand);
        }
    }

    public Collection<NdhSubCommand> getPrimarySubCommands() {
        return Collections.unmodifiableCollection(primarySubCommands.values());
    }

    public Optional<NdhSubCommand> getSubCommand(@Nullable String nameOrAlias) {
        if (nameOrAlias == null)
            return Optional.empty();
        return Optional.ofNullable(allTriggers.get(nameOrAlias.toLowerCase(Locale.ROOT)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label,
            String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subLabel = args[0];
        NdhSubCommand subCommand = allTriggers.get(subLabel.toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            sender.sendMessage(Component.text("Subcomando desconocido: ", NamedTextColor.RED)
                    .append(Component.text(subLabel, NamedTextColor.YELLOW))
                    .append(Component.text(". Usa ", NamedTextColor.RED))
                    .append(Component.text("/ndh ayuda", NamedTextColor.GOLD))
                    .append(Component.text(" para ver las opciones disponibles.", NamedTextColor.RED)));
            return true;
        }

        if (!subCommand.hasPermission(sender)) {
            sender.sendMessage(Component.text("No tienes permiso para ejecutar este comando.", NamedTextColor.RED));
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subLabel, subArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command,
            String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();

            // Autocompletado limpio: ÚNICAMENTE los nombres canónicos en español, sin alias
            // redundantes
            for (NdhSubCommand sub : primarySubCommands.values()) {
                if (sub.hasPermission(sender) && sub.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    matches.add(sub.getName().toLowerCase(Locale.ROOT));
                }
            }
            matches.sort(String.CASE_INSENSITIVE_ORDER);
            return matches;
        }

        if (args.length >= 2) {
            NdhSubCommand subCommand = allTriggers.get(args[0].toLowerCase(Locale.ROOT));
            if (subCommand == null || !subCommand.hasPermission(sender)) {
                return List.of();
            }
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.tabComplete(sender, args[0], subArgs);
        }

        return List.of();
    }

    private void registerCoreSubCommands() {
        registerSubCommand(new AyudaSubCommand(this));
        registerSubCommand(new EstadoSubCommand(plugin));
        registerSubCommand(new RecargarSubCommand(plugin));
        registerSubCommand(new dev.ndh.survival.core.command.impl.ModulosSubCommand(plugin));
    }

    public void sendHelp(CommandSender sender) {
        sender.sendMessage(
                Component.text("=== Comandos de ndhSurvival (/ndh) ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        List<NdhSubCommand> allowed = primarySubCommands.values().stream()
                .filter(sub -> sub.hasPermission(sender))
                .sorted(Comparator.comparing(NdhSubCommand::getName))
                .toList();

        for (NdhSubCommand sub : allowed) {
            sender.sendMessage(Component.text("• ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(sub.getUsage(), NamedTextColor.YELLOW))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(sub.getDescription(), NamedTextColor.GRAY)));
        }
    }
}
