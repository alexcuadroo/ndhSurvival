package dev.ndh.survival.modules.invsee.command;

import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.invsee.InvSeeModule;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Subcomando para inspeccionar y editar el inventario de un jugador (/ndh inventario).
 */
public final class InventarioSubCommand implements NdhSubCommand {

    private final InvSeeModule module;

    public InventarioSubCommand(InvSeeModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "inventario";
    }

    @Override
    public List<String> getAliases() {
        return List.of("invsee", "inv", "openinv");
    }

    @Override
    public String getDescription() {
        return "Ver y editar el inventario, armadura y offhand de un jugador (online/offline).";
    }

    @Override
    public String getUsage() {
        return "/ndh inventario <jugador|uuid>";
    }

    @Override
    public @Nullable String getPermission() {
        return "invsee.use";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        module.executeInvSee(sender, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return module.suggestPlayers(args);
    }
}
