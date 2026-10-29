package dev.ndh.survival.modules.saludo.command;

import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.saludo.SaludoModule;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Subcomando para administrar los títulos y mensajes de bienvenida (/ndh saludo).
 */
public final class SaludoSubCommand implements NdhSubCommand {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final SaludoModule module;

    /** @param module módulo de bienvenida */
    public SaludoSubCommand(SaludoModule module) {
        this.module = module;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "saludo";
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getAliases() {
        return List.of();
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return "Administra los títulos y mensajes de bienvenida.";
    }

    /** {@inheritDoc} */
    @Override
    public String getUsage() {
        return "/ndh saludo <activar|desactivar|estado|recargar>";
    }

    /** {@inheritDoc} */
    @Override
    public @Nullable String getPermission() {
        return "ndh.saludo.admin";
    }

    /** {@inheritDoc} */
    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("estado") || args[0].equalsIgnoreCase("status")) {
            boolean active = module.isEnabled();
            sender.sendMessage(MINI_MESSAGE.deserialize(
                    "<yellow>[Saludo] Bienvenida: "
                            + (active ? "<green>ACTIVA</green>" : "<red>DESACTIVADA</red>")));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("recargar") || sub.equals("reload")) {
            module.getPlugin().reloadConfig();
            module.onReload();
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>[Saludo] Configuración recargada correctamente.</green>"));
            return;
        }

        if (sub.equals("activar") || sub.equals("on") || sub.equals("desactivar") || sub.equals("off")) {
            boolean enabled = sub.equals("activar") || sub.equals("on");
            module.getPlugin().getConfig().set("saludo.enabled", enabled);
            module.getPlugin().saveConfig();
            module.onReload();
            sender.sendMessage(MINI_MESSAGE.deserialize(
                    "<yellow>[Saludo] Bienvenida "
                            + (module.isEnabled() ? "<green>activada</green>." : "<red>desactivada</red> (revisa la configuración).")));
            return;
        }

        sender.sendMessage(MINI_MESSAGE.deserialize("<yellow>Uso: /ndh saludo <activar|desactivar|estado|recargar></yellow>"));
    }

    /** {@inheritDoc} */
    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("activar", "desactivar", "estado", "recargar").stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
