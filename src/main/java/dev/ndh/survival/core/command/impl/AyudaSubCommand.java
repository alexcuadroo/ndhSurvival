package dev.ndh.survival.core.command.impl;

import dev.ndh.survival.core.command.NdhCommandRouter;
import dev.ndh.survival.core.command.NdhSubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Subcomando para mostrar la ayuda general de ndhSurvival.
 */
public final class AyudaSubCommand implements NdhSubCommand {

    private final NdhCommandRouter router;

    public AyudaSubCommand(NdhCommandRouter router) {
        this.router = router;
    }

    @Override
    public String getName() {
        return "ayuda";
    }

    @Override
    public List<String> getAliases() {
        return List.of("help", "?");
    }

    @Override
    public String getDescription() {
        return "Muestra la lista de subcomandos disponibles en /ndh.";
    }

    @Override
    public String getUsage() {
        return "/ndh ayuda";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        router.sendHelp(sender);
    }
}
