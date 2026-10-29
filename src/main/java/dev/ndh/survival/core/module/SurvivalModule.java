package dev.ndh.survival.core.module;

/**
 * Contrato base para cualquier módulo de funcionalidad dentro de ndhSurvival.
 * Para añadir una nueva función en el futuro:
 * 1. Implementa {@link SurvivalModule}.
 * 2. Registra sus subcomandos de /ndh en {@link #onEnable()} mediante el CommandRegistry.
 * 3. Registra la instancia en {@code NdhSurvivalPlugin#registerModules()}.
 */
public interface SurvivalModule {

    /**
     * Identificador único del módulo (por ejemplo: "creeperask", "invsee", "jointg", "barrendero", "sharestats").
     */
    String getId();

    /**
     * Nombre legible del módulo para mostrar en listados y estados.
     */
    String getDisplayName();

    /**
     * Descripción breve de lo que hace este módulo.
     */
    String getDescription();

    /**
     * Se ejecuta al habilitar el plugin.
     */
    void onEnable();

    /**
     * Se ejecuta al deshabilitar el plugin.
     */
    void onDisable();

    /**
     * Se ejecuta cuando se recarga la configuración global (/ndh reload) o del módulo.
     */
    default void onReload() {
    }

    /**
     * Indica si el módulo se encuentra actualmente activo/operativo.
     */
    default boolean isEnabled() {
        return true;
    }
}
