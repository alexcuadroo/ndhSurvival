package dev.ndh.survival.core.command.impl;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.core.module.SurvivalModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Subcomando para recargar la configuración global o la de un módulo específico.
 */
public final class RecargarSubCommand implements NdhSubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final NdhSurvivalPlugin plugin;

    public RecargarSubCommand(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "recargar";
    }

    @Override
    public List<String> getAliases() {
        return List.of("reload");
    }

    @Override
    public String getDescription() {
        return "Recarga la configuración global o de un módulo específico.";
    }

    @Override
    public String getUsage() {
        return "/ndh recargar [todos|<módulo>]";
    }

    @Override
    public @Nullable String getPermission() {
        return "ndh.admin";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("todos") || args[0].equalsIgnoreCase("all")) {
            plugin.getModuleManager().reloadAll();
            sender.sendMessage(MINI.deserialize("<green>[ndhSurvival] Configuración y todos los módulos fueron recargados correctamente.</green>"));
            return;
        }

        String targetModule = args[0].toLowerCase(Locale.ROOT);
        Optional<SurvivalModule> moduleOpt = plugin.getModuleManager().getModule(targetModule);
        if (moduleOpt.isEmpty()) {
            sender.sendMessage(MINI.deserialize("<red>No existe ningún módulo llamado '<yellow>" + args[0] + "</yellow>'.</red>"));
            return;
        }

        plugin.reloadConfig();
        SurvivalModule module = moduleOpt.get();
        module.onReload();
        sender.sendMessage(MINI.deserialize("<green>[ndhSurvival] Módulo <yellow>" + module.getDisplayName() + "</yellow> recargado correctamente.</green>"));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            options.add("todos");
            for (SurvivalModule m : plugin.getModuleManager().getModules()) {
                options.add(m.getId());
            }
            return options.stream()
                    .filter(opt -> opt.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
