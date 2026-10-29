package dev.ndh.survival.core.command;

import org.bukkit.command.CommandSender;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Representa un subcomando dentro del comando unificado {@code /ndh <subcomando> [argumentos...]}.
 */
public interface NdhSubCommand {

    /**
     * Nombre principal del subcomando (ej. "creeperask", "openinv", "sharestats").
     */
    String getName();

    /**
     * Lista de alias alternativos para este subcomando (ej. "invsee", "inv" para "openinv").
     */
    default List<String> getAliases() {
        return List.of();
    }

    /**
     * Descripción breve para el menú de ayuda {@code /ndh help}.
     */
    String getDescription();

    /**
     * Sintaxis de uso (ej. "/ndh creeperask <on|off|status>").
     */
    String getUsage();

    /**
     * Permiso requerido para ejecutar y ver este subcomando en el autocompletado.
     * Devuelve {@code null} si cualquier jugador puede usarlo.
     */
    default @Nullable String getPermission() {
        return null;
    }

    /**
     * Comprueba si el {@link CommandSender} tiene permiso para ejecutar este subcomando.
     */
    default boolean hasPermission(CommandSender sender) {
        String perm = getPermission();
        return perm == null || perm.isBlank() || sender.hasPermission(perm) || sender.hasPermission("ndh.admin");
    }

    /**
     * Ejecuta la lógica del subcomando.
     *
     * @param sender Quien ejecuta el comando
     * @param label  El subcomando o alias utilizado
     * @param args   Argumentos posteriores al subcomando
     */
    void execute(CommandSender sender, String label, String[] args);

    /**
     * Devuelve las sugerencias de autocompletado para los argumentos de este subcomando.
     *
     * @param sender Quien solicita autocompletado
     * @param alias  El subcomando o alias utilizado
     * @param args   Argumentos actuales posteriores al subcomando
     */
    default List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return List.of();
    }
}
