package dev.ndh.survival.modules.creeperask.command;

import dev.ndh.survival.core.command.NdhSubCommand;
import dev.ndh.survival.modules.creeperask.CreeperAskModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Subcomando para administrar la funcionalidad de confirmación de creepers (/ndh creeper).
 */
public final class CreeperSubCommand implements NdhSubCommand {

    private final CreeperAskModule module;

    public CreeperSubCommand(CreeperAskModule module) {
        this.module = module;
    }

    @Override
    public String getName() {
        return "creeper";
    }

    @Override
    public List<String> getAliases() {
        return List.of("creeperask");
    }

    @Override
    public String getDescription() {
        return "Activa, desactiva o consulta el estado de confirmación de creepers.";
    }

    @Override
    public String getUsage() {
        return "/ndh creeper <activar|desactivar|estado>";
    }

    @Override
    public @Nullable String getPermission() {
        return "creeperask.admin";
    }

    @Override
    public void execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("estado") || args[0].equalsIgnoreCase("status")) {
            boolean active = module.isEnabled();
            sender.sendMessage(Component.text("CreeperAsk está ", NamedTextColor.YELLOW)
                    .append(Component.text(active ? "activado" : "desactivado",
                            active ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.text(".", NamedTextColor.YELLOW)));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("activar") || sub.equals("on") || sub.equals("desactivar") || sub.equals("off")) {
            boolean enable = sub.equals("activar") || sub.equals("on");
            module.setGloballyEnabled(enable);
            sender.sendMessage(Component.text("CreeperAsk " + (enable ? "activado." : "desactivado."),
                    enable ? NamedTextColor.GREEN : NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("Uso: /ndh creeper <activar|desactivar|estado>", NamedTextColor.YELLOW));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("activar", "desactivar", "estado").stream()
                    .filter(opt -> opt.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }
}
