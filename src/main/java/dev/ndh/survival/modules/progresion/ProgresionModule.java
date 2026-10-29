package dev.ndh.survival.modules.progresion;

import dev.ndh.survival.NdhSurvivalPlugin;
import dev.ndh.survival.core.module.SurvivalModule;
import dev.ndh.survival.modules.progresion.command.ProgresionSubCommand;
import dev.ndh.survival.modules.progresion.listener.ProgresionListener;
import dev.ndh.survival.modules.progresion.service.ProgressionRules;
import dev.ndh.survival.modules.progresion.service.ProgressionService;
import java.io.IOException;
import org.jspecify.annotations.Nullable;

/** Registra y administra el ciclo de vida de la dificultad progresiva. */
public final class ProgresionModule implements SurvivalModule {
    private final NdhSurvivalPlugin plugin;
    private @Nullable ProgressionService service;

    /** Crea el módulo asociado al plugin. */
    public ProgresionModule(NdhSurvivalPlugin plugin) { this.plugin = plugin; }
    /** Devuelve el servicio, ausente si el módulo está desactivado o bloqueado. */
    public @Nullable ProgressionService service() { return service; }
    /** {@inheritDoc} */
    @Override public String getId() { return "progresion"; }
    /** {@inheritDoc} */
    @Override public String getDisplayName() { return "Dificultad progresiva"; }
    /** {@inheritDoc} */
    @Override public String getDescription() { return "Temporada acumulativa de treinta días de servidor encendido."; }
    /** {@inheritDoc} */
    @Override public void onEnable() {
        plugin.getCommandRouter().registerSubCommand(new ProgresionSubCommand(this, plugin));
        plugin.getServer().getPluginManager().registerEvents(new ProgresionListener(this, plugin), plugin);
        onReload();
    }
    /** Valida antes de publicar; conserva el servicio anterior si la configuración es inválida. */
    @Override public void onReload() {
        try {
            if (!plugin.getConfig().isBoolean("progresion.enabled")) {
                throw new IllegalArgumentException("progresion.enabled debe ser booleano");
            }
            var section = plugin.getConfig().getConfigurationSection("progresion.dias");
            if (section == null) throw new IllegalArgumentException("Falta progresion.dias");
            ProgressionRules rules = ProgressionRules.load(section);
            if (!plugin.getConfig().getBoolean("progresion.enabled")) { onDisable(); return; }
            ProgressionService current = service;
            if (current != null && current.operational()) { current.rules(rules); return; }
            ProgressionService replacement = new ProgressionService(plugin, rules);
            if (current != null) current.stop();
            service = replacement;
            replacement.start();
        } catch (IllegalArgumentException | IOException exception) {
            plugin.getLogger().severe("No se pudo cargar progresión: " + exception.getMessage());
            throw new IllegalStateException("No se pudo cargar progresión; se conserva la configuración anterior", exception);
        }
    }
    /** {@inheritDoc} */
    @Override public void onDisable() {
        ProgressionService current = service;
        if (current != null) current.stop();
        service = null;
    }
    /** {@inheritDoc} */
    @Override public boolean isEnabled() {
        ProgressionService current = service;
        return current != null && current.operational();
    }
}
