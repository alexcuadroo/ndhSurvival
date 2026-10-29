package dev.ndh.survival.modules.rtp.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.rtp.RtpConfig;
import dev.ndh.survival.modules.rtp.RtpModule;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Subcomando administrativo para el sistema de teletransporte aleatorio (/ndh
 * rtp).
 */
public final class RtpAdminSubCommand implements NdhSubCommand {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final RtpModule module;

    public RtpAdminSubCommand(RtpModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "rtp";
    }

    @Override
    public List<String> getAliases() {
        return List.of("randomtp", "azar");
    }

    @Override
    public String getDescription() {
        return "Administra el sistema de teletransporte aleatorio (RTP).";
    }

    @Override
    public String getUsage() {
        return "/ndh rtp <activar|desactivar|estado|recargar|tp|limpiarcooldown>";
    }

    @Override
    public @Nullable String getPermission() {
        return "rtp.admin";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        RtpConfig config = module.getConfig();

        if (args.length == 0 || args[0].equalsIgnoreCase("estado") || args[0].equalsIgnoreCase("status")) {
            boolean active = module.isEnabled();
            sender.sendMessage(MINI.deserialize(
                    "<gold><bold>[RTP Admin]</bold></gold> Estado: "
                            + (active ? "<green>ACTIVO</green>" : "<red>DESACTIVADO</red>")
                            + " <gray>| Rango: <yellow>" + (config != null ? config.getMinRadius() : 0) + " - "
                            + (config != null ? config.getMaxRadius() : 0) + " bloques</yellow>"
                            + " | Cooldown: <yellow>" + (config != null ? config.getCooldownSeconds() : 0)
                            + "s</yellow>"
                            + " | Warmup: <yellow>" + (config != null ? config.getWarmupSeconds() : 0) + "s</yellow>"
                            + " | Mundos: <yellow>" + (config == null || config.getAllowedWorlds().isEmpty() ? "Todos"
                                    : String.join(", ", config.getAllowedWorlds()))
                            + "</yellow></gray>"));
            return;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "activar", "on", "desactivar", "off" -> {
                boolean enable = action.equals("activar") || action.equals("on");
                module.getPlugin().getConfig().set("rtp.enabled", enable);
                module.getPlugin().saveConfig();
                module.onReload();
                sender.sendMessage(MINI.deserialize("<gold>[RTP]</gold> Módulo Random Teleport "
                        + (enable ? "<green>activado</green>." : "<red>desactivado</red>.")));
            }
            case "recargar", "reload" -> {
                module.getPlugin().reloadConfig();
                module.onReload();
                sender.sendMessage(MINI.deserialize("<green>[RTP] Configuración recargada correctamente.</green>"));
            }
            case "tp", "force" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        sender.sendMessage(
                                MINI.deserialize("<red>[RTP] El jugador '" + args[1] + "' no está conectado.</red>"));
                        return;
                    }
                } else if (sender instanceof Player playerSender) {
                    target = playerSender;
                } else {
                    sender.sendMessage(MINI.deserialize("<yellow>Uso desde consola: /ndh rtp tp <jugador></yellow>"));
                    return;
                }

                sender.sendMessage(MINI.deserialize("<green>[RTP] Teletransportando aleatoriamente a <yellow>"
                        + target.getName() + "</yellow> (sin esperas)...</green>"));
                module.startPlayerRtp(target, true);
            }
            case "limpiarcooldown", "clearcooldown", "resetcooldown" -> {
                if (args.length < 2) {
                    sender.sendMessage(
                            MINI.deserialize("<yellow>Uso: /ndh rtp limpiarcooldown <jugador|todos></yellow>"));
                    return;
                }
                if (args[1].equalsIgnoreCase("todos") || args[1].equalsIgnoreCase("all")) {
                    module.clearAllCooldowns();
                    sender.sendMessage(MINI.deserialize(
                            "<green>[RTP] Se han reiniciado los cooldowns de todos los jugadores.</green>"));
                    return;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(
                            MINI.deserialize("<red>[RTP] El jugador '" + args[1] + "' no está conectado.</red>"));
                    return;
                }
                module.clearCooldown(target.getUniqueId());
                sender.sendMessage(MINI.deserialize(
                        "<green>[RTP] Cooldown reiniciado para <yellow>" + target.getName() + "</yellow>.</green>"));
            }
            default -> sender.sendMessage(MINI.deserialize("<yellow>Uso: " + getUsage() + "</yellow>"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias,
            String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("activar", "desactivar", "estado", "recargar", "tp", "limpiarcooldown").stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            if (sub.equals("tp") || sub.equals("force")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .toList();
            }
            if (sub.equals("limpiarcooldown") || sub.equals("clearcooldown") || sub.equals("resetcooldown")) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("todos");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    suggestions.add(p.getName());
                }
                return suggestions.stream()
                        .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                        .toList();
            }
        }
        return List.of();
    }
}
