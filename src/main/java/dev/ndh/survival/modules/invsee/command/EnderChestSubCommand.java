package dev.ndh.survival.modules.invsee.command;

import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.invsee.InvSeeModule;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Subcomando para inspeccionar y editar el Ender Chest de un jugador (/ndh enderchest).
 */
public final class EnderChestSubCommand implements NdhSubCommand {

    private final InvSeeModule module;

    public EnderChestSubCommand(InvSeeModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "enderchest";
    }

    @Override
    public List<String> getAliases() {
        return List.of("ender", "ecsee", "openender", "baul");
    }

    @Override
    public String getDescription() {
        return "Ver y editar el Ender Chest de un jugador (online/offline).";
    }

    @Override
    public String getUsage() {
        return "/ndh enderchest <jugador|uuid>";
    }

    @Override
    public @Nullable String getPermission() {
        return "invsee.enderchest";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        module.executeEnderSee(sender, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return module.suggestPlayers(args);
    }
}
