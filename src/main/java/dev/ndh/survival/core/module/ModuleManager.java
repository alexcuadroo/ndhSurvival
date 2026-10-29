package dev.ndh.survival.core.module;

import dev.ndh.survival.NdhSurvivalPlugin;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Gestor central del ciclo de vida de todos los módulos de ndhSurvival.
 */
public final class ModuleManager {

    private final NdhSurvivalPlugin plugin;
    private final Map<String, SurvivalModule> modules = new LinkedHashMap<>();

    public ModuleManager(NdhSurvivalPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerModule(SurvivalModule module) {
        String key = module.getId().toLowerCase(Locale.ROOT);
        modules.put(key, module);
    }

    public void enableAll() {
        for (SurvivalModule module : modules.values()) {
            try {
                module.onEnable();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al habilitar el módulo " + module.getDisplayName(), e);
            }
        }
    }

    public void disableAll() {
        for (SurvivalModule module : modules.values()) {
            try {
                module.onDisable();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al deshabilitar el módulo " + module.getDisplayName(), e);
            }
        }
    }

    public void reloadAll() {
        plugin.reloadConfig();
        for (SurvivalModule module : modules.values()) {
            try {
                module.onReload();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error al recargar el módulo " + module.getDisplayName(), e);
            }
        }
    }

    public Optional<SurvivalModule> getModule(@Nullable String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(modules.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<SurvivalModule> getModules() {
        return Collections.unmodifiableCollection(modules.values());
    }

    /**
     * Aplica el interruptor de un módulo mediante su recarga en caliente.
     * @param id identificador registrado del módulo
     * @param enabled estado solicitado
     * @throws IllegalArgumentException si el módulo no existe
     * @throws IllegalStateException si no puede aplicar el cambio
     */
    public void setEnabled(String id, boolean enabled) {
        SurvivalModule module = getModule(id).orElseThrow(() -> new IllegalArgumentException("Módulo desconocido: " + id));
        String path = module.getId() + ".enabled";
        Object previous = plugin.getConfig().get(path);
        Object previousTelegram = plugin.getConfig().get("jointg.telegram-enabled");
        plugin.getConfig().set(path, enabled);
        if (module.getId().equals("jointg")) plugin.getConfig().set("jointg.telegram-enabled", enabled);
        try {
            module.onReload();
            if (module.isEnabled() != enabled) {
                throw new IllegalStateException("El módulo no alcanzó el estado solicitado; consulta la consola.");
            }
        } catch (RuntimeException exception) {
            plugin.getConfig().set(path, previous);
            if (module.getId().equals("jointg")) plugin.getConfig().set("jointg.telegram-enabled", previousTelegram);
            try {
                module.onReload();
            } catch (RuntimeException rollback) {
                exception.addSuppressed(rollback);
            }
            plugin.getLogger().log(Level.SEVERE, "No se pudo cambiar el estado de " + id, exception);
            throw new IllegalStateException("No se pudo cambiar " + id + ": " + exception.getMessage(), exception);
        }
        plugin.saveConfig();
    }
}
