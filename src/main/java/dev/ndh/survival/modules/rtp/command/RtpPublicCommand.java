package dev.ndh.survival.modules.rtp.command;

import dev.ndh.survival.modules.rtp.RtpModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Manejador del comando público {@code /rtp} para jugadores normales.
 */
public final class RtpPublicCommand implements CommandExecutor, TabCompleter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final RtpModule module;

    public RtpPublicCommand(RtpModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(MINI.deserialize("<red>Este comando solo puede ser usado por jugadores. Usa <yellow>/ndh rtp tp <jugador></yellow> desde consola.</red>"));
            return true;
        }

        if (!player.hasPermission("rtp.use") && !player.hasPermission("ndh.admin")) {
            player.sendMessage(MINI.deserialize("<red>No tienes permiso para usar /rtp.</red>"));
            return true;
        }

        module.startPlayerRtp(player, false);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
