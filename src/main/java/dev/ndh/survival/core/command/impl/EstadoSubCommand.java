package dev.ndh.survival.core.command.impl;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.core.module.SurvivalModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Subcomando para consultar el estado de activación de cada módulo.
 */
public final class EstadoSubCommand implements NdhSubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final NdhSurvivalPlugin plugin;

    public EstadoSubCommand(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "estado";
    }

    @Override
    public List<String> getAliases() {
        return List.of("status", "info");
    }

    @Override
    public String getDescription() {
        return "Muestra el estado actual de todos los módulos survival.";
    }

    @Override
    public String getUsage() {
        return "/ndh estado";
    }

    @Override
    public @Nullable String getPermission() {
        return "ndh.admin";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        sender.sendMessage(MINI.deserialize("<gold><bold>=== Estado de Módulos ndhSurvival ===</bold></gold>"));
        for (SurvivalModule module : plugin.getModuleManager().getModules()) {
            boolean active = module.isEnabled();
            String stateTag = active ? "<green>ACTIVO</green>" : "<red>INACTIVO</red>";
            sender.sendMessage(MINI.deserialize(
                    "<dark_gray>▪</dark_gray> <yellow>" + module.getDisplayName() + "</yellow> <gray>(" + module.getId() + ")</gray>: "
                            + stateTag + " <dark_gray>-</dark_gray> <gray>" + module.getDescription() + "</gray>"
            ));
        }
    }
}
